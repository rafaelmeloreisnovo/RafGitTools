package com.rafgittools.kernel

import android.util.Log

object RafKernelBridge {
    private const val TAG = "RafKernel"
    private var loaded = false

    init {
        try {
            System.loadLibrary("raf_llama_kernel")
            loaded = true
            Log.i(TAG, "raf_llama_kernel loaded — embedded kernel mode")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "raf_llama_kernel not available — fallback to Termux server mode")
        }
    }

    fun isAvailable(): Boolean = loaded

    external fun nativeKernelOpen(modelPath: String, ctiPath: String): Int
    external fun nativeKernelChat(requestJson: String, responseCapacity: Int): String?
    external fun nativeKernelToolResult(toolResultJson: String, responseCapacity: Int): String?
    external fun nativeKernelClose()
}
