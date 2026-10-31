/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.data.repo

import app.opentv.R
import app.opentv.core.AppText
import app.opentv.data.db.SourceDao
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.data.remote.StalkerApi
import app.opentv.data.remote.XtreamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SourceRepository(
    private val dao: SourceDao,
    private val api: XtreamApi,
    private val stalkerApi: StalkerApi,
) {
    fun observeAll(): Flow<List<Source>> = dao.observeAll()

    suspend fun enabled(): List<Source> = dao.enabled()

    suspend fun byId(id: Long): Source? = dao.byId(id)

    data class EnsureBuiltInResult(
        val source: Source,
        val created: Boolean,
    )

    /** Ensure the free IPTV-ORG Turkey playlist exists as OpenTV's built-in live source. */
    suspend fun ensureBuiltInTurkeySource(): EnsureBuiltInResult = withContext(Dispatchers.IO) {
        val url = normaliseUrl(BUILT_IN_TURKEY_URL, SourceKind.M3U)
        val existing = dao.byKindAndUrl(SourceKind.M3U, url)
        if (existing != null) {
            return@withContext EnsureBuiltInResult(existing, created = false)
        }

        val id = dao.insert(
            Source(
                name = BUILT_IN_TURKEY_NAME,
                kind = SourceKind.M3U,
                url = url,
            ),
        )
        val saved = dao.byId(id)
            ?: error("Built-in IPTV-ORG Turkey source was inserted but could not be read back")
        EnsureBuiltInResult(saved, created = true)
    }

    /** Ensure the hidden SporB supplemental source exists after the local feature is unlocked. */
    suspend fun ensureSporbSource(): EnsureBuiltInResult = withContext(Dispatchers.IO) {
        val url = normaliseUrl(SPORB_URL, SourceKind.M3U)
        val existing = dao.byKindAndUrl(SourceKind.M3U, url)
        if (existing != null) {
            val active = if (existing.enabled) existing else existing.copy(enabled = true).also { dao.update(it) }
            return@withContext EnsureBuiltInResult(active, created = false)
        }

        val id = dao.insert(
            Source(
                name = SPORB_NAME,
                kind = SourceKind.M3U,
                url = url,
            ),
        )
        val saved = dao.byId(id)
            ?: error("SporB source was inserted but could not be read back")
        EnsureBuiltInResult(saved, created = true)
    }

    /** Disabling preserves cached channels and favourites; public catalogue queries hide them. */
    suspend fun disableSporbSource() = withContext(Dispatchers.IO) {
        val url = normaliseUrl(SPORB_URL, SourceKind.M3U)
        val source = dao.byKindAndUrl(SourceKind.M3U, url) ?: return@withContext
        if (source.enabled) dao.update(source.copy(enabled = false))
    }

    suspend fun save(source: Source): Long = withContext(Dispatchers.IO) {
        val normalised = source.copy(url = normaliseUrl(source.url, source.kind))
        if (source.id == 0L) dao.insert(normalised)
        else {
            dao.update(normalised)
            source.id
        }
    }

    /** Checks the details work before the user is committed to them. */
    suspend fun test(source: Source): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (source.kind) {
                SourceKind.XTREAM -> {
                    val info = api.authenticate(
                        source.copy(url = normaliseUrl(source.url, source.kind)),
                    )
                    buildString {
                        append(AppText.get(R.string.msg_connected))
                        info.username?.let { append(AppText.get(R.string.msg_connected_as, it)) }
                        info.maxConnections?.let { append(AppText.get(R.string.msg_connections, it)) }
                        info.expiryMillis?.let {
                            append(AppText.get(R.string.msg_expires, java.text.DateFormat.getDateInstance().format(java.util.Date(it))))
                        }
                    }
                }
                SourceKind.M3U -> AppText.get(R.string.msg_playlist_valid)
                SourceKind.STALKER -> {
                    stalkerApi.handshakeTest(source.copy(url = normaliseUrl(source.url, source.kind)))
                    AppText.get(R.string.msg_portal_ok)
                }
            }
        }
    }

    companion object {
        const val BUILT_IN_TURKEY_NAME = "IPTV-ORG_Turkiye"
        const val BUILT_IN_TURKEY_URL = "https://iptv-org.github.io/iptv/countries/tr.m3u"

        const val SPORB_NAME = "SporB"
        const val SPORB_URL = "https://raw.githubusercontent.com/ugur2941/b-t-n-kanallar/main/sporb"

        // A short-lived source used during early Turkey-list testing. It is intentionally retired;
        // keep the exact URL only so existing tester installs can remove that one legacy row.
        const val LEGACY_FAMELACK_URL =
            "https://raw.githubusercontent.com/DEvmIb/famelack-channels-m3u/main/m3u/tr.m3u"

        /**
         * Users paste all sorts of things. Accept them all rather than making someone guess
         * the format: a trailing slash, a missing scheme, or a full `get.php` URL copied out
         * of a provider's welcome email.
         *
         * For an Xtream source we want the bare host, so API paths are stripped. For an M3U
         * source the URL *is* the playlist — stripping the path there would destroy it, which
         * is why [kind] is not optional.
         *
         * Pure and static so it can be tested without standing up the repository.
         */
        fun normaliseUrl(raw: String, kind: SourceKind): String {
            var url = raw.trim()
            if (url.isEmpty()) return url
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
                url = "http://$url"
            }
            if (kind == SourceKind.M3U) return url
            // A Stalker portal URL is the portal path itself (e.g. .../stalker_portal or .../c) —
            // stripping Xtream API paths would be wrong; just tidy the trailing slash.
            if (kind == SourceKind.STALKER) return url.trimEnd('/')

            url = url.substringBefore("/player_api.php")
                .substringBefore("/panel_api.php")
                .substringBefore("/get.php")
                .substringBefore("/xmltv.php")
            return url.trimEnd('/')
        }
    }
}
