package com.rafgittools.workspace

import com.google.common.truth.Truth.assertThat
import com.rafgittools.bridge.RafBridgeContract
import org.junit.Test

class WorkbenchModelPolicyTest {
    @Test
    fun public_context_maps_to_public_without_truncation() {
        val decision = WorkbenchModelPolicy.prepare(
            bundleJson = """{"schema":"rafaelia.context_bundle.v2"}""",
            question = "Explain this",
            privacyClass = ContextPrivacyClass.PUBLIC
        )

        assertThat(decision.state).isEqualTo(WorkbenchPromptState.READY)
        assertThat(decision.dataClass).isEqualTo("public")
        assertThat(decision.message).contains("CONTEXT_BUNDLE_V2")
        assertThat(decision.message).contains("USER_QUESTION")
        assertThat(decision.message).contains("Explain this")
    }

    @Test
    fun private_and_internal_map_to_private() {
        assertThat(
            WorkbenchModelPolicy.prepare("{}", "q", ContextPrivacyClass.PRIVATE).dataClass
        ).isEqualTo("private")
        assertThat(
            WorkbenchModelPolicy.prepare("{}", "q", ContextPrivacyClass.INTERNAL).dataClass
        ).isEqualTo("private")
    }

    @Test
    fun sensitive_maps_to_sensitive_for_contract_to_decide() {
        val decision = WorkbenchModelPolicy.prepare(
            "{}", "q", ContextPrivacyClass.SENSITIVE
        )
        assertThat(decision.state).isEqualTo(WorkbenchPromptState.READY)
        assertThat(decision.dataClass).isEqualTo("sensitive")
    }

    @Test
    fun token_vazio_privacy_fails_closed() {
        val decision = WorkbenchModelPolicy.prepare(
            "{}", "q", ContextPrivacyClass.TOKEN_VAZIO
        )
        assertThat(decision.state).isEqualTo(WorkbenchPromptState.REJECTED_UNKNOWN_PRIVACY)
        assertThat(decision.message).isNull()
    }

    @Test
    fun blank_question_is_rejected() {
        val decision = WorkbenchModelPolicy.prepare(
            "{}", "   ", ContextPrivacyClass.PRIVATE
        )
        assertThat(decision.state).isEqualTo(WorkbenchPromptState.REJECTED_EMPTY_QUESTION)
    }

    @Test
    fun oversized_prompt_is_rejected_instead_of_truncated() {
        val oversized = "x".repeat(RafBridgeContract.MAX_MESSAGE_CHARS)
        val decision = WorkbenchModelPolicy.prepare(
            oversized,
            "question",
            ContextPrivacyClass.PRIVATE
        )
        assertThat(decision.state).isEqualTo(WorkbenchPromptState.REJECTED_MESSAGE_LIMIT)
        assertThat(decision.message).isNull()
    }
}
