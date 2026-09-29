/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.data.repo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import app.opentv.data.db.DownloadDao
import app.opentv.data.model.Download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Films and episodes saved for offline viewing (a plane, a train, no signal).
 *
 * The transfer itself is Android's DownloadManager: it resumes after a dropped connection, keeps
 * going when the app is closed, and posts a system notification when it finishes. Files land in this
 * app's own external folder, so no storage permission is needed (and they go when the app is
 * uninstalled). The database row is just the catalogue the Downloads tab lists; live state comes
 * from DownloadManager each time it's asked.
 */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val http: okhttp3.OkHttpClient,
) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    enum class State { QUEUED, RUNNING, PAUSED, DONE, FAILED }

    /** A download plus what DownloadManager says about it right now. */
    data class Item(
        val download: Download,
        val state: State,
        val bytesSoFar: Long,
        val totalBytes: Long,
        /** Local file to play, once [state] is DONE. */
        val fileUri: String?,
        /** Why it failed or is paused (a DownloadManager ERROR_/PAUSED_ reason), 0 when n/a. */
        val reason: Int = 0,
    ) {
        val outOfSpace: Boolean get() = state == State.FAILED && reason == DownloadManager.ERROR_INSUFFICIENT_SPACE
        val progress: Float get() = if (totalBytes > 0) (bytesSoFar.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }

    /** Everything downloaded or downloading, refreshed every second while someone is watching. */
    fun observe(): Flow<List<Item>> =
        combine(dao.observeAll(), ticker()) { downloads, _ -> withStatus(downloads) }
            .flowOn(Dispatchers.IO)

    /** One entry's live state, or null when it isn't downloaded — drives the Download buttons. */
    fun observe(mediaKey: String): Flow<Item?> =
        combine(dao.observeAll(), ticker()) { downloads, _ ->
            downloads.firstOrNull { it.mediaKey == mediaKey }?.let { withStatus(listOf(it)).firstOrNull() }
        }.flowOn(Dispatchers.IO)

    sealed interface Start {
        data object Started : Start
        /** Won't fit: [neededBytes] is 0 when the provider didn't say how big it is. */
        data class NoSpace(val neededBytes: Long, val freeBytes: Long) : Start
    }

    /**
     * Starts a download — unless it can't fit. The provider is asked for the file size first:
     * 4K films run to 20 GB+, and starting one that can't finish just fills the phone and fails.
     */
    suspend fun enqueue(download: Download, userAgent: String): Start = withContext(Dispatchers.IO) {
        val free = freeBytes()
        val size = remoteSize(download.sourceUrl, userAgent)
        if (free < MIN_FREE_BYTES || (size > 0 && size + MARGIN_BYTES > free)) {
            return@withContext Start.NoSpace(size, free)
        }
        dao.byKey(download.mediaKey)?.let { remove(it) }

        val extension = download.sourceUrl.substringAfterLast('.', "mp4")
            .substringBefore('?').take(5).ifBlank { "mp4" }
        val fileName = "${download.mediaKey.replace(':', '_')}.$extension"
        val request = DownloadManager.Request(Uri.parse(download.sourceUrl))
            .addRequestHeader("User-Agent", userAgent)
            .setTitle(download.title)
            .setDescription(download.seriesTitle ?: "OpenTV")
            // Only the "done" notification. The system's live-progress one re-alerts on every update
            // on some phones (a chime every few seconds for the whole download); progress is shown
            // in the Downloads tab and on the Download button instead.
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
        val systemId = manager.enqueue(request)
        dao.insert(download.copy(systemId = systemId, createdMillis = System.currentTimeMillis()))
        Start.Started
    }

    /**
     * The file's size, or 0 when the server won't say. Many IPTV panels ignore HEAD, so this falls
     * back to asking for the first byte only and reading the total from Content-Range
     * ("bytes 0-0/20640236829") — no real download happens.
     */
    private fun remoteSize(url: String, userAgent: String): Long {
        val head = runCatching {
            val request = okhttp3.Request.Builder().url(url).head().header("User-Agent", userAgent).build()
            http.newCall(request).execute().use { r ->
                if (r.isSuccessful) r.header("Content-Length")?.toLongOrNull() ?: 0L else 0L
            }
        }.getOrDefault(0L)
        if (head > 0) return head
        return runCatching {
            val request = okhttp3.Request.Builder().url(url)
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-0")
                .build()
            http.newCall(request).execute().use { r ->
                r.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                    ?: r.header("Content-Length")?.toLongOrNull()?.takeIf { r.code == 200 }
                    ?: 0L
            }
        }.getOrDefault(0L)
    }

    /** The finished local file for [mediaKey], or null when it isn't fully downloaded. */
    suspend fun localFile(mediaKey: String): String? = withContext(Dispatchers.IO) {
        val download = dao.byKey(mediaKey) ?: return@withContext null
        withStatus(listOf(download)).firstOrNull()
            ?.takeIf { it.state == State.DONE }?.fileUri
    }

    /** Cancels the transfer if it's running, deletes the file, and forgets the entry. */
    suspend fun remove(download: Download) = withContext(Dispatchers.IO) {
        runCatching { manager.remove(download.systemId) }
        dao.delete(download.id)
    }

    /** Space left where downloads are saved. */
    fun freeBytes(): Long {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return 0L
        return runCatching { StatFs(dir.path).availableBytes }.getOrDefault(0L)
    }

    private fun withStatus(downloads: List<Download>): List<Item> {
        if (downloads.isEmpty()) return emptyList()
        val byId = HashMap<Long, Item>()
        val query = DownloadManager.Query().setFilterById(*downloads.map { it.systemId }.toLongArray())
        runCatching {
            manager.query(query)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                val statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val soFarCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val uriCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                val reasonCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val download = downloads.firstOrNull { it.systemId == id } ?: continue
                    val state = when (c.getInt(statusCol)) {
                        DownloadManager.STATUS_SUCCESSFUL -> State.DONE
                        DownloadManager.STATUS_FAILED -> State.FAILED
                        DownloadManager.STATUS_PAUSED -> State.PAUSED
                        DownloadManager.STATUS_RUNNING -> State.RUNNING
                        else -> State.QUEUED
                    }
                    byId[id] = Item(
                        download, state, c.getLong(soFarCol), c.getLong(totalCol), c.getString(uriCol),
                        reason = c.getInt(reasonCol),
                    )
                }
            }
        }
        // A row DownloadManager no longer knows (cleared from system settings) shows as failed, so
        // the user can see it and delete it rather than it silently vanishing.
        return downloads.map { byId[it.systemId] ?: Item(it, State.FAILED, 0, 0, null) }
    }

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1_000)
        }
    }

    private companion object {
        const val MIN_FREE_BYTES = 1_500L * 1024 * 1024
        /** Head-room left over after a download, so it doesn't fill the phone to the last byte. */
        const val MARGIN_BYTES = 500L * 1024 * 1024
    }
}
