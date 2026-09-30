/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.content.Context

/**
 * Localised text for code that has no Context of its own — repositories, API clients, workers —
 * whose messages still end up on screen (sync status, connection errors). Set once from the
 * Application; falls back to the resource's English only if used before that (never in practice).
 */
object AppText {
    @Volatile private var context: Context? = null

    fun init(context: Context) { this.context = context.applicationContext }

    fun get(id: Int, vararg args: Any): String =
        context?.getString(id, *args) ?: "…"
}
