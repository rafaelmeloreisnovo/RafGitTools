package com.rafgittools.ui.screens.rafgitfs

import com.rafgittools.rafgitfs.remote.RafGitFsRemoteResult
import java.util.Locale

enum class RafGitFsUiEvidence {
    IDLE,
    LOADING,
    OBSERVED,
    NOT_MODIFIED,
    TOKEN_VAZIO,
    RATE_LIMITED,
    ERROR
}

data class RafGitFsUiStatus(
    val evidence: RafGitFsUiEvidence = RafGitFsUiEvidence.IDLE,
    val title: String = "Ready",
    val detail: String? = null
)

data class RafGitFsBreadcrumb(
    val label: String,
    val path: String
)

object RafGitFsUiPaths {
    const val ROOT_ROUTE_VALUE = "__root__"

    fun normalize(path: String?): String = path.orEmpty().trim().trim('/')

    fun routeValue(path: String): String = normalize(path).ifEmpty { ROOT_ROUTE_VALUE }

    fun fromRoute(value: String?): String = when (value) {
        null, "", ROOT_ROUTE_VALUE -> ""
        else -> normalize(value)
    }

    fun breadcrumbs(path: String): List<RafGitFsBreadcrumb> {
        val normalized = normalize(path)
        val result = mutableListOf(RafGitFsBreadcrumb("root", ""))
        if (normalized.isEmpty()) return result
        var current = ""
        normalized.split('/').filter { it.isNotBlank() }.forEach { segment ->
            current = if (current.isEmpty()) segment else "$current/$segment"
            result += RafGitFsBreadcrumb(segment, current)
        }
        return result
    }

    fun parent(path: String): String = normalize(path).substringBeforeLast('/', "")

    fun formatBytes(bytes: Long?): String {
        if (bytes == null) return "—"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KiB", "MiB", "GiB", "TiB")
        var value = bytes.toDouble()
        var index = -1
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }
}

fun RafGitFsRemoteResult<*>.toUiStatus(): RafGitFsUiStatus = when (this) {
    is RafGitFsRemoteResult.Observed -> RafGitFsUiStatus(
        RafGitFsUiEvidence.OBSERVED,
        "Observed",
        "GitHub response recorded with complete evidence."
    )
    is RafGitFsRemoteResult.NotModified -> RafGitFsUiStatus(
        RafGitFsUiEvidence.NOT_MODIFIED,
        "Up to date",
        "The indexed snapshot already matches the observed commit."
    )
    is RafGitFsRemoteResult.TokenVazio -> RafGitFsUiStatus(
        RafGitFsUiEvidence.TOKEN_VAZIO,
        "TOKEN_VAZIO",
        reason
    )
    is RafGitFsRemoteResult.RateLimited -> RafGitFsUiStatus(
        RafGitFsUiEvidence.RATE_LIMITED,
        "GitHub rate limit",
        buildString {
            append(message)
            retryAfterSeconds?.let { append(" Retry after ${it}s.") }
        }
    )
    is RafGitFsRemoteResult.Failure -> RafGitFsUiStatus(
        RafGitFsUiEvidence.ERROR,
        "Request failed",
        message
    )
}
