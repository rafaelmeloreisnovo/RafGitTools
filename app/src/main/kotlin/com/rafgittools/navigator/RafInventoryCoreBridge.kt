package com.rafgittools.navigator

/** Android/JNI boundary for metadata-only InventarioRAF analysis. */
internal object RafInventoryCoreBridge {
    data class Result(
        val pathComponents: Int,
        val passMask: Int,
        val kindFlags: Int,
        val sizeKnown: Boolean,
        val parentKnown: Boolean,
        val tokens: List<String>
    )

    init {
        System.loadLibrary("rafinventory")
    }

    private external fun nativeAnalyze(
        path: String,
        mime: String,
        isDirectory: Boolean,
        sizeKnown: Boolean,
        reportedBytes: Long,
        parentKnown: Boolean
    ): Array<String>

    fun analyze(
        path: String,
        mime: String,
        isDirectory: Boolean,
        reportedBytes: Long?,
        parentKnown: Boolean
    ): Result {
        require(reportedBytes == null || reportedBytes >= 0L) {
            "TOKEN_VAZIO_UNSAFE_SIZE"
        }
        val raw = nativeAnalyze(
            path = path,
            mime = mime,
            isDirectory = isDirectory,
            sizeKnown = reportedBytes != null,
            reportedBytes = reportedBytes ?: 0L,
            parentKnown = parentKnown
        )
        require(raw.size >= 7) { "TOKEN_VAZIO_NATIVE_RESULT_TRUNCATED" }
        val status = raw[0].toIntOrNull() ?: error("TOKEN_VAZIO_NATIVE_STATUS")
        require(status == 0) { "TOKEN_VAZIO_NATIVE_REJECTED:$status" }
        val expectedTokens = raw[1].toIntOrNull()
            ?: error("TOKEN_VAZIO_NATIVE_TOKEN_COUNT")
        require(raw.size == 7 + expectedTokens) {
            "TOKEN_VAZIO_NATIVE_RESULT_TRUNCATED"
        }
        val result = Result(
            pathComponents = raw[2].toIntOrNull()
                ?: error("TOKEN_VAZIO_NATIVE_COMPONENT_COUNT"),
            passMask = raw[3].toIntOrNull()
                ?: error("TOKEN_VAZIO_NATIVE_PASS_MASK"),
            kindFlags = raw[4].toIntOrNull()
                ?: error("TOKEN_VAZIO_NATIVE_KIND_FLAGS"),
            sizeKnown = raw[5] == "1",
            parentKnown = raw[6] == "1",
            tokens = raw.drop(7)
        )
        require(result.passMask == ALL_PASSES && result.tokens.size == expectedTokens) {
            "TOKEN_VAZIO_NATIVE_ANALYSIS_INCOMPLETE"
        }
        require(result.sizeKnown == (reportedBytes != null) &&
            result.parentKnown == parentKnown) {
            "TOKEN_VAZIO_NATIVE_STATE_MISMATCH"
        }
        return result
    }

    // One constant owned by the existing singleton; no nested companion is legal here.
    const val ALL_PASSES = 0x1f
}
