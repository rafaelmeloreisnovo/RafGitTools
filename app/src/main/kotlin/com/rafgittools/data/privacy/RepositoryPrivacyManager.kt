package com.rafgittools.data.privacy

import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fail-closed bulk repository visibility manager.
 *
 * Invariants:
 * - no fork/archived/disabled/already-private repository is mutated;
 * - GitHub must explicitly report admin permission;
 * - stale inventory never authorizes a mutation: every repository is re-read live;
 * - a durable append-only receipt checkpoint exists before every PATCH;
 * - if provenance persistence is unavailable, remaining mutations stop.
 */
@Singleton
class RepositoryPrivacyManager @Inject constructor(
    private val api: RepositoryPrivacyApi,
    private val receiptStore: PrivacyReceiptStore
) {
    suspend fun loadInventory(): Result<List<RepositoryPrivacyCandidate>> = runCatching {
        val all = mutableListOf<PrivacyRepositoryDto>()
        var page = 1
        while (true) {
            check(page <= MAX_INVENTORY_PAGES) {
                "Repository inventory exceeded the defensive pagination limit; refusing partial inventory"
            }
            val batch = api.listRepositories(
                visibility = "all",
                affiliation = "owner,organization_member",
                sort = "full_name",
                direction = "asc",
                page = page,
                perPage = PAGE_SIZE
            )
            all += batch
            if (batch.size < PAGE_SIZE) break
            page++
        }
        all.distinctBy { it.id }
            .map(PrivacyRepositoryDto::toCandidate)
            .sortedWith(compareBy(RepositoryPrivacyCandidate::ownerLogin, RepositoryPrivacyCandidate::name))
    }

    suspend fun makePrivate(selected: Collection<RepositoryPrivacyCandidate>): RepositoryPrivacyBulkResult {
        val ordered = selected.distinctBy { it.id }.sortedBy { it.fullName.lowercase() }
        val createdAt = System.currentTimeMillis()
        val operationId = "visibility-$createdAt-${UUID.randomUUID()}"
        val mutations = ordered.map {
            it.mutation(
                PrivacyMutationStatus.NOT_ATTEMPTED,
                "Planned; no mutation attempted yet"
            )
        }.toMutableList()

        var checkpointSequence = 0
        var lastReceiptPath: String? = null
        var lastPersistedReceipt: RepositoryPrivacyReceipt? = null
        var provenanceState = PrivacyProvenanceState.DURABLE
        var provenanceMessage = "Append-only receipt journal initialized"
        var abortReason: String? = null

        fun buildReceipt(phase: PrivacyReceiptPhase): RepositoryPrivacyReceipt = RepositoryPrivacyReceipt(
            operationId = operationId,
            createdAtEpochMs = createdAt,
            checkpointSequence = checkpointSequence,
            phase = phase,
            requested = ordered.size,
            updated = mutations.count { it.status == PrivacyMutationStatus.UPDATED },
            failed = mutations.count { it.status == PrivacyMutationStatus.FAILED },
            skipped = mutations.count { it.status == PrivacyMutationStatus.SKIPPED },
            notAttempted = mutations.count { it.status == PrivacyMutationStatus.NOT_ATTEMPTED },
            attempting = mutations.count { it.status == PrivacyMutationStatus.ATTEMPTING },
            mutations = mutations.toList()
        )

        fun persistCheckpoint(phase: PrivacyReceiptPhase): Boolean {
            val receipt = buildReceipt(phase)
            val saved = receiptStore.save(receipt)
            if (saved.isSuccess) {
                lastReceiptPath = saved.getOrNull()
                lastPersistedReceipt = receipt
                checkpointSequence++
                return true
            }
            return false
        }

        // P0 provenance gate: no repository mutation is allowed unless the operation
        // intent itself is already durable in app-private storage.
        if (!persistCheckpoint(PrivacyReceiptPhase.PLANNED)) {
            val aborted = buildReceipt(PrivacyReceiptPhase.ABORTED)
            return RepositoryPrivacyBulkResult(
                receipt = aborted,
                receiptPath = null,
                provenanceState = PrivacyProvenanceState.FAILED_BEFORE_MUTATION,
                provenanceMessage = "Receipt journal could not be initialized; zero GitHub mutations attempted"
            )
        }

        ordered.forEachIndexed { index, candidate ->
            if (abortReason != null) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.NOT_ATTEMPTED,
                    abortReason!!
                )
                return@forEachIndexed
            }

            if (candidate.blockReason != null) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.SKIPPED,
                    candidate.blockReason
                )
                if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                    provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                    provenanceMessage = "Receipt checkpoint failed; remaining mutations aborted"
                    abortReason = provenanceMessage
                }
                return@forEachIndexed
            }

            // Live TOCTOU preflight. The inventory selected by the user is informative;
            // this fresh GitHub response is the authority used immediately before PATCH.
            val liveCandidate = try {
                api.getRepository(candidate.ownerLogin, candidate.name).toCandidate()
            } catch (error: HttpException) {
                val message = safeHttpMessage(error.code(), "preflight")
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.FAILED,
                    message,
                    error.code()
                )
                if (error.code() == 401) abortReason = message
                if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                    provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                    provenanceMessage = "Receipt checkpoint failed after preflight error; remaining mutations aborted"
                    abortReason = provenanceMessage
                }
                return@forEachIndexed
            } catch (_: Exception) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.FAILED,
                    "GitHub preflight failed due to a network/runtime error"
                )
                if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                    provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                    provenanceMessage = "Receipt checkpoint failed after preflight error; remaining mutations aborted"
                    abortReason = provenanceMessage
                }
                return@forEachIndexed
            }

            if (liveCandidate.id != candidate.id) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.SKIPPED,
                    "Live preflight repository identity changed; mutation blocked fail-closed"
                )
                if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                    provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                    provenanceMessage = "Receipt checkpoint failed; remaining mutations aborted"
                    abortReason = provenanceMessage
                }
                return@forEachIndexed
            }

            if (!liveCandidate.eligible) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.SKIPPED,
                    "Live preflight blocked: ${liveCandidate.blockReason ?: "repository is no longer eligible"}"
                )
                if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                    provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                    provenanceMessage = "Receipt checkpoint failed; remaining mutations aborted"
                    abortReason = provenanceMessage
                }
                return@forEachIndexed
            }

            // The ATTEMPTING checkpoint is intentionally durable before the PATCH.
            // If the process dies after GitHub accepts the request, reconciliation can
            // identify the exact repository whose result was not yet confirmed locally.
            mutations[index] = candidate.mutation(
                PrivacyMutationStatus.ATTEMPTING,
                "Live preflight passed; visibility PATCH is the next external action"
            )
            if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.NOT_ATTEMPTED,
                    "Pre-mutation receipt checkpoint failed; PATCH was not sent"
                )
                provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                provenanceMessage = "Provenance persistence failed before PATCH; remaining mutations aborted"
                abortReason = provenanceMessage
                return@forEachIndexed
            }

            try {
                val updated = api.updateVisibility(
                    candidate.ownerLogin,
                    candidate.name,
                    VisibilityPatchRequest("private")
                )
                mutations[index] = if (
                    updated.id == candidate.id &&
                    (updated.isPrivate || updated.visibility == "private")
                ) {
                    candidate.mutation(
                        PrivacyMutationStatus.UPDATED,
                        "GitHub response confirmed private visibility"
                    )
                } else {
                    candidate.mutation(
                        PrivacyMutationStatus.FAILED,
                        "GitHub response did not confirm the expected repository as private"
                    )
                }
            } catch (error: HttpException) {
                val message = safeHttpMessage(error.code(), "mutation")
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.FAILED,
                    message,
                    error.code()
                )
                if (error.code() == 401) abortReason = message
            } catch (_: Exception) {
                mutations[index] = candidate.mutation(
                    PrivacyMutationStatus.FAILED,
                    "GitHub visibility mutation failed due to a network/runtime error"
                )
            }

            if (!persistCheckpoint(PrivacyReceiptPhase.MUTATING)) {
                provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
                provenanceMessage =
                    "Post-mutation receipt checkpoint failed; durable journal remains at ATTEMPTING and remaining mutations were aborted"
                abortReason = provenanceMessage
            }

            if (index != ordered.lastIndex && abortReason == null) delay(MUTATION_DELAY_MS)
        }

        // Persist explicit NOT_ATTEMPTED states for items skipped after an abort.
        if (abortReason != null) {
            ordered.indices.forEach { index ->
                if (mutations[index].status == PrivacyMutationStatus.NOT_ATTEMPTED &&
                    mutations[index].message == "Planned; no mutation attempted yet"
                ) {
                    mutations[index] = ordered[index].mutation(
                        PrivacyMutationStatus.NOT_ATTEMPTED,
                        abortReason!!
                    )
                }
            }
        }

        val finalPhase = if (abortReason == null) PrivacyReceiptPhase.COMPLETED else PrivacyReceiptPhase.ABORTED
        val finalSaved = persistCheckpoint(finalPhase)
        if (finalSaved && provenanceState == PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS) {
            provenanceState = PrivacyProvenanceState.DURABLE_RECOVERED
            provenanceMessage = "A transient checkpoint failure occurred; final append-only receipt recovered the complete in-memory outcome"
        } else if (!finalSaved && provenanceState == PrivacyProvenanceState.DURABLE) {
            provenanceState = PrivacyProvenanceState.PARTIAL_DURABILITY_LOSS
            provenanceMessage = "Final receipt checkpoint failed; use the latest durable checkpoint for reconciliation"
        }

        val finalReceipt = if (finalSaved) {
            lastPersistedReceipt!!
        } else {
            buildReceipt(finalPhase)
        }

        return RepositoryPrivacyBulkResult(
            receipt = finalReceipt,
            receiptPath = lastReceiptPath,
            provenanceState = provenanceState,
            provenanceMessage = provenanceMessage
        )
    }

    companion object {
        const val PAGE_SIZE = 100
        const val MUTATION_DELAY_MS = 250L
        const val MAX_INVENTORY_PAGES = 1_000
    }
}

private fun safeHttpMessage(code: Int, stage: String): String = when (code) {
    401 -> "Authentication rejected during $stage; remaining mutations aborted"
    403 -> "Forbidden during $stage: token permission, admin authority, SSO, rate limit, or organization policy"
    404 -> "Repository not found during $stage or token cannot access it"
    422 -> "GitHub rejected $stage; repository or organization policy may restrict the operation"
    429 -> "GitHub rate limit rejected $stage"
    else -> "GitHub HTTP $code rejected $stage"
}

internal fun PrivacyRepositoryDto.toCandidate(): RepositoryPrivacyCandidate {
    val reason = when {
        isPrivate || visibility == "private" -> "Already private"
        isFork -> "Fork visibility is controlled by its repository network"
        archived -> "Archived repository: mutation blocked fail-closed"
        disabled -> "Disabled repository: mutation blocked fail-closed"
        permissions?.admin != true -> "TOKEN_VAZIO: GitHub did not confirm admin permission"
        else -> null
    }
    return RepositoryPrivacyCandidate(
        id = id,
        ownerLogin = owner.login,
        ownerType = owner.type,
        name = name,
        fullName = fullName,
        currentVisibility = visibility ?: if (isPrivate) "private" else "public",
        isPrivate = isPrivate,
        isFork = isFork,
        archived = archived,
        disabled = disabled,
        hasPages = hasPages,
        stars = stargazersCount,
        watchers = watchersCount,
        forks = forksCount,
        admin = permissions?.admin == true,
        blockReason = reason
    )
}

private fun RepositoryPrivacyCandidate.mutation(
    status: PrivacyMutationStatus,
    message: String,
    httpStatus: Int? = null
) = RepositoryPrivacyMutation(
    repositoryId = id,
    fullName = fullName,
    ownerType = ownerType,
    fromVisibility = currentVisibility,
    targetVisibility = "private",
    status = status,
    httpStatus = httpStatus,
    message = message
)

data class RepositoryPrivacyCandidate(
    val id: Long,
    val ownerLogin: String,
    val ownerType: String,
    val name: String,
    val fullName: String,
    val currentVisibility: String,
    val isPrivate: Boolean,
    val isFork: Boolean,
    val archived: Boolean,
    val disabled: Boolean,
    val hasPages: Boolean,
    val stars: Int,
    val watchers: Int,
    val forks: Int,
    val admin: Boolean,
    val blockReason: String?
) {
    val eligible: Boolean get() = blockReason == null
    val organizationOwned: Boolean get() = ownerType.equals("Organization", ignoreCase = true)
    val hasPublicImpactWarning: Boolean get() = hasPages || stars > 0 || watchers > 0 || forks > 0
}

enum class PrivacyMutationStatus { UPDATED, FAILED, SKIPPED, NOT_ATTEMPTED, ATTEMPTING }

enum class PrivacyReceiptPhase { PLANNED, MUTATING, COMPLETED, ABORTED }

enum class PrivacyProvenanceState {
    DURABLE,
    DURABLE_RECOVERED,
    FAILED_BEFORE_MUTATION,
    PARTIAL_DURABILITY_LOSS
}

data class RepositoryPrivacyMutation(
    val repositoryId: Long,
    val fullName: String,
    val ownerType: String,
    val fromVisibility: String,
    val targetVisibility: String,
    val status: PrivacyMutationStatus,
    val httpStatus: Int?,
    val message: String
)

data class RepositoryPrivacyReceipt(
    val schema: String = "RAFGITTOOLS_REPOSITORY_PRIVACY_RECEIPT_V2",
    val operationId: String,
    val createdAtEpochMs: Long,
    val checkpointSequence: Int,
    val phase: PrivacyReceiptPhase,
    val requested: Int,
    val updated: Int,
    val failed: Int,
    val skipped: Int,
    val notAttempted: Int,
    val attempting: Int,
    val mutations: List<RepositoryPrivacyMutation>
)

data class RepositoryPrivacyBulkResult(
    val receipt: RepositoryPrivacyReceipt,
    val receiptPath: String?,
    val provenanceState: PrivacyProvenanceState,
    val provenanceMessage: String
)
