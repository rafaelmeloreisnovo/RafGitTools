package com.rafgittools.workspace

import java.nio.ByteBuffer

/**
 * OPTIONAL hosted JVM adapter/reference for the authoritative freestanding L0 core.
 *
 * Authority lives in:
 *   freestanding/orchestration/raf_orchestrator_l0.c
 *
 * This Kotlin surface is deliberately outside the L0 dependency boundary. JVM/Android
 * is NOT freestanding and must never be required for the L0 core to compile or execute.
 * It mirrors the typed fail-closed semantics for hosted integration and adapter tests.
 */
class FreestandingModulePipeline(
    private val stages: Array<FreestandingStage>
) {
    init {
        require(stages.isNotEmpty()) { "pipeline requires at least one stage" }
        validatePlan(stages)
    }

    fun run(context: PipelineContext): PipelineReceipt {
        var stageIndex = 0
        var terminalState = PipelineState.COMPLETED

        while (stageIndex < stages.size) {
            val stage = stages[stageIndex]

            if ((context.flags and stage.descriptor.requiredFlags) != stage.descriptor.requiredFlags) {
                context.warningMask = context.warningMask or PipelineWarnings.INCOHERENT_FLAGS
                terminalState = PipelineState.BLOCKED
                break
            }

            context.lastModuleCode = stage.descriptor.kind.code
            val stageState = stage.execute(context)
            context.executedStages += 1

            when (stageState) {
                PipelineState.CONTINUE -> Unit
                PipelineState.TOKEN_VAZIO -> {
                    context.warningMask = context.warningMask or PipelineWarnings.TOKEN_VAZIO
                    terminalState = PipelineState.TOKEN_VAZIO
                    break
                }
                PipelineState.BLOCKED -> {
                    terminalState = PipelineState.BLOCKED
                    break
                }
                PipelineState.FAILED -> {
                    terminalState = PipelineState.FAILED
                    break
                }
                PipelineState.COMPLETED -> {
                    terminalState = PipelineState.COMPLETED
                    break
                }
            }

            stageIndex += 1
        }

        return PipelineReceipt(
            state = terminalState,
            flags = context.flags,
            warningMask = context.warningMask,
            executedStages = context.executedStages,
            lastModuleCode = context.lastModuleCode
        )
    }

    private fun validatePlan(plan: Array<FreestandingStage>) {
        var outerIndex = 0
        while (outerIndex < plan.size) {
            require(plan[outerIndex].descriptor.moduleId > 0) { "moduleId must be positive" }

            var innerIndex = outerIndex + 1
            while (innerIndex < plan.size) {
                require(plan[outerIndex].descriptor.moduleId != plan[innerIndex].descriptor.moduleId) {
                    "moduleId must be unique inside one pipeline plan"
                }
                innerIndex += 1
            }
            outerIndex += 1
        }
    }
}

class PipelineContext(
    val payload: ByteBuffer,
    var flags: Int,
    var warningMask: Int = PipelineWarnings.NONE,
    var executedStages: Int = 0,
    var lastModuleCode: Int = 0
)

interface FreestandingStage {
    val descriptor: ModuleDescriptor
    fun execute(context: PipelineContext): PipelineState
}

data class ModuleDescriptor(
    val moduleId: Int,
    val kind: ModuleKind,
    val boundary: ModuleBoundary,
    val requiredFlags: Int
)

enum class ModuleKind(val code: Int) {
    ROUTE(1),
    POLIMATA_L0(2),
    AUDIO_DSP(3),
    CRYPTO_RMR(4),
    VM_KERNEL(5),
    PLATFORM_RUNTIME(6)
}

enum class ModuleBoundary {
    JVM_CONTROL_PLANE,
    HOSTED_ADAPTER,
    FREESTANDING_CORE,
    PLATFORM_GATE
}

enum class PipelineState {
    CONTINUE,
    COMPLETED,
    TOKEN_VAZIO,
    BLOCKED,
    FAILED
}

data class PipelineReceipt(
    val state: PipelineState,
    val flags: Int,
    val warningMask: Int,
    val executedStages: Int,
    val lastModuleCode: Int
) {
    val claimAllowed: Boolean
        get() = false
}

object PipelineFlags {
    const val NONE = 0
    const val SOURCE_BOUND = 1 shl 0
    const val AUTHORITY_BOUND = 1 shl 1
    const val FAIL_CLOSED = 1 shl 2
    const val LOW_ALLOCATION = 1 shl 3
    const val SHADOW_GUARD = 1 shl 4
}

object PipelineWarnings {
    const val NONE = 0
    const val TOKEN_VAZIO = 1 shl 0
    const val MODULE_UNAVAILABLE = 1 shl 1
    const val AUTHORITY_MISSING = 1 shl 2
    const val EVIDENCE_MISSING = 1 shl 3
    const val ABI_MISMATCH = 1 shl 4
    const val HOSTED_BOUNDARY = 1 shl 5
    const val INCOHERENT_FLAGS = 1 shl 6
}
