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
        return allowlistedPlans[envelope.targetRoute]?.contains(envelope.testPlanId) == true
    }

    fun knownPlans(route: SandboxRoute): Set<String> = allowlistedPlans[route].orEmpty()
}
