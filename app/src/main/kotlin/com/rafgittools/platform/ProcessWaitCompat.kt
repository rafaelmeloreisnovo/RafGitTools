package com.rafgittools.platform

import android.os.Build
import java.util.concurrent.TimeUnit

/**
 * API 24-safe bounded process lifecycle adapter.
 *
 * On API 26+ preserve the platform timed wait and forceful termination.
 * On API 24-25 poll Process.exitValue and use best-effort destroy.
 * Never block indefinitely in a terminal timeout path.
 */
internal object ProcessWaitCompat {
    fun waitFor(process: Process, timeout: Long, unit: TimeUnit): Boolean {
        require(timeout >= 0L) { "timeout must be nonnegative" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return process.waitFor(timeout, unit)
        }

        val started = System.nanoTime()
        val timeoutNanos = unit.toNanos(timeout)
        while (true) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                // Still running; do not call Java 8 Process timed APIs on API 24.
            }
            if (System.nanoTime() - started >= timeoutNanos) return false
            Thread.sleep(25L)
        }
    }

    fun terminate(process: Process) {
        process.destroy()
        if (!waitFor(process, 500L, TimeUnit.MILLISECONDS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.destroyForcibly()
            } else {
                process.destroy()
            }
            // Best-effort bounded reaping. Never wait indefinitely.
            waitFor(process, 1L, TimeUnit.SECONDS)
        }
    }
}
