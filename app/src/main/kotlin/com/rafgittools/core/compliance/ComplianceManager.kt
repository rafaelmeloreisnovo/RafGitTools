package com.rafgittools.core.compliance

import java.util.Calendar
import java.util.Date

/**
 * Evidence-gated compliance reporting.
 *
 * This component does not certify the application and does not infer
 * implementation from control names, source presence or documentation.
 * Without an explicit evidence provider every standard remains NOT_ASSESSED.
 */
class ComplianceManager(
    private val evidenceProvider: ComplianceEvidenceProvider = EmptyComplianceEvidenceProvider
) {

    /** Every enum value is represented, preventing enum/map drift. */
    fun getComplianceStatus(): Map<ComplianceStandard, ComplianceStatus> =
        ComplianceStandard.entries.associateWith(::evaluateStandard)

    fun generateComplianceReport(standard: ComplianceStandard? = null): ComplianceReport {
        val all = getComplianceStatus()
        val statuses = if (standard == null) all else all.filterKeys { it == standard }
        return ComplianceReport(
            generatedDate = Date(),
            statuses = statuses,
            gaps = identifyComplianceGaps(statuses),
            recommendations = generateRecommendations(statuses),
            nextReviewDate = calculateNextReviewDate(),
            claimAllowed = false
        )
    }

    /**
     * Control catalogue only. `implemented=false` means no evidence has been
     * supplied to this process; it is not a finding of non-compliance.
     */
    fun getSecurityControls(): List<SecurityControl> = listOf(
        SecurityControl(
            id = "A.8.3",
            name = "Information access restriction",
            description = "Access restrictions require repository and runtime evidence",
            implemented = false,
            standard = ComplianceStandard.ISO_27001
        ),
        SecurityControl(
            id = "A.8.5",
            name = "Secure authentication",
            description = "Authentication effectiveness requires test and configuration evidence",
            implemented = false,
            standard = ComplianceStandard.ISO_27001
        ),
        SecurityControl(
            id = "A.8.24",
            name = "Use of cryptography",
            description = "Cryptography claims require algorithm, key-management and runtime evidence",
            implemented = false,
            standard = ComplianceStandard.ISO_27001
        ),
        SecurityControl(
            id = "AC-1",
            name = "Access Control Policy and Procedures",
            description = "NIST access-control evidence has not been attached",
            implemented = false,
            standard = ComplianceStandard.NIST_CSF
        ),
        SecurityControl(
            id = "AU-2",
            name = "Audit Events",
            description = "Audit-event coverage requires an observed event inventory",
            implemented = false,
            standard = ComplianceStandard.NIST_CSF
        ),
        SecurityControl(
            id = "IA-2",
            name = "Identification and Authentication",
            description = "Identity assurance requires observed authentication evidence",
            implemented = false,
            standard = ComplianceStandard.NIST_CSF
        ),
        SecurityControl(
            id = "SC-13",
            name = "Cryptographic Protection",
            description = "Cryptographic protection requires configuration and runtime evidence",
            implemented = false,
            standard = ComplianceStandard.NIST_CSF
        ),
        SecurityControl(
            id = "MSTG-STORAGE-1",
            name = "Secure Data Storage",
            description = "Storage safety requires device and application evidence",
            implemented = false,
            standard = ComplianceStandard.OWASP_MASVS
        ),
        SecurityControl(
            id = "MSTG-CRYPTO-1",
            name = "Strong Cryptography",
            description = "Cryptography requires implementation and test evidence",
            implemented = false,
            standard = ComplianceStandard.OWASP_MASVS
        ),
        SecurityControl(
            id = "MSTG-AUTH-1",
            name = "Secure Authentication",
            description = "Authentication requires adversarial runtime evidence",
            implemented = false,
            standard = ComplianceStandard.OWASP_MASVS
        ),
        SecurityControl(
            id = "MSTG-NETWORK-1",
            name = "Secure Communication",
            description = "TLS requires protocol, certificate and hostname-verification evidence",
            implemented = false,
            standard = ComplianceStandard.OWASP_MASVS
        )
    )

    fun getPrivacyControls(): List<PrivacyControl> = listOf(
        PrivacyControl(
            id = "GDPR-Art15",
            name = "Right of Access",
            description = "Access-request handling requires operational evidence",
            implemented = false,
            regulation = PrivacyRegulation.GDPR
        ),
        PrivacyControl(
            id = "GDPR-Art17",
            name = "Right to Erasure",
            description = "Erasure handling requires storage and deletion evidence",
            implemented = false,
            regulation = PrivacyRegulation.GDPR
        ),
        PrivacyControl(
            id = "GDPR-Art20",
            name = "Right to Data Portability",
            description = "Portability requires export-format and delivery evidence",
            implemented = false,
            regulation = PrivacyRegulation.GDPR
        ),
        PrivacyControl(
            id = "CCPA-1798.100",
            name = "Right to Know",
            description = "Disclosure handling requires operational evidence",
            implemented = false,
            regulation = PrivacyRegulation.CCPA
        ),
        PrivacyControl(
            id = "CCPA-1798.105",
            name = "Right to Delete",
            description = "Deletion handling requires operational evidence",
            implemented = false,
            regulation = PrivacyRegulation.CCPA
        ),
        PrivacyControl(
            id = "ISO27701-5.2.1",
            name = "Consent",
            description = "Consent lifecycle requires provenance and revocation evidence",
            implemented = false,
            regulation = PrivacyRegulation.ISO_27701
        ),
        PrivacyControl(
            id = "ISO27701-5.2.2",
            name = "Purpose Limitation",
            description = "Purpose limitation requires data-flow evidence",
            implemented = false,
            regulation = PrivacyRegulation.ISO_27701
        )
    )

    /** Values are placeholders until evidence references are supplied. */
    fun getQualityMetrics(): QualityMetrics = QualityMetrics(
        testCoverage = 0.0,
        bugDensity = 0.0,
        criticalVulnerabilities = 0,
        codeQualityScore = 0.0,
        performanceScore = 0.0,
        userSatisfactionScore = 0.0,
        evidenceRefs = emptyList()
    )

    private fun evaluateStandard(standard: ComplianceStandard): ComplianceStatus {
        val evidence = evidenceProvider.evidenceFor(standard)
            ?: return notAssessed(standard, "TOKEN_VAZIO: no evidence package supplied")

        if (!evidence.isStructurallyValid()) {
            return notAssessed(standard, "TOKEN_VAZIO: malformed evidence package")
        }

        val percentage = (evidence.satisfiedCriteria * 100) / evidence.totalCriteria
        val level = when {
            percentage >= evidence.fullThreshold -> ComplianceLevel.FULLY_COMPLIANT
            percentage >= evidence.substantialThreshold -> ComplianceLevel.SUBSTANTIALLY_COMPLIANT
            percentage > 0 -> ComplianceLevel.PARTIALLY_COMPLIANT
            else -> ComplianceLevel.NON_COMPLIANT
        }

        return ComplianceStatus(
            level = level,
            percentage = percentage,
            lastAuditDate = evidence.observedAt,
            nextAuditDate = calculateNextReviewDate(evidence.observedAt),
            findings = evidence.findings,
            assessmentState = AssessmentState.OBSERVED,
            claimAllowed = false,
            evidenceRefs = evidence.evidenceRefs
        )
    }

    private fun notAssessed(standard: ComplianceStandard, reason: String): ComplianceStatus =
        ComplianceStatus(
            level = ComplianceLevel.NOT_ASSESSED,
            percentage = 0,
            lastAuditDate = Date(0),
            nextAuditDate = calculateNextReviewDate(),
            findings = listOf("${standard.displayName}: $reason"),
            assessmentState = AssessmentState.TOKEN_VAZIO,
            claimAllowed = false,
            evidenceRefs = emptyList()
        )

    private fun identifyComplianceGaps(
        statuses: Map<ComplianceStandard, ComplianceStatus>
    ): List<ComplianceGap> = statuses
        .filterValues { it.level != ComplianceLevel.FULLY_COMPLIANT }
        .map { (standard, status) ->
            ComplianceGap(
                standard = standard,
                currentLevel = status.level,
                targetLevel = ComplianceLevel.FULLY_COMPLIANT,
                description = if (status.level == ComplianceLevel.NOT_ASSESSED) {
                    "Evidence gap for ${standard.displayName}; compliance is not assessed"
                } else {
                    "Observed gap in ${standard.displayName}"
                },
                priority = when {
                    status.level == ComplianceLevel.NOT_ASSESSED -> Priority.HIGH
                    status.percentage < 80 -> Priority.HIGH
                    else -> Priority.MEDIUM
                }
            )
        }

    private fun generateRecommendations(
        statuses: Map<ComplianceStandard, ComplianceStatus>
    ): List<String> = statuses.mapNotNull { (standard, status) ->
        when {
            status.level == ComplianceLevel.NOT_ASSESSED ->
                "Collect immutable evidence for ${standard.displayName}; do not claim conformity"
            status.level != ComplianceLevel.FULLY_COMPLIANT ->
                "Address observed ${standard.displayName} gaps with linked evidence and re-assessment"
            else -> null
        }
    }

    private fun calculateNextReviewDate(from: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = from
        calendar.add(Calendar.MONTH, 3)
        return calendar.time
    }
}

fun interface ComplianceEvidenceProvider {
    fun evidenceFor(standard: ComplianceStandard): ComplianceEvidence?
}

object EmptyComplianceEvidenceProvider : ComplianceEvidenceProvider {
    override fun evidenceFor(standard: ComplianceStandard): ComplianceEvidence? = null
}

data class ComplianceEvidence(
    val standard: ComplianceStandard,
    val satisfiedCriteria: Int,
    val totalCriteria: Int,
    val evidenceRefs: List<String>,
    val observedAt: Date,
    val findings: List<String> = emptyList(),
    val fullThreshold: Int = 95,
    val substantialThreshold: Int = 80
) {
    fun isStructurallyValid(): Boolean =
        totalCriteria > 0 &&
            satisfiedCriteria in 0..totalCriteria &&
            evidenceRefs.isNotEmpty() &&
            evidenceRefs.none { it.isBlank() } &&
            fullThreshold in 1..100 &&
            substantialThreshold in 1..fullThreshold
}

enum class ComplianceStandard(val displayName: String) {
    ISO_27001("ISO/IEC 27001 - Information Security"),
    ISO_27701("ISO/IEC 27701 - Privacy Management"),
    ISO_9001("ISO 9001 - Quality Management"),
    NIST_CSF("NIST Cybersecurity Framework"),
    OWASP_MASVS("OWASP Mobile Security"),
    GDPR("GDPR - EU Privacy Regulation"),
    CCPA("CCPA - California Privacy"),
    IEEE_730("IEEE 730 - Software QA"),
    IEEE_828("IEEE 828 - Configuration Management")
}

enum class ComplianceLevel {
    NOT_ASSESSED,
    FULLY_COMPLIANT,
    SUBSTANTIALLY_COMPLIANT,
    PARTIALLY_COMPLIANT,
    NON_COMPLIANT
}

enum class AssessmentState {
    TOKEN_VAZIO,
    OBSERVED
}

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

enum class PrivacyRegulation {
    GDPR,
    CCPA,
    LGPD,
    PIPEDA,
    ISO_27701
}

data class ComplianceStatus(
    val level: ComplianceLevel,
    val percentage: Int,
    val lastAuditDate: Date,
    val nextAuditDate: Date,
    val findings: List<String>,
    val assessmentState: AssessmentState = AssessmentState.TOKEN_VAZIO,
    val claimAllowed: Boolean = false,
    val evidenceRefs: List<String> = emptyList()
)

data class ComplianceReport(
    val generatedDate: Date,
    val statuses: Map<ComplianceStandard, ComplianceStatus>,
    val gaps: List<ComplianceGap>,
    val recommendations: List<String>,
    val nextReviewDate: Date,
    val claimAllowed: Boolean = false
)

data class ComplianceGap(
    val standard: ComplianceStandard,
    val currentLevel: ComplianceLevel,
    val targetLevel: ComplianceLevel,
    val description: String,
    val priority: Priority
)

data class SecurityControl(
    val id: String,
    val name: String,
    val description: String,
    val implemented: Boolean,
    val standard: ComplianceStandard,
    val evidenceRefs: List<String> = emptyList()
)

data class PrivacyControl(
    val id: String,
    val name: String,
    val description: String,
    val implemented: Boolean,
    val regulation: PrivacyRegulation,
    val evidenceRefs: List<String> = emptyList()
)

data class QualityMetrics(
    val testCoverage: Double,
    val bugDensity: Double,
    val criticalVulnerabilities: Int,
    val codeQualityScore: Double,
    val performanceScore: Double,
    val userSatisfactionScore: Double,
    val evidenceRefs: List<String> = emptyList()
)
