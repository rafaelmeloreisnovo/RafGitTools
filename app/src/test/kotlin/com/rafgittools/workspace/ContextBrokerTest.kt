package com.rafgittools.workspace

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ContextBrokerTest {
    private fun resource(
        path: String = "src/a.kt",
        visibility: ResourceVisibility = ResourceVisibility.LOCAL_ONLY
    ) = ResourceRef(
        provider = "LOCAL_GIT",
        repositoryOrCorpus = "/repo",
        refOrGeneration = "main",
        pathOrLocator = path,
        objectId = "blob-1",
        visibility = visibility
    )

    @Test
    fun addText_requires_explicit_bounded_text_and_deduplicates() {
        val broker = ContextBroker()

        val first = broker.addText(resource(), "hello")
        val duplicate = broker.addText(resource(), "hello")

        assertThat(first.state).isEqualTo(ContextAddState.ADDED)
        assertThat(duplicate.state).isEqualTo(ContextAddState.ALREADY_PRESENT)
        assertThat(broker.state.value.count).isEqualTo(1)
        assertThat(broker.state.value.totalChars).isEqualTo(5)
    }

    @Test
    fun oversized_segment_is_rejected_without_truncation() {
        val broker = ContextBroker()
        val text = "x".repeat(ContextBroker.MAX_SEGMENT_CHARS + 1)

        val outcome = broker.addText(resource(), text)

        assertThat(outcome.state).isEqualTo(ContextAddState.REJECTED_SEGMENT_LIMIT)
        assertThat(broker.state.value.segments).isEmpty()
    }

    @Test
    fun binary_and_empty_context_are_rejected() {
        val broker = ContextBroker()

        assertThat(broker.addText(resource(), "", isBinary = false).state)
            .isEqualTo(ContextAddState.REJECTED_EMPTY)
        assertThat(broker.addText(resource(), "bytes", isBinary = true).state)
            .isEqualTo(ContextAddState.REJECTED_BINARY)
    }

    @Test
    fun bundle_preserves_source_and_hash_and_is_v2() {
        val broker = ContextBroker()
        broker.addText(resource(), "bounded source")

        val bundle = broker.buildBundle(
            bundleId = "b1",
            objective = "Explain selected source",
            createdAt = "2026-09-23T04:40:00Z",
            requestId = "r1"
        )

        assertThat(bundle.schema).isEqualTo("rafaelia.context_bundle.v2")
        assertThat(bundle.resources).hasSize(1)
        assertThat(bundle.resources.single().provider).isEqualTo("LOCAL_GIT")
        assertThat(bundle.segments).hasSize(1)
        assertThat(bundle.segments.single().text).isEqualTo("bounded source")
        assertThat(bundle.segments.single().textSha256).hasLength(64)
        assertThat(bundle.privacyClass).isEqualTo("PRIVATE")
        assertThat(bundle.compatibility.sourceVariant).isEqualTo("native-v2")
    }

    @Test
    fun json_uses_contract_snake_case_fields() {
        val broker = ContextBroker()
        broker.addText(resource(), "hello")
        val json = broker.buildBundle(
            bundleId = "b1",
            objective = "Inspect",
            createdAt = "2026-09-23T04:40:00Z"
        ).toJson()

        assertThat(json).contains("\\"bundle_id\\":\\"b1\\"")
        assertThat(json).contains("\\"created_at\\":")
        assertThat(json).contains("\\"privacy_class\\":\\"PRIVATE\\"")
        assertThat(json).contains("\\"chunk_refs\\":")
        assertThat(json).contains("\\"source_variant\\":\\"native-v2\\"")
    }

    @Test
    fun unknown_privacy_is_not_promoted() {
        val broker = ContextBroker()
        broker.addText(
            resource(visibility = ResourceVisibility.TOKEN_VAZIO),
            "unknown privacy"
        )

        val bundle = broker.buildBundle(
            bundleId = "b1",
            objective = "Inspect",
            createdAt = "TOKEN_VAZIO"
        )

        assertThat(bundle.privacyClass).isEqualTo("TOKEN_VAZIO")
    }

    @Test
    fun remove_and_clear_do_not_mutate_sources() {
        val broker = ContextBroker()
        val a = broker.addText(resource("a.kt"), "a")
        broker.addText(resource("b.kt"), "bb")

        assertThat(broker.remove(a.segmentId!!)).isTrue()
        assertThat(broker.state.value.totalChars).isEqualTo(2)

        broker.clear()
        assertThat(broker.state.value.segments).isEmpty()
        assertThat(broker.state.value.totalChars).isEqualTo(0)
    }

    @Test
    fun bundle_requires_human_supplied_intent_and_identity() {
        val broker = ContextBroker()
        assertThrows(IllegalArgumentException::class.java) {
            broker.buildBundle("", "objective", "2026-09-23T04:40:00Z")
        }
        assertThrows(IllegalArgumentException::class.java) {
            broker.buildBundle("b", "", "2026-09-23T04:40:00Z")
        }
    }
}
