/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.player

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import app.opentv.R
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns a [PlaybackException] into something a person can act on.
 *
 * "ERROR_CODE_IO_BAD_HTTP_STATUS" tells a user nothing. Worse, the habit of showing a raw
 * error code on screen is what led one player to *remove* its 403 display entirely as a
 * "fix" — hiding the symptom while the stream still failed. The right answer is to say what
 * probably went wrong and what to try.
 */
@OptIn(UnstableApi::class)
object PlaybackErrors {

    /** Set once by the app so messages come out in the user's language. */
    @Volatile var context: android.content.Context? = null

    private fun text(id: Int, vararg args: Any): String =
        context?.getString(id, *args) ?: "Playback failed."

    fun describe(error: PlaybackException): String {
        (error.cause as? HttpDataSource.InvalidResponseCodeException)?.let {
            return describeHttpStatus(it.responseCode)
        }
        if (error.cause is UnknownHostException) {
            return text(R.string.err_unreachable)
        }
        if (error.cause is SocketTimeoutException) {
            return text(R.string.err_timeout)
        }

        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                text(R.string.err_lost)

            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
                text(R.string.err_html)

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                text(R.string.err_malformed)

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                text(R.string.err_no_decoder)

            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ->
                text(R.string.err_unsupported)

            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                text(R.string.err_behind)

            else -> text(R.string.err_generic, error.errorCodeName)
        }
    }

    fun describeHttpStatus(code: Int): String = when (code) {
        401 -> text(R.string.err_401)
        403 -> text(R.string.err_403)
        404 -> text(R.string.err_404)
        405 -> text(R.string.err_405)
        429 -> text(R.string.err_429)
        451 -> text(R.string.err_451)
        in 500..599 -> text(R.string.err_5xx, code)
        else -> text(R.string.err_http, code)
    }
}
