package com.rafgittools.data.network

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.URI
import java.util.Locale
import java.util.UUID

/**
 * Fail-closed application-layer egress guard.
 *
 * Boundary:
 * - Protects requests that traverse an OkHttpClient carrying this interceptor.
 * - Does not claim kernel firewall, device-wide VPN, NAT/PAT visibility, DNS integrity,
 *   or coverage of sockets opened outside the guarded client.
 * - Logs metadata only: no Authorization header, cookies, query string, path, or body.
 */
enum class RafNetworkGuardMode {
    AUDIT,
    ENFORCE
}

data class RafNetworkEndpointRule(
    val id: String,
    val host: String,
    val schemes: Set<String>,
    /** null means any TCP port for this exact host. */
    val ports: Set<Int>? = null
)

data class RafNetworkDecision(
    val allowed: Boolean,
    val ruleId: String,
    val reason: String
)

enum class RafNetworkAuditPhase {
    PRE,
    POST,
    BLOCK,
    ERROR
}

data class RafNetworkAuditEvent(
    val eventId: String,
    val phase: RafNetworkAuditPhase,
    val wallTimeMs: Long,
    val monotonicNs: Long,
    val method: String,
    val scheme: String,
    val host: String,
    val port: Int,
    val requestBytes: Long,
    val responseBytes: Long?,
    val action: String,
    val ruleId: String,
    val reason: String,
    val errorClass: String? = null
) {
    fun toCanonicalLogLine(): String = buildString {
        append("event_id=").append(safe(eventId))
        append("|phase=").append(phase.name)
        append("|wall_ms=").append(wallTimeMs)
        append("|mono_ns=").append(monotonicNs)
        append("|adapter=okhttp")
        append("|direction=EGRESS")
        append("|method=").append(safe(method))
        append("|scheme=").append(safe(scheme))
        append("|host=").append(safe(host))
        append("|port=").append(port)
        append("|request_bytes=").append(requestBytes)
        append("|response_bytes=").append(responseBytes ?: -1L)
        append("|action=").append(safe(action))
        append("|rule_id=").append(safe(ruleId))
        append("|reason=").append(safe(reason))
        append("|nat_state=TOKEN_VAZIO_APP_LAYER")
        append("|pat_state=TOKEN_VAZIO_APP_LAYER")
        append("|payload=REDACTED_METADATA_ONLY")
        errorClass?.let { append("|error=").append(safe(it)) }
    }

    private fun safe(value: String): String =
        value.replace("|", "_").replace("\n", "_").replace("\r", "_")
}

fun interface RafNetworkAuditSink {
    fun emit(event: RafNetworkAuditEvent)
}

object AndroidRafNetworkAuditSink : RafNetworkAuditSink {
    override fun emit(event: RafNetworkAuditEvent) {
        Log.i("RafNetGuard", event.toCanonicalLogLine())
    }
}

class RafNetworkGuardPolicy private constructor(
    private val mode: RafNetworkGuardMode,
    private val rules: List<RafNetworkEndpointRule>
) {
    fun evaluate(url: HttpUrl): RafNetworkDecision {
        val scheme = url.scheme.lowercase(Locale.US)
        val host = url.host.lowercase(Locale.US)
        val port = url.port

        val exact = rules.firstOrNull { rule ->
            rule.host == host &&
                scheme in rule.schemes &&
                (rule.ports == null || port in rule.ports)
        }

        if (exact != null) {
            return RafNetworkDecision(
                allowed = true,
                ruleId = exact.id,
                reason = "EXPLICIT_ENDPOINT_ALLOW"
            )
        }

        val reason = when {
            scheme == "http" && !isLoopback(host) -> "CLEARTEXT_REMOTE_DENY"
            rules.none { it.host == host } -> "HOST_NOT_AUTHORIZED"
            rules.none { it.host == host && scheme in it.schemes } -> "SCHEME_NOT_AUTHORIZED"
            else -> "PORT_NOT_AUTHORIZED"
        }

        return RafNetworkDecision(
            allowed = mode == RafNetworkGuardMode.AUDIT,
            ruleId = if (mode == RafNetworkGuardMode.AUDIT) "AUDIT_ONLY" else "DEFAULT_DENY",
            reason = reason
        )
    }

    companion object {
        fun forBaseUrl(
            baseUrl: String,
            mode: RafNetworkGuardMode = RafNetworkGuardMode.ENFORCE,
            extraRules: List<RafNetworkEndpointRule> = emptyList()
        ): RafNetworkGuardPolicy {
            val uri = URI(baseUrl)
            val host = requireNotNull(uri.host) { "baseUrl must contain a host" }
                .lowercase(Locale.US)
            val scheme = requireNotNull(uri.scheme) { "baseUrl must contain a scheme" }
                .lowercase(Locale.US)
            require(scheme == "https" || isLoopback(host)) {
                "Remote baseUrl must use HTTPS"
            }

            val effectivePort = when {
                uri.port > 0 -> uri.port
                scheme == "https" -> 443
                else -> 80
            }

            val baseRule = RafNetworkEndpointRule(
                id = "BASE_URL",
                host = host,
                schemes = setOf(scheme),
                ports = setOf(effectivePort)
            )
            val loopback = listOf(
                RafNetworkEndpointRule("LOOPBACK_V4", "127.0.0.1", setOf("http", "https"), null),
                RafNetworkEndpointRule("LOOPBACK_HOST", "localhost", setOf("http", "https"), null),
                RafNetworkEndpointRule("LOOPBACK_V6", "::1", setOf("http", "https"), null)
            )
            return RafNetworkGuardPolicy(
                mode = mode,
                rules = (listOf(baseRule) + loopback + extraRules).distinctBy { it.id }
            )
        }

        private fun isLoopback(host: String): Boolean =
            host == "127.0.0.1" || host == "localhost" || host == "::1"
    }
}

class RafNetworkGuardInterceptor(
    private val policy: RafNetworkGuardPolicy,
    private val sink: RafNetworkAuditSink = AndroidRafNetworkAuditSink
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val decision = policy.evaluate(url)
        val eventId = UUID.randomUUID().toString()
        val requestBytes = request.body?.contentLength() ?: 0L

        if (!decision.allowed) {
            sink.emit(
                event(
                    eventId = eventId,
                    phase = RafNetworkAuditPhase.BLOCK,
                    request = request,
                    requestBytes = requestBytes,
                    responseBytes = null,
                    action = "DENY",
                    decision = decision
                )
            )
            throw IOException(
                "RafNetGuard blocked egress host=${url.host} port=${url.port}: ${decision.reason}"
            )
        }

        sink.emit(
            event(
                eventId = eventId,
                phase = RafNetworkAuditPhase.PRE,
                request = request,
                requestBytes = requestBytes,
                responseBytes = null,
                action = "ALLOW",
                decision = decision
            )
        )

        return try {
            val response = chain.proceed(request)
            sink.emit(
                event(
                    eventId = eventId,
                    phase = RafNetworkAuditPhase.POST,
                    request = request,
                    requestBytes = requestBytes,
                    responseBytes = response.body?.contentLength(),
                    action = "ALLOW",
                    decision = decision
                )
            )
            response
        } catch (error: IOException) {
            sink.emit(
                event(
                    eventId = eventId,
                    phase = RafNetworkAuditPhase.ERROR,
                    request = request,
                    requestBytes = requestBytes,
                    responseBytes = null,
                    action = "ERROR",
                    decision = decision,
                    errorClass = error.javaClass.simpleName
                )
            )
            throw error
        }
    }

    private fun event(
        eventId: String,
        phase: RafNetworkAuditPhase,
        request: okhttp3.Request,
        requestBytes: Long,
        responseBytes: Long?,
        action: String,
        decision: RafNetworkDecision,
        errorClass: String? = null
    ): RafNetworkAuditEvent = RafNetworkAuditEvent(
        eventId = eventId,
        phase = phase,
        wallTimeMs = System.currentTimeMillis(),
        monotonicNs = System.nanoTime(),
        method = request.method,
        scheme = request.url.scheme,
        host = request.url.host,
        port = request.url.port,
        requestBytes = requestBytes,
        responseBytes = responseBytes,
        action = action,
        ruleId = decision.ruleId,
        reason = decision.reason,
        errorClass = errorClass
    )
}
