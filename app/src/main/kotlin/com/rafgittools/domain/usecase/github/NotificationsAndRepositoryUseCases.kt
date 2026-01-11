package com.rafgittools.domain.usecase.github

import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.*
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting notifications
 * Features #135-144 from roadmap (Phase 2)
 */
class GetNotificationsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetNotificationsParams, Result<List<GithubNotification>>> {
    
    override suspend fun invoke(params: GetNotificationsParams): Result<List<GithubNotification>> {
        return try {
            val notifications = githubApiService.getNotifications(
                all = params.all,
                participating = params.participating,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetNotificationsParams(
    val all: Boolean = false,
    val participating: Boolean = false,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for marking a notification as read
 * Feature #144 from roadmap
 */
class MarkNotificationReadUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<MarkNotificationReadParams, Result<Unit>> {
    
    override suspend fun invoke(params: MarkNotificationReadParams): Result<Unit> {
        return try {
            githubApiService.markNotificationRead(params.threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class MarkNotificationReadParams(
    val threadId: String
)

/**
 * Use case for getting repository releases
 * From roadmap
 */
class GetReleasesUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetReleasesParams, Result<List<GithubRelease>>> {
    
    override suspend fun invoke(params: GetReleasesParams): Result<List<GithubRelease>> {
        return try {
            val releases = githubApiService.getReleases(
                owner = params.owner,
                repo = params.repo,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(releases)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetReleasesParams(
    val owner: String,
    val repo: String,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting repository labels
 * Feature #97 from roadmap
 */
class GetLabelsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetLabelsParams, Result<List<GithubLabel>>> {
    
    override suspend fun invoke(params: GetLabelsParams): Result<List<GithubLabel>> {
        return try {
            val labels = githubApiService.getLabels(
                owner = params.owner,
                repo = params.repo,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(labels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetLabelsParams(
    val owner: String,
    val repo: String,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting repository milestones
 * Feature #98 from roadmap
 */
class GetMilestonesUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetMilestonesParams, Result<List<GithubMilestone>>> {
    
    override suspend fun invoke(params: GetMilestonesParams): Result<List<GithubMilestone>> {
        return try {
            val milestones = githubApiService.getMilestones(
                owner = params.owner,
                repo = params.repo,
                state = params.state,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(milestones)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetMilestonesParams(
    val owner: String,
    val repo: String,
    val state: String = "open",
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting repository assignees
 * Feature #99 from roadmap
 */
class GetAssigneesUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetAssigneesParams, Result<List<GithubUser>>> {
    
    override suspend fun invoke(params: GetAssigneesParams): Result<List<GithubUser>> {
        return try {
            val assignees = githubApiService.getAssignees(
                owner = params.owner,
                repo = params.repo,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(assignees)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetAssigneesParams(
    val owner: String,
    val repo: String,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting repository branches
 * From roadmap
 */
class GetBranchesUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetBranchesParams, Result<List<GithubBranchInfo>>> {
    
    override suspend fun invoke(params: GetBranchesParams): Result<List<GithubBranchInfo>> {
        return try {
            val branches = githubApiService.getBranches(
                owner = params.owner,
                repo = params.repo,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(branches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetBranchesParams(
    val owner: String,
    val repo: String,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting repository commits
 * From roadmap
 */
class GetCommitsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetCommitsParams, Result<List<GithubCommit>>> {
    
    override suspend fun invoke(params: GetCommitsParams): Result<List<GithubCommit>> {
        return try {
            val commits = githubApiService.getCommits(
                owner = params.owner,
                repo = params.repo,
                sha = params.sha,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(commits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetCommitsParams(
    val owner: String,
    val repo: String,
    val sha: String? = null,
    val page: Int = 1,
    val perPage: Int = 30
)
