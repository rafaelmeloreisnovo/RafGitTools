package com.rafgittools.kernel

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
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
            runCatching { System.loadLibrary(LIB_NAME) }.onFailure { error ->
                // Logging must not make the bridge uninitializable in host-side JVM tests.
                runCatching {
                    Log.w(TAG, "Native library $LIB_NAME not loaded: ${error.message}")
                }
            }
        }

        /**
         * Parse provider/tool-loop JSON independently from JNI and Android Context.
         * Whitespace and object-valued `input` are both valid JSON and must not alter semantics.
         */
        internal fun parseToolResponse(json: String): ToolLoopIteration? = runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            when (root.get("type")?.asString) {
                "tool_use" -> {
                    val toolName = root.get("name")?.takeUnless { it.isJsonNull }?.asString
                    val input = root.get("input")?.takeUnless { it.isJsonNull }
                    if (toolName.isNullOrBlank() || input == null) {
                        null
                    } else {
                        val arguments = if (input.isJsonPrimitive && input.asJsonPrimitive.isString) {
                            input.asString
                        } else {
                            input.toString()
                        }
                        ToolLoopIteration.ToolRequest(
                            toolName = toolName,
                            arguments = arguments,
                            modelResponse = json
                        )
                    }
                }
                "text" -> {
                    val text = root.get("text")?.takeUnless { it.isJsonNull }?.asString
                    text?.let { ToolLoopIteration.FinalResponse(it) }
                }
                else -> ToolLoopIteration.Error("Unknown response type", json)
            }
        }.getOrNull()
    }

    external fun nativeAsmHealth(): Int
    external fun nativeAbiMask(): Int
    external fun nativeContextInit(ctiPath: String, maxTokens: Int): Long
    external fun nativeInvokeTool(contextId: Long, toolName: String, arguments: String): String
    external fun nativeRunToolLoop(
        contextId: Long,
        prompt: String,
        maxIterations: Int
    ): String
    external fun nativeContextCleanup(contextId: Long)

    suspend fun executeToolLoop(
        prompt: String,
        maxIterations: Int = 10,
        onToolInvoke: suspend (toolName: String, arguments: String) -> String
    ): ToolLoopResult = withContext(Dispatchers.Default) {
        val iterations = mutableListOf<ToolLoopIteration>()
        var currentPrompt = prompt
        var contextId = -1L

        try {
            val ctiPath = getCtiPath()
            contextId = nativeContextInit(ctiPath, 4096)

            if (!isNativeAssemblerCoreReady(contextId)) {
                return@withContext ToolLoopResult.Error("Native kernel not ready")
            }

            for (iteration in 0 until maxIterations) {
                Log.d(TAG, "Tool loop iteration $iteration")
                val turnResult = nativeRunToolLoop(contextId, currentPrompt, 1)
                val turnData = parseToolResponse(turnResult)
                if (turnData == null) {
                    Log.w(TAG, "Failed to parse tool response: $turnResult")
                    iterations.add(ToolLoopIteration.Error("Parse failed", turnResult))
                    break
                }

                iterations.add(turnData)

                if (turnData !is ToolLoopIteration.ToolRequest) {
                    Log.d(TAG, "Model finished at iteration $iteration")
                    break
                }

                val toolResult = onToolInvoke(turnData.toolName, turnData.arguments)
                Log.d(TAG, "Tool ${turnData.toolName} returned: ${toolResult.take(100)}")

                currentPrompt = """
                    ${turnData.modelResponse}

                    Tool "${turnData.toolName}" returned:
                    $toolResult
                """.trimIndent()
            }

            ToolLoopResult.Success(
                iterations = iterations,
                totalIterations = iterations.size,
                finalResponse = (iterations.lastOrNull() as? ToolLoopIteration.FinalResponse)?.text ?: ""
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
            health >= 8
        }.getOrDefault(false)
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

fun isNativeAssemblerCoreReady(): Boolean {
    return runCatching {
        false
    }.getOrDefault(false)
}
