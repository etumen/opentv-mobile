/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.os.Process
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Where catalogue/guide syncs run: a small pool at Android's background thread priority.
 *
 * A sync on a big provider parses and normalises hundreds of thousands of rows. On the default
 * dispatchers that work competes with the UI at the same priority, and on a phone or TV box with
 * few cores the app felt frozen for the first seconds after launch. Background priority lets the
 * scheduler put taps and drawing first; the sync just takes a little longer when you're busy.
 */
object BackgroundWork {
    private val count = AtomicInteger()

    val dispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(2) { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "opentv-sync-${count.incrementAndGet()}").apply { isDaemon = true }
    }.asCoroutineDispatcher()
}
