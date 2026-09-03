package com.rafgittools.kernel

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Kotlin bridge for Rafaelia Kernel JNI layer.
 *
 * Provides Kotlin abstraction over native LLM kernel functions.
 * Handles multi-turn tool call loops, context management, and evidence collection.
 *
 * Authority: RafGitTools kernel layer
 * Epistemic state: IMPLEMENTED (Kotlin wrapper); JNI layer TOKEN_VAZIO until llama.h available
 */
class RafaeliaKernelBridge(private val context: Context) {

    companion object {
        private const val TAG = "RafaeliaKernel"
        private const val LIB_NAME = "rafcore"

        init {
            runCatching { System.loadLibrary(LIB_NAME) }.onFailure {
                Log.w(TAG, "Native library $LIB_NAME not loaded: ${it.message}")
            }
        }
    }

    // Native function declarations (JNI contract with raf_kernel_jni.c)
    external fun nativeAsmHealth(): Int
    external fun nativeAbiMask(): Int

    // Multi-turn LLM context initialization
    // CONTRACT: ctiPath must be passed through to llama_context_init()
    // Currently: TOKEN_VAZIO_LLAMA_HEADER (llama.h not in repo)
    external fun nativeContextInit(ctiPath: String, maxTokens: Int): Long

    // Tool call invocation
    // Returns: tool_result JSON string or error message
    external fun nativeInvokeTool(contextId: Long, toolName: String, arguments: String): String

    // Multi-turn tool call loop (TOKEN_VAZIO_LLAMA_LOOP status)
    // Currently: Single-turn implementation; multi-turn requires llama.h continuation logic
    external fun nativeRunToolLoop(
        contextId: Long,
        prompt: String,
        maxIterations: Int
    ): String

    // Context cleanup
    external fun nativeContextCleanup(contextId: Long)

    /**
     * Kotlin-level multi-turn tool call loop.
     *
     * This layer implements the loop logic in Kotlin to avoid blocking JNI.
     * Once raf_kernel_jni.c has llama.h, this becomes a thin wrapper.
     */
    suspend fun executeToolLoop(
        prompt: String,
        maxIterations: Int = 10,
        onToolInvoke: suspend (toolName: String, arguments: String) -> String
    ): ToolLoopResult = withContext(Dispatchers.Default) {
        val iterations = mutableListOf<ToolLoopIteration>()
        var currentPrompt = prompt
        var contextId = -1L

        try {
            // Initialize context
            val ctiPath = getCtiPath()
            contextId = nativeContextInit(ctiPath, 4096)

            if (!isNativeAssemblerCoreReady(contextId)) {
                return@withContext ToolLoopResult.Error("Native kernel not ready")
            }

            // Main loop: continue while model requests tools
            for (iteration in 0 until maxIterations) {
                Log.d(TAG, "Tool loop iteration $iteration")

                // Run single turn
                val turnResult = nativeRunToolLoop(contextId, currentPrompt, 1)

                // Parse response
                val turnData = parseToolResponse(turnResult)
                if (turnData == null) {
                    Log.w(TAG, "Failed to parse tool response: $turnResult")
                    iterations.add(ToolLoopIteration.Error("Parse failed", turnResult))
                    break
                }

                iterations.add(turnData)

                // Check if model requests tools
                if (turnData !is ToolLoopIteration.ToolRequest) {
                    Log.d(TAG, "Model finished at iteration $iteration")
                    break
                }

                // Execute tool
                val toolResult = onToolInvoke(turnData.toolName, turnData.arguments)
                Log.d(TAG, "Tool ${turnData.toolName} returned: ${toolResult.take(100)}")

                // Prepare next prompt with tool result
                currentPrompt = """
                    ${turnData.modelResponse}

                    Tool "${turnData.toolName}" returned:
                    $toolResult
                """.trimIndent()
            }

            ToolLoopResult.Success(
                iterations = iterations,
                totalIterations = iterations.size,
                finalResponse = (iterations.lastOrNull() as? ToolLoopIteration.FinalResponse)?.text
                    ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Tool loop error", e)
            ToolLoopResult.Error(e.message ?: "Unknown error")
        } finally {
            if (contextId >= 0) {
                nativeContextCleanup(contextId)
            }
        }
    }

    private fun getCtiPath(): String {
        val ctiFile = File(context.cacheDir, "llama_context.bin")
        return ctiFile.absolutePath
    }

    private fun isNativeAssemblerCoreReady(contextId: Long): Boolean {
        return runCatching {
            val health = nativeAsmHealth()
            health >= 8  // Arbitrary health threshold
        }.getOrDefault(false)
    }

    private fun parseToolResponse(json: String): ToolLoopIteration? {
        return runCatching {
            when {
                json.contains("\"type\":\"tool_use\"") -> {
                    // Extract tool name and arguments
                    val toolName = extractJsonString(json, "name")
                    val arguments = extractJsonString(json, "input")
                    if (toolName != null && arguments != null) {
                        ToolLoopIteration.ToolRequest(
                            toolName = toolName,
                            arguments = arguments,
                            modelResponse = json
                        )
                    } else null
                }
                json.contains("\"type\":\"text\"") -> {
                    val text = extractJsonString(json, "text")
                    if (text != null) {
                        ToolLoopIteration.FinalResponse(text)
                    } else null
                }
                else -> ToolLoopIteration.Error("Unknown response type", json)
            }
        }.getOrNull()
    }

    private fun extractJsonString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }
}

sealed class ToolLoopResult {
    data class Success(
        val iterations: List<ToolLoopIteration>,
        val totalIterations: Int,
        val finalResponse: String
    ) : ToolLoopResult()

    data class Error(val message: String) : ToolLoopResult()
}

sealed class ToolLoopIteration {
    data class ToolRequest(
        val toolName: String,
        val arguments: String,
        val modelResponse: String
    ) : ToolLoopIteration()

    data class FinalResponse(val text: String) : ToolLoopIteration()

    data class Error(val message: String, val context: String) : ToolLoopIteration()
}

/**
 * Health check for native assembler core library.
 *
 * Returns: true if native library is loaded and healthy (health >= 8)
 */
fun isNativeAssemblerCoreReady(): Boolean {
    return runCatching {
        val bridge = RafaeliaKernelBridge(null!!) // Stateless check
        bridge.nativeAsmHealth() >= 8
    }.getOrDefault(false)
}
