package com.rafgittools.sandbox

/**
 * Fail-closed route policy.
 *
 * The envelope contains a test-plan identifier only. A JSON/envelope never
 * carries executable shell, so remote data cannot become a command directly.
 */
object SandboxExchangePlanner {
    private val allowlistedPlans = mapOf(
        SandboxRoute.VECTRA_SANDBOX to setOf(
            "VECTRA_ARMV7_FREESTANDING_KAT_V1",
            "VECTRA_ANDROID_VM_SMOKE_V1"
        ),
        SandboxRoute.PCR_SANDBOX to setOf(
            "PCR_ANDROID32_ABI_SMOKE_V1",
            "PCR_FREESTANDING_OBJECT_AUDIT_V1"
        ),
        SandboxRoute.RAFPOLIMATA_HANDOFF to setOf(
            "RAFPOLIMATA_CROSS_ARCH_OBJECT_AUDIT_V1",
            "RAFPOLIMATA_RECEIPT_IMPORT_V1"
        ),
        SandboxRoute.PRIVATE_CUSTODY_ONLY to setOf(
            "PRIVATE_CUSTODY_RECORD_ONLY_V1"
        )
    )

    fun planAllowed(envelope: SandboxExchangeEnvelope): Boolean {
        val validation = SandboxExchangeProtocol.validate(envelope)
        if (!validation.allowed) return false
        if (allowlistedPlans[envelope.targetRoute]?.contains(envelope.testPlanId) != true) {
            return false
        }

        val classes = envelope.artifacts.map { it.artifactClass }.toSet()
        return when (envelope.targetRoute) {
            SandboxRoute.VECTRA_SANDBOX,
            SandboxRoute.PCR_SANDBOX ->
                classes.all {
                    it == SandboxArtifactClass.SOURCE_PATCH ||
                        it == SandboxArtifactClass.TEST_FIXTURE ||
                        it == SandboxArtifactClass.MANIFEST
                }

            SandboxRoute.RAFPOLIMATA_HANDOFF ->
                SandboxArtifactClass.RECEIPT in classes &&
                    classes.all {
                        it == SandboxArtifactClass.SOURCE_PATCH ||
                            it == SandboxArtifactClass.TEST_FIXTURE ||
                            it == SandboxArtifactClass.MANIFEST ||
                            it == SandboxArtifactClass.RECEIPT ||
                            it == SandboxArtifactClass.BENCHMARK_RESULT
                    }

            SandboxRoute.PRIVATE_CUSTODY_ONLY -> true
        }
    }

    fun knownPlans(route: SandboxRoute): Set<String> = allowlistedPlans[route].orEmpty()
}
