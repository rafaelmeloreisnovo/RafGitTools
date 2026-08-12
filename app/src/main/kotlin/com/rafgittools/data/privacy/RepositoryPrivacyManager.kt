package com.rafgittools.data.privacy

import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fail-closed bulk repository visibility manager.
 *
 * It never changes forks, archived/disabled repositories, already-private repositories,
 * or repositories where GitHub did not explicitly report admin permission.
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
        val mutations = mutableListOf<RepositoryPrivacyMutation>()
        var abortReason: String? = null

        ordered.forEachIndexed { index, candidate ->
            if (candidate.blockReason != null) {
                mutations += candidate.mutation(
                    PrivacyMutationStatus.SKIPPED,
                    candidate.blockReason
                )
                return@forEachIndexed
            }
            if (abortReason != null) {
                mutations += candidate.mutation(
                    PrivacyMutationStatus.NOT_ATTEMPTED,
                    abortReason
                )
                return@forEachIndexed
            }

            try {
                val updated = api.updateVisibility(
                    candidate.ownerLogin,
                    candidate.name,
                    VisibilityPatchRequest("private")
                )
                if (updated.isPrivate || updated.visibility == "private") {
                    mutations += candidate.mutation(
                        PrivacyMutationStatus.UPDATED,
                        "Visibility confirmed private"
                    )
                } else {
                    mutations += candidate.mutation(
                        PrivacyMutationStatus.FAILED,
                        "GitHub response did not confirm private visibility"
                    )
                }
            } catch (e: HttpException) {
                val safeMessage = when (e.code()) {
                    401 -> "Authentication rejected; remaining mutations aborted"
                    403 -> "Forbidden: token permission, admin authority, SSO, or organization policy"
                    404 -> "Repository not found or token cannot access it"
                    422 -> "Visibility change rejected; organization policy may restrict this operation"
                    else -> "GitHub HTTP ${e.code()} rejected visibility change"
                }
                mutations += candidate.mutation(
                    PrivacyMutationStatus.FAILED,
                    safeMessage,
                    e.code()
                )
                if (e.code() == 401) abortReason = safeMessage
            } catch (e: Exception) {
                mutations += candidate.mutation(
                    PrivacyMutationStatus.FAILED,
                    e.message?.take(MAX_MESSAGE) ?: "Unexpected network/runtime error"
                )
            }

            if (index != ordered.lastIndex && abortReason == null) delay(MUTATION_DELAY_MS)
        }

        val receipt = RepositoryPrivacyReceipt(
            createdAtEpochMs = System.currentTimeMillis(),
            requested = ordered.size,
            updated = mutations.count { it.status == PrivacyMutationStatus.UPDATED },
            failed = mutations.count { it.status == PrivacyMutationStatus.FAILED },
            skipped = mutations.count { it.status == PrivacyMutationStatus.SKIPPED },
            notAttempted = mutations.count { it.status == PrivacyMutationStatus.NOT_ATTEMPTED },
            mutations = mutations
        )
        return RepositoryPrivacyBulkResult(
            receipt = receipt,
            receiptPath = receiptStore.save(receipt).getOrNull()
        )
    }

    companion object {
        const val PAGE_SIZE = 100
        const val MUTATION_DELAY_MS = 250L
        private const val MAX_MESSAGE = 240
    }
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

enum class PrivacyMutationStatus { UPDATED, FAILED, SKIPPED, NOT_ATTEMPTED }

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
    val schema: String = "RAFGITTOOLS_REPOSITORY_PRIVACY_RECEIPT_V1",
    val createdAtEpochMs: Long,
    val requested: Int,
    val updated: Int,
    val failed: Int,
    val skipped: Int,
    val notAttempted: Int,
    val mutations: List<RepositoryPrivacyMutation>
)

data class RepositoryPrivacyBulkResult(
    val receipt: RepositoryPrivacyReceipt,
    val receiptPath: String?
)
