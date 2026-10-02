package com.rafgittools.workspace

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test

class FreestandingModulePipelineTest {
    private val strictFlags =
        PipelineFlags.SOURCE_BOUND or
            PipelineFlags.AUTHORITY_BOUND or
            PipelineFlags.FAIL_CLOSED or
            PipelineFlags.SHADOW_GUARD

    @Test
    fun all_continue_completes_in_stable_iterative_order() {
        val order = mutableListOf<Int>()
        val pipeline = FreestandingModulePipeline(
            arrayOf(
                stage(11, ModuleKind.ROUTE) { context ->
                    order += context.lastModuleCode
                    PipelineState.CONTINUE
                },
                stage(12, ModuleKind.AUDIO_DSP) { context ->
                    order += context.lastModuleCode
                    PipelineState.CONTINUE
                }
            )
        )

        val receipt = pipeline.run(context())

        assertThat(receipt.state).isEqualTo(PipelineState.COMPLETED)
        assertThat(receipt.executedStages).isEqualTo(2)
        assertThat(order).containsExactly(ModuleKind.ROUTE.code, ModuleKind.AUDIO_DSP.code).inOrder()
        assertThat(receipt.claimAllowed).isFalse()
    }

    @Test
    fun token_vazio_stops_pipeline_without_promoting_following_stage() {
        var downstreamExecuted = false
        val pipeline = FreestandingModulePipeline(
            arrayOf(
                stage(21, ModuleKind.CRYPTO_RMR) { context ->
                    context.warningMask = context.warningMask or PipelineWarnings.EVIDENCE_MISSING
                    PipelineState.TOKEN_VAZIO
                },
                stage(22, ModuleKind.VM_KERNEL) {
                    downstreamExecuted = true
                    PipelineState.CONTINUE
                }
            )
        )

        val receipt = pipeline.run(context())

        assertThat(receipt.state).isEqualTo(PipelineState.TOKEN_VAZIO)
        assertThat(receipt.warningMask and PipelineWarnings.TOKEN_VAZIO).isNotEqualTo(0)
        assertThat(receipt.warningMask and PipelineWarnings.EVIDENCE_MISSING).isNotEqualTo(0)
        assertThat(receipt.executedStages).isEqualTo(1)
        assertThat(downstreamExecuted).isFalse()
    }

    @Test
    fun missing_required_flag_fails_closed_before_stage_execution() {
        var executed = false
        val descriptor = ModuleDescriptor(
            moduleId = 31,
            kind = ModuleKind.PLATFORM_RUNTIME,
            boundary = ModuleBoundary.PLATFORM_GATE,
            requiredFlags = strictFlags or PipelineFlags.LOW_ALLOCATION
        )
        val pipeline = FreestandingModulePipeline(
            arrayOf(object : FreestandingStage {
                override val descriptor = descriptor
                override fun execute(context: PipelineContext): PipelineState {
                    executed = true
                    return PipelineState.CONTINUE
                }
            })
        )

        val receipt = pipeline.run(context(flags = strictFlags))

        assertThat(receipt.state).isEqualTo(PipelineState.BLOCKED)
        assertThat(receipt.warningMask and PipelineWarnings.INCOHERENT_FLAGS).isNotEqualTo(0)
        assertThat(receipt.executedStages).isEqualTo(0)
        assertThat(executed).isFalse()
    }

    @Test
    fun duplicate_module_ids_are_rejected_before_execution() {
        val duplicate = arrayOf(
            stage(41, ModuleKind.ROUTE) { PipelineState.CONTINUE },
            stage(41, ModuleKind.POLIMATA_L0) { PipelineState.CONTINUE }
        )

        val result = runCatching { FreestandingModulePipeline(duplicate) }

        assertThat(result.isFailure).isTrue()
    }

    private fun context(flags: Int = strictFlags): PipelineContext = PipelineContext(
        payload = ByteBuffer.allocateDirect(64),
        flags = flags
    )

    private fun stage(
        id: Int,
        kind: ModuleKind,
        block: (PipelineContext) -> PipelineState
    ): FreestandingStage {
        val descriptor = ModuleDescriptor(
            moduleId = id,
            kind = kind,
            boundary = ModuleBoundary.FREESTANDING_CORE,
            requiredFlags = strictFlags
        )
        return object : FreestandingStage {
            override val descriptor = descriptor
            override fun execute(context: PipelineContext): PipelineState = block(context)
        }
    }
}
