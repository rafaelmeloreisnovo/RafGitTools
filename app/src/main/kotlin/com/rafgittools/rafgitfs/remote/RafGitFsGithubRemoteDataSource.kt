package com.rafgittools.rafgitfs.remote

import com.rafgittools.data.github.GithubCodeSearchItem
import com.rafgittools.domain.model.github.GithubBranchInfo
import com.rafgittools.domain.model.github.GithubRepository
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsGithubRemoteDataSource @Inject constructor(
    private val api: RafGitFsGithubApiService
) {
    suspend fun listRepositories(
        perPage: Int = RafGitFsPagination.DEFAULT_PAGE_SIZE,
        maxPages: Int = RafGitFsPagination.DEFAULT_MAX_PAGES
    ): RafGitFsRemoteResult<List<GithubRepository>> = collectPages(perPage, maxPages) { page, size ->
        api.listRepositories(page = page, perPage = size)
    }

    suspend fun listBranches(
        repositoryFullName: String,
        perPage: Int = RafGitFsPagination.DEFAULT_PAGE_SIZE,
        maxPages: Int = RafGitFsPagination.DEFAULT_MAX_PAGES
    ): RafGitFsRemoteResult<List<GithubBranchInfo>> {
        val target = splitRepository(repositoryFullName)
            ?: return invalidRepository(repositoryFullName)
        return collectPages(perPage, maxPages) { page, size ->
            api.listBranches(target.first, target.second, page, size)
        }
    }

    suspend fun listTags(
        repositoryFullName: String,
        perPage: Int = RafGitFsPagination.DEFAULT_PAGE_SIZE,
        maxPages: Int = RafGitFsPagination.DEFAULT_MAX_PAGES
    ): RafGitFsRemoteResult<List<RafGitFsTagDto>> {
        val target = splitRepository(repositoryFullName)
            ?: return invalidRepository(repositoryFullName)
        return collectPages(perPage, maxPages) { page, size ->
            api.listTags(target.first, target.second, page, size)
        }
    }

    suspend fun resolveCommit(
        repositoryFullName: String,
        ref: String
    ): RafGitFsRemoteResult<RafGitFsCommitDto> {
        val target = splitRepository(repositoryFullName)
            ?: return invalidRepository(repositoryFullName)
        if (ref.isBlank()) return tokenVazio("REF_EMPTY")
        return requestSingle { api.resolveCommit(target.first, target.second, ref) }
    }

    suspend fun getTree(
        repositoryFullName: String,
        treeSha: String
    ): RafGitFsRemoteResult<RafGitFsTreeDto> {
        val target = splitRepository(repositoryFullName)
            ?: return invalidRepository(repositoryFullName)
        if (!isGitSha(treeSha)) return tokenVazio("TREE_SHA_INVALID")
        return when (val result = requestSingle { api.getTree(target.first, target.second, treeSha) }) {
            is RafGitFsRemoteResult.Observed -> {
                if (result.value.truncated) {
                    RafGitFsRemoteResult.TokenVazio(
                        reason = "GITHUB_TREE_TRUNCATED",
                        partialValue = result.value,
                        metadata = result.metadata.copy(complete = false)
                    )
                } else result
            }
            else -> result
        }
    }

    suspend fun getBlob(
        repositoryFullName: String,
        blobSha: String
    ): RafGitFsRemoteResult<RafGitFsBlobDto> {
        val target = splitRepository(repositoryFullName)
            ?: return invalidRepository(repositoryFullName)
        if (!isGitSha(blobSha)) return tokenVazio("BLOB_SHA_INVALID")
        return when (val result = requestSingle { api.getBlob(target.first, target.second, blobSha) }) {
            is RafGitFsRemoteResult.Observed -> {
                val blob = result.value
                if (blob.encoding != "base64" || blob.content.isNullOrBlank()) {
                    RafGitFsRemoteResult.TokenVazio(
                        reason = "BLOB_CONTENT_OR_ENCODING_MISSING",
                        partialValue = blob,
                        metadata = result.metadata.copy(complete = false)
                    )
                } else result
            }
            else -> result
        }
    }

    suspend fun searchCode(
        repositoryFullName: String,
        query: String,
        perPage: Int = RafGitFsPagination.DEFAULT_PAGE_SIZE,
        maxPages: Int = 10
    ): RafGitFsRemoteResult<List<GithubCodeSearchItem>> {
        if (splitRepository(repositoryFullName) == null) return invalidRepository(repositoryFullName)
        if (query.isBlank()) return tokenVazio("SEARCH_QUERY_EMPTY")
        RafGitFsPagination.validateBounds(perPage, maxPages)

        val items = mutableListOf<GithubCodeSearchItem>()
        var page = 1
        var pagesFetched = 0
        var metadata = RafGitFsRemoteMetadata(pagesFetched = 0)
        val githubQuery = "${query.trim()} repo:$repositoryFullName"

        while (pagesFetched < maxPages) {
            val response = try {
                api.searchCode(githubQuery, page, perPage)
            } catch (error: IOException) {
                return RafGitFsRemoteResult.Failure(null, error.message ?: "network error", true)
            } catch (error: Exception) {
                return RafGitFsRemoteResult.Failure(null, error.message ?: "unexpected error", false)
            }

            pagesFetched += 1
            metadata = metadataFrom(response, pagesFetched)
            if (!response.isSuccessful) return errorResult(response)
            val body = response.body() ?: return RafGitFsRemoteResult.TokenVazio(
                "SEARCH_BODY_MISSING",
                items,
                metadata.copy(complete = false)
            )
            items += body.items
            if (body.incomplete_results) {
                return RafGitFsRemoteResult.TokenVazio(
                    "GITHUB_SEARCH_INCOMPLETE",
                    items.distinctBy { "${it.repository.fullName}:${it.path}" },
                    metadata.copy(complete = false)
                )
            }
            val next = RafGitFsPagination.nextPage(response.headers()["Link"])
            if (next == null) {
                return RafGitFsRemoteResult.Observed(
                    items.distinctBy { "${it.repository.fullName}:${it.path}" },
                    metadata.copy(complete = true)
                )
            }
            page = next
        }

        return RafGitFsRemoteResult.TokenVazio(
            "SEARCH_PAGE_BUDGET_EXHAUSTED",
            items.distinctBy { "${it.repository.fullName}:${it.path}" },
            metadata.copy(complete = false)
        )
    }

    private suspend fun <T> collectPages(
        perPage: Int,
        maxPages: Int,
        fetch: suspend (page: Int, perPage: Int) -> Response<List<T>>
    ): RafGitFsRemoteResult<List<T>> {
        RafGitFsPagination.validateBounds(perPage, maxPages)
        val accumulated = mutableListOf<T>()
        var page = 1
        var pagesFetched = 0
        var metadata = RafGitFsRemoteMetadata(pagesFetched = 0)

        while (pagesFetched < maxPages) {
            val response = try {
                fetch(page, perPage)
            } catch (error: IOException) {
                return RafGitFsRemoteResult.Failure(null, error.message ?: "network error", true)
            } catch (error: Exception) {
                return RafGitFsRemoteResult.Failure(null, error.message ?: "unexpected error", false)
            }

            pagesFetched += 1
            metadata = metadataFrom(response, pagesFetched)
            if (!response.isSuccessful) return errorResult(response)
            val body = response.body() ?: return RafGitFsRemoteResult.TokenVazio(
                "RESPONSE_BODY_MISSING",
                accumulated,
                metadata.copy(complete = false)
            )
            accumulated += body

            val next = RafGitFsPagination.nextPage(response.headers()["Link"])
            if (next == null) {
                return RafGitFsRemoteResult.Observed(accumulated, metadata.copy(complete = true))
            }
            page = next
        }

        return RafGitFsRemoteResult.TokenVazio(
            "PAGE_BUDGET_EXHAUSTED",
            accumulated,
            metadata.copy(complete = false)
        )
    }

    private suspend fun <T> requestSingle(
        request: suspend () -> Response<T>
    ): RafGitFsRemoteResult<T> {
        val response = try {
            request()
        } catch (error: IOException) {
            return RafGitFsRemoteResult.Failure(null, error.message ?: "network error", true)
        } catch (error: Exception) {
            return RafGitFsRemoteResult.Failure(null, error.message ?: "unexpected error", false)
        }
        val metadata = metadataFrom(response, 1)
        if (!response.isSuccessful) return errorResult(response)
        val body = response.body() ?: return RafGitFsRemoteResult.TokenVazio(
            "RESPONSE_BODY_MISSING",
            null,
            metadata.copy(complete = false)
        )
        return RafGitFsRemoteResult.Observed(body, metadata)
    }

    private fun metadataFrom(response: Response<*>, pagesFetched: Int): RafGitFsRemoteMetadata =
        RafGitFsRemoteMetadata(
            pagesFetched = pagesFetched,
            requestId = response.headers()["X-GitHub-Request-Id"],
            etag = response.headers()["ETag"],
            rateLimit = RafGitFsRateLimitSnapshot.from(response.headers()),
            complete = true
        )

    private fun errorResult(response: Response<*>): RafGitFsRemoteResult<Nothing> {
        val rate = RafGitFsRateLimitSnapshot.from(response.headers())
        if (response.code() == 429 || (response.code() == 403 && rate.exhausted)) {
            return RafGitFsRemoteResult.RateLimited(
                resetAtEpochSeconds = rate.resetAtEpochSeconds,
                retryAfterSeconds = rate.retryAfterSeconds,
                resource = rate.resource,
                message = "GitHub rate limit reached"
            )
        }
        if (response.code() == 304) {
            return RafGitFsRemoteResult.NotModified(metadataFrom(response, 1))
        }
        val message = runCatching { response.errorBody()?.string() }
            .getOrNull()
            ?.take(500)
            ?.ifBlank { null }
            ?: response.message().ifBlank { "GitHub request failed" }
        return RafGitFsRemoteResult.Failure(
            statusCode = response.code(),
            message = message,
            retryable = response.code() == 408 || response.code() == 429 || response.code() >= 500
        )
    }

    companion object {
        fun splitRepository(fullName: String): Pair<String, String>? {
            val parts = fullName.trim().split('/')
            return if (parts.size == 2 && parts.all { it.isNotBlank() }) parts[0] to parts[1] else null
        }

        fun isGitSha(value: String): Boolean = value.matches(Regex("^[0-9a-fA-F]{7,64}$"))

        private fun <T> invalidRepository(value: String): RafGitFsRemoteResult<T> =
            tokenVazio("REPOSITORY_FULL_NAME_INVALID:$value")

        private fun <T> tokenVazio(reason: String): RafGitFsRemoteResult<T> =
            RafGitFsRemoteResult.TokenVazio(
                reason = reason,
                partialValue = null,
                metadata = RafGitFsRemoteMetadata(complete = false)
            )
    }
}
