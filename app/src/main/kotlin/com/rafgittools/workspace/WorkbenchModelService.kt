package com.rafgittools.workspace

import android.content.Context
import com.rafgittools.bridge.RafBridgeContract
import com.rafgittools.bridge.RafBridgePrefs
import com.rafgittools.bridge.RafModelClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class WorkbenchModelResultState {
    SUCCESS,
    TOKEN_VAZIO_CONTEXT,
    REJECTED_POLICY,
    MODEL_ERROR
}

data class WorkbenchModelResult(
    val state: WorkbenchModelResultState,
    val reply: String? = null,
    val reason: String? = null,
    val bundleId: String? = null
)

/**
 * Direct internal adapter to the already configured local llamaRafaelia endpoint.
 *
 * The HTTP RafBridge service is not used as an execution bypass. Instead this
 * class reuses the same RafBridgeContract policy gate in-process, then calls
 * the existing loopback-only RafModelClient.
 */
@Singleton
class WorkbenchModelService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contextBroker: ContextBroker
) {
    suspend fun ask(question: String): WorkbenchModelResult = withContext(Dispatchers.IO) {
        val snapshot = contextBroker.state.value
        if (snapshot.segments.isEmpty()) {
            return@withContext WorkbenchModelResult(
                state = WorkbenchModelResultState.TOKEN_VAZIO_CONTEXT,
                reason = "no_explicit_context"
            )
        }

        val createdAt = utcNow()
        val bundleId = buildBundleId(question, snapshot.segments, createdAt)
        val bundle = contextBroker.buildBundle(
            bundleId = bundleId,
            objective = question.trim().ifEmpty { "TOKEN_VAZIO" },
            createdAt = createdAt,
            requestId = bundleId,
            evidenceRefs = snapshot.segments.map { it.sourceRef }
        )
        val privacy = runCatching {
            ContextPrivacyClass.valueOf(bundle.privacyClass)
        }.getOrDefault(ContextPrivacyClass.TOKEN_VAZIO)

        val prepared = WorkbenchModelPolicy.prepare(
            bundleJson = bundle.toJson(),
            question = question,
            privacyClass = privacy
        )
        if (!prepared.ready) {
            return@withContext WorkbenchModelResult(
                state = WorkbenchModelResultState.REJECTED_POLICY,
                reason = prepared.reason,
                bundleId = bundleId
            )
        }

        val envelope = JSONObject()
            .put("schema", "raf.client.envelope.v1")
            .put("request_id", bundleId)
            .put("action", "chat")
            .put("intent", question.trim())
            .put("data_class", prepared.dataClass)
            .put("source", "rafgittools-workbench")
            .put("message", prepared.message)
            .put("consent", true)

        val contract = RafBridgeContract.validate(
            envelope,
            RafBridgePrefs.allowSensitive(context)
        )
        if (!contract.allowed) {
            return@withContext WorkbenchModelResult(
                state = WorkbenchModelResultState.REJECTED_POLICY,
                reason = contract.error,
                bundleId = bundleId
            )
        }

        runCatching {
            RafModelClient().chat(
                RafBridgePrefs.getModelEndpoint(context),
                RafBridgePrefs.getModelName(context),
                contract.intent,
                contract.dataClass,
                contract.message
            )
        }.fold(
            onSuccess = { reply ->
                WorkbenchModelResult(
                    state = WorkbenchModelResultState.SUCCESS,
                    reply = reply,
                    bundleId = bundleId
                )
            },
            onFailure = { error ->
                WorkbenchModelResult(
                    state = WorkbenchModelResultState.MODEL_ERROR,
                    reason = error.message ?: error.javaClass.simpleName,
                    bundleId = bundleId
                )
            }
        )
    }

    private fun utcNow(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    private fun buildBundleId(
        question: String,
        segments: List<ContextSegment>,
        createdAt: String
    ): String {
        val material = buildString {
            append(createdAt)
            append('\u001f')
            append(question.trim())
            segments.forEach { segment ->
                append('\u001f')
                append(segment.segmentId)
                append('\u001f')
                append(segment.textSha256)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "WB-\${digest.take(24)}"
    }
}
