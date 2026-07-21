package com.rafgittools.kernel

import android.util.Log
import java.nio.ByteBuffer

/**
 * JNI bridge to the RAFAELIA engine (7D toroidal map, CRC32C, coherence/entropy EMA).
 *
 * Native peer: kernel/native/rafaelia_jni.c (library: rafaelia)
 *
 * Usage:
 *   val inBuf  = ByteBuffer.allocateDirect(65536)
 *   val outBuf = ByteBuffer.allocateDirect(65536)
 *   inBuf.put(data).flip()
 *   val written = RafaeliaCore.processNative(inBuf, inBuf.remaining(), outBuf)
 */
object RafaeliaCore {
    private const val TAG = "RafaeliaCore"
    private var loaded = false

    init {
        try {
            System.loadLibrary("rafaelia")
            loaded = true
            Log.i(TAG, "rafaelia native engine loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "rafaelia native library not available — engine disabled")
        }
    }

    fun isAvailable(): Boolean = loaded

    /**
     * Process [inLen] bytes from [input] DirectByteBuffer through the toroidal EMA engine.
     * Writes result (8 or 16 bytes: crc32c|phi|phase|step) into [output] DirectByteBuffer.
     * Returns bytes written, or -1/-2 on error.
     */
    external fun processNative(input: ByteBuffer, inLen: Int, output: ByteBuffer): Int

    /**
     * Advance the 7D toroidal state stored in [state] DirectByteBuffer by one [cycle] (0..41).
     * Returns phi Q16.16, or -1 on error.
     */
    external fun stepNative(state: ByteBuffer, cycle: Int): Int

    /**
     * Write a JSON hardware-profile string into [output] DirectByteBuffer (capacity [cap]).
     * Returns bytes written, or -1 on error.
     */
    external fun profileNative(output: ByteBuffer, cap: Int): Long

    /**
     * Returns bytes used in the internal 256 KB JNI arena (diagnostic only).
     */
    external fun arenaSizeNative(): Int

    /**
     * Compute CRC32C of [len] bytes in [buf] DirectByteBuffer. Returns the CRC as a signed int.
     */
    external fun crc32Native(buf: ByteBuffer, len: Int): Int
}
