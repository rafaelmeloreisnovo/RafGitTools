package com.rafgittools.rafgitfs.sync

import java.security.MessageDigest

object RafGitFsCanonical {
    fun planPayload(
        requestId: String,
        profileId: String,
        repositoryFullName: String,
        refName: String,
        baseCommitSha: String?,
        steps: List<RafGitFsPlanStep>,
        conflicts: List<RafGitFsDiffItem>,
        generatedAt: Long
    ): String = buildString {
        append("requestId=").append(escape(requestId)).append('\n')
        append("profileId=").append(escape(profileId)).append('\n')
        append("repository=").append(escape(repositoryFullName)).append('\n')
        append("ref=").append(escape(refName)).append('\n')
        append("base=").append(baseCommitSha.orEmpty().lowercase()).append('\n')
        append("generatedAt=").append(generatedAt).append('\n')
        steps.sortedBy { it.order }.forEach { step ->
            append("step=")
                .append(step.order).append('|')
                .append(step.action.name).append('|')
                .append(escape(step.path.orEmpty())).append('|')
                .append(step.risk.name).append('|')
                .append(step.baseSha.orEmpty().lowercase()).append('|')
                .append(step.observedSha.orEmpty().lowercase()).append('|')
                .append(step.requiresApproval).append('|')
                .append(step.executableNow).append('|')
                .append(escape(step.reason)).append('\n')
        }
        conflicts.sortedBy { it.path }.forEach { item ->
            append("conflict=")
                .append(escape(item.path)).append('|')
                .append(item.kind.name).append('|')
                .append(item.localSha.orEmpty().lowercase()).append('|')
                .append(item.remoteSha.orEmpty().lowercase()).append('|')
                .append(item.evidenceState).append('\n')
        }
    }

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

    fun sanitize(value: String?): String? = value
        ?.replace(
            Regex("(?i)(token|authorization|password|secret|cookie)\\s*[:=]\\s*[^\\s,;]+"),
            "\$1=[REDACTED]"
        )
        ?.replace(Regex("gh[pousr]_[A-Za-z0-9_]{20,}"), "[REDACTED_GITHUB_TOKEN]")
        ?.take(512)

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("|", "\\|")
}

data class RafGitFsLogEvent(
    val jobId: String,
    val phase: String,
    val state: String,
    val code: String,
    val detail: String?,
    val timestamp: Long
)

class RafGitFsSanitizedLog {
    private val events = ArrayDeque<RafGitFsLogEvent>()

    @Synchronized
    fun append(event: RafGitFsLogEvent) {
        events.addLast(event.copy(detail = RafGitFsCanonical.sanitize(event.detail)))
        while (events.size > MAX_EVENTS) events.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<RafGitFsLogEvent> = events.toList()

    companion object {
        private const val MAX_EVENTS = 256
    }
}
