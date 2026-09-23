package com.rafgittools.workspace

import com.rafgittools.bridge.RafBridgeContract

enum class WorkbenchPromptState {
    READY,
    REJECTED_EMPTY_QUESTION,
    REJECTED_EMPTY_BUNDLE,
    REJECTED_MESSAGE_LIMIT,
    REJECTED_UNKNOWN_PRIVACY
}

data class WorkbenchPromptDecision(
    val state: WorkbenchPromptState,
    val message: String? = null,
    val dataClass: String? = null,
    val reason: String? = null
) {
    val ready: Boolean get() = state == WorkbenchPromptState.READY
}

/**
 * Pure policy/composition layer for Workbench -> local model messages.
 *
 * It never truncates context. If the existing RafBridge chat contract cannot
 * carry the complete bounded prompt, the request is rejected and the caller
 * must reduce context explicitly.
 */
object WorkbenchModelPolicy {
    fun prepare(
        bundleJson: String,
        question: String,
        privacyClass: ContextPrivacyClass
    ): WorkbenchPromptDecision {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty()) {
            return WorkbenchPromptDecision(
                state = WorkbenchPromptState.REJECTED_EMPTY_QUESTION,
                reason = "question_empty"
            )
        }
        if (bundleJson.isBlank()) {
            return WorkbenchPromptDecision(
                state = WorkbenchPromptState.REJECTED_EMPTY_BUNDLE,
                reason = "context_bundle_empty"
            )
        }

        val dataClass = when (privacyClass) {
            ContextPrivacyClass.PUBLIC -> "public"
            ContextPrivacyClass.INTERNAL,
            ContextPrivacyClass.PRIVATE -> "private"
            ContextPrivacyClass.SENSITIVE -> "sensitive"
            ContextPrivacyClass.TOKEN_VAZIO -> {
                return WorkbenchPromptDecision(
                    state = WorkbenchPromptState.REJECTED_UNKNOWN_PRIVACY,
                    reason = "privacy_token_vazio"
                )
            }
        }

        val message = buildString {
            append("CONTEXT_BUNDLE_V2\n")
            append(bundleJson)
            append("\n\nUSER_QUESTION\n")
            append(cleanQuestion)
            append("\n\nBOUNDARY\n")
            append("Use only the supplied context and clearly mark uncertainty. ")
            append("Do not claim external execution or write permission.")
        }

        if (message.length > RafBridgeContract.MAX_MESSAGE_CHARS) {
            return WorkbenchPromptDecision(
                state = WorkbenchPromptState.REJECTED_MESSAGE_LIMIT,
                dataClass = dataClass,
                reason = "message_exceeds_\${RafBridgeContract.MAX_MESSAGE_CHARS}"
            )
        }

        return WorkbenchPromptDecision(
            state = WorkbenchPromptState.READY,
            message = message,
            dataClass = dataClass
        )
    }
}
