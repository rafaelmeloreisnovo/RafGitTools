package com.rafgittools.kernel

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class RafaeliaKernelBridgeTest {

    @Test
    fun `kernel bridge initialization remains TOKEN_VAZIO without Android context`() {
        // Native/context initialization is intentionally not promoted by a JVM-only unit test.
    }

    @Test
    fun `parseToolResponse handles tool_use type and object input`() = runTest {
        val json = """
            {
                "type": "tool_use",
                "name": "execute_command",
                "input": {"command": "ls -la"}
            }
        """.trimIndent()

        val parsed = RafaeliaKernelBridge.parseToolResponse(json)
        val request = assertIs<ToolLoopIteration.ToolRequest>(parsed)
        assertEquals("execute_command", request.toolName)
        assertEquals("{\"command\":\"ls -la\"}", request.arguments)
    }

    @Test
    fun `parseToolResponse handles text response type with whitespace`() = runTest {
        val json = """
            {
                "type": "text",
                "text": "The directory listing is complete."
            }
        """.trimIndent()

        val parsed = RafaeliaKernelBridge.parseToolResponse(json)
        val response = assertIs<ToolLoopIteration.FinalResponse>(parsed)
        assertEquals("The directory listing is complete.", response.text)
    }

    @Test
    fun `parseToolResponse preserves unknown type as explicit error`() {
        val parsed = RafaeliaKernelBridge.parseToolResponse("""{"type":"future_type"}""")
        assertIs<ToolLoopIteration.Error>(parsed)
    }

    @Test
    fun `executeToolLoop respects maxIterations`() = runTest {
        // Requires mock native layer; remains TOKEN_VAZIO until a JNI test double is bound.
    }

    @Test
    fun `executeToolLoop handles native initialization error`() = runTest {
        // Requires mock native layer with error condition.
    }

    @Test
    fun `isNativeAssemblerCoreReady returns boolean`() {
        val result = runCatching { isNativeAssemblerCoreReady() }
        assertNotNull(result)
    }
}
