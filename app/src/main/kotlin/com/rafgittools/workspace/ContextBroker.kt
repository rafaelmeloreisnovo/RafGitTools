package com.rafgittools.workspace

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContextPrivacyClass {
    PUBLIC,
    INTERNAL,
    PRIVATE,
    SENSITIVE,
    TOKEN_VAZIO
}

enum class ContextAddState {
    ADDED,
    ALREADY_PRESENT,
    REJECTED_EMPTY,
    REJECTED_BINARY,
    REJECTED_SEGMENT_LIMIT,
    REJECTED_TOTAL_LIMIT,
    REJECTED_COUNT_LIMIT
}

data class ContextAddOutcome(
    val state: ContextAddState,
    val segmentId: String? = null,
    val reason: String? = null
)

data class ContextSegment(
    val segmentId: String,
    val sourceRef: String,
    val resource: ResourceRef,
    val text: String,
    val textSha256: String,
    val privacyClass: ContextPrivacyClass
)

data class ContextBrokerState(
    val segments: List<ContextSegment> = emptyList(),
    val totalChars: Int = 0
) {
    val count: Int get() = segments.size
}

data class ContextBundleV2Intent(
    val objective: String,
    @SerializedName("request_id") val requestId: String?,
    val state: String
)

data class ContextBundleV2Resource(
    @SerializedName("resource_id") val resourceId: String,
    val provider: String,
    val source: String,
    val locator: String,
    @SerializedName("object_id") val objectId: String?,
    val ref: String?,
    val sha256: String?,
    val visibility: String,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("epistemic_state") val epistemicState: String
)

data class ContextBundleV2Segment(
    @SerializedName("segment_id") val segmentId: String,
    @SerializedName("source_ref") val sourceRef: String,
    val text: String,
    @SerializedName("text_sha256") val textSha256: String,
    @SerializedName("privacy_class") val privacyClass: String
)

data class ContextBundleV2Compatibility(
    @SerializedName("source_schema") val sourceSchema: String,
    @SerializedName("source_variant") val sourceVariant: String,
    val adapter: String,
    @SerializedName("unresolved_fields") val unresolvedFields: List<String>,
    @SerializedName("unmapped_keys") val unmappedKeys: List<String>
)

data class ContextBundleV2(
    val schema: String = "rafaelia.context_bundle.v2",
    @SerializedName("bundle_id") val bundleId: String,
    @SerializedName("created_at") val createdAt: String,
    val intent: ContextBundleV2Intent,
    @SerializedName("privacy_class") val privacyClass: String,
    @SerializedName("source_generation") val sourceGeneration: Any? = null,
    val resources: List<ContextBundleV2Resource>,
    @SerializedName("chunk_refs") val chunkRefs: List<String>,
    val segments: List<ContextBundleV2Segment>,
    @SerializedName("evidence_refs") val evidenceRefs: List<String>,
    val constraints: List<String>,
    val annotations: Map<String, Any>,
    val compatibility: ContextBundleV2Compatibility
) {
    fun toJson(gson: Gson = Gson()): String = gson.toJson(this)
}

/**
 * Explicit, in-memory context assembly for the workbench.
 *
 * No source is added automatically. Callers must invoke addText after a human
 * action. The broker never truncates silently: oversized material is rejected.
 */
@Singleton
class ContextBroker @Inject constructor() {
    companion object {
        const val MAX_SEGMENT_CHARS = 32_768
        const val MAX_TOTAL_CHARS = 131_072
        const val MAX_SEGMENTS = 16
    }

    private val _state = MutableStateFlow(ContextBrokerState())
    val state: StateFlow<ContextBrokerState> = _state.asStateFlow()

    @Synchronized
    fun addText(
        resource: ResourceRef,
        text: String,
        privacyClass: ContextPrivacyClass = resource.defaultPrivacyClass(),
        isBinary: Boolean = false,
        sourceRef: String = resource.defaultSourceRef()
    ): ContextAddOutcome {
        if (isBinary) {
            return ContextAddOutcome(
                ContextAddState.REJECTED_BINARY,
                reason = "binary_context_not_supported"
            )
        }
        if (text.isEmpty()) {
            return ContextAddOutcome(
                ContextAddState.REJECTED_EMPTY,
                reason = "empty_context"
            )
        }
        if (text.length > MAX_SEGMENT_CHARS) {
            return ContextAddOutcome(
                ContextAddState.REJECTED_SEGMENT_LIMIT,
                reason = "segment_exceeds_$MAX_SEGMENT_CHARS"
            )
        }

        val textSha = sha256(text)
        val segmentId = segmentId(resource, sourceRef, textSha)
        val current = _state.value
        if (current.segments.any { it.segmentId == segmentId }) {
            return ContextAddOutcome(ContextAddState.ALREADY_PRESENT, segmentId)
        }
        if (current.segments.size >= MAX_SEGMENTS) {
            return ContextAddOutcome(
                ContextAddState.REJECTED_COUNT_LIMIT,
                reason = "context_count_exceeds_$MAX_SEGMENTS"
            )
        }
        if (current.totalChars + text.length > MAX_TOTAL_CHARS) {
            return ContextAddOutcome(
                ContextAddState.REJECTED_TOTAL_LIMIT,
                reason = "context_total_exceeds_$MAX_TOTAL_CHARS"
            )
        }

        val segment = ContextSegment(
            segmentId = segmentId,
            sourceRef = sourceRef,
            resource = resource,
            text = text,
            textSha256 = textSha,
            privacyClass = privacyClass
        )
        val next = current.segments + segment
        _state.value = ContextBrokerState(
            segments = next,
            totalChars = next.sumOf { it.text.length }
        )
        return ContextAddOutcome(ContextAddState.ADDED, segmentId)
    }

    @Synchronized
    fun remove(segmentId: String): Boolean {
        val current = _state.value
        val next = current.segments.filterNot { it.segmentId == segmentId }
        if (next.size == current.segments.size) return false
        _state.value = ContextBrokerState(next, next.sumOf { it.text.length })
        return true
    }

    @Synchronized
    fun clear() {
        _state.value = ContextBrokerState()
    }

    fun buildBundle(
        bundleId: String,
        objective: String,
        createdAt: String,
        requestId: String? = null,
        evidenceRefs: List<String> = emptyList(),
        constraints: List<String> = listOf(
            "context assembled only from explicit user-selected resources",
            "model output is not execution permission"
        ),
        annotations: Map<String, Any> = mapOf(
            "claim_allowed" to false,
            "context_broker" to "ContextBroker"
        )
    ): ContextBundleV2 {
        require(bundleId.isNotBlank()) { "bundleId must not be blank" }
        require(objective.isNotBlank()) { "objective must not be blank" }
        require(createdAt.isNotBlank()) { "createdAt must not be blank" }

        val snapshot = _state.value
        val resources = snapshot.segments
            .map { it.resource }
            .distinctBy { it.stableTabId() }
            .map { resource ->
                ContextBundleV2Resource(
                    resourceId = resource.stableTabId(),
                    provider = resource.provider,
                    source = resource.repositoryOrCorpus,
                    locator = resource.pathOrLocator,
                    objectId = resource.objectId,
                    ref = resource.refOrGeneration,
                    sha256 = null,
                    visibility = resource.visibility.name,
                    mimeType = null,
                    epistemicState = "SOURCE_OBSERVED"
                )
            }

        return ContextBundleV2(
            bundleId = bundleId,
            createdAt = createdAt,
            intent = ContextBundleV2Intent(
                objective = objective,
                requestId = requestId,
                state = "DECLARED"
            ),
            privacyClass = aggregatePrivacy(snapshot.segments).name,
            resources = resources,
            chunkRefs = snapshot.segments.map { it.sourceRef },
            segments = snapshot.segments.map { segment ->
                ContextBundleV2Segment(
                    segmentId = segment.segmentId,
                    sourceRef = segment.sourceRef,
                    text = segment.text,
                    textSha256 = segment.textSha256,
                    privacyClass = segment.privacyClass.name
                )
            },
            evidenceRefs = evidenceRefs,
            constraints = constraints,
            annotations = annotations,
            compatibility = ContextBundleV2Compatibility(
                sourceSchema = "rafaelia.context_bundle.v2",
                sourceVariant = "native-v2",
                adapter = "ContextBroker",
                unresolvedFields = emptyList(),
                unmappedKeys = emptyList()
            )
        )
    }

    private fun ResourceRef.defaultSourceRef(): String =
        "$provider:$repositoryOrCorpus@$refOrGeneration:$pathOrLocator"

    private fun ResourceRef.defaultPrivacyClass(): ContextPrivacyClass =
        when (visibility) {
            ResourceVisibility.PUBLIC -> ContextPrivacyClass.PUBLIC
            ResourceVisibility.INTERNAL -> ContextPrivacyClass.INTERNAL
            ResourceVisibility.LOCAL_ONLY,
            ResourceVisibility.PRIVATE -> ContextPrivacyClass.PRIVATE
            ResourceVisibility.TOKEN_VAZIO -> ContextPrivacyClass.TOKEN_VAZIO
        }

    private fun aggregatePrivacy(segments: List<ContextSegment>): ContextPrivacyClass {
        if (segments.isEmpty()) return ContextPrivacyClass.TOKEN_VAZIO
        if (segments.any { it.privacyClass == ContextPrivacyClass.TOKEN_VAZIO }) {
            return ContextPrivacyClass.TOKEN_VAZIO
        }
        return segments.maxBy { privacyRank(it.privacyClass) }.privacyClass
    }

    private fun privacyRank(value: ContextPrivacyClass): Int = when (value) {
        ContextPrivacyClass.PUBLIC -> 0
        ContextPrivacyClass.INTERNAL -> 1
        ContextPrivacyClass.PRIVATE -> 2
        ContextPrivacyClass.SENSITIVE -> 3
        ContextPrivacyClass.TOKEN_VAZIO -> 4
    }

    private fun segmentId(resource: ResourceRef, sourceRef: String, textSha: String): String =
        "seg-" + sha256(resource.stableTabId() + "\\u001f" + sourceRef + "\\u001f" + textSha).take(24)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
