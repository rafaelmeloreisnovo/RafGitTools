package com.rafgittools.domain.model.github

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * GitHub repository model
 */
@Parcelize
data class GithubRepository(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: GithubUser,
    val description: String?,
    val htmlUrl: String,
    val cloneUrl: String,
    val sshUrl: String,
    val language: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val watchersCount: Int,
    val openIssuesCount: Int,
    val isPrivate: Boolean,
    val isFork: Boolean,
    val defaultBranch: String,
    val createdAt: String,
    val updatedAt: String,
    val pushedAt: String?
) : Parcelable

/**
 * GitHub user model
 */
@Parcelize
data class GithubUser(
    val id: Long,
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val type: String,
    val name: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val company: String? = null,
    val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
) : Parcelable

/**
 * GitHub issue model
 */
@Parcelize
data class GithubIssue(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val user: GithubUser,
    val labels: List<GithubLabel>,
    val assignees: List<GithubUser>,
    val createdAt: String,
    val updatedAt: String,
    val closedAt: String?,
    val htmlUrl: String,
    val commentsCount: Int
) : Parcelable

/**
 * GitHub label model
 */
@Parcelize
data class GithubLabel(
    val id: Long,
    val name: String,
    val color: String,
    val description: String?
) : Parcelable

/**
 * GitHub pull request model
 */
@Parcelize
data class GithubPullRequest(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val user: GithubUser,
    val head: GithubBranch,
    val base: GithubBranch,
    val createdAt: String,
    val updatedAt: String,
    val closedAt: String?,
    val mergedAt: String?,
    val htmlUrl: String,
    val draft: Boolean
) : Parcelable

/**
 * GitHub branch info for PR head/base
 */
@Parcelize
data class GithubBranch(
    val label: String,
    val ref: String,
    val sha: String,
    val user: GithubUser,
    val repo: GithubRepository
) : Parcelable

/**
 * GitHub branch info
 */
@Parcelize
data class GithubBranchInfo(
    val name: String,
    val commit: GithubCommitRef,
    val protected: Boolean
) : Parcelable

/**
 * GitHub commit reference (minimal)
 */
@Parcelize
data class GithubCommitRef(
    val sha: String,
    val url: String
) : Parcelable

/**
 * GitHub comment model
 */
@Parcelize
data class GithubComment(
    val id: Long,
    val body: String,
    val user: GithubUser,
    val createdAt: String,
    val updatedAt: String,
    val htmlUrl: String,
    val authorAssociation: String?
) : Parcelable

/**
 * GitHub review model
 */
@Parcelize
data class GithubReview(
    val id: Long,
    val user: GithubUser,
    val body: String?,
    val state: String, // APPROVED, CHANGES_REQUESTED, COMMENTED, PENDING, DISMISSED
    val htmlUrl: String,
    val submittedAt: String?
) : Parcelable

/**
 * GitHub review comment model
 */
@Parcelize
data class GithubReviewComment(
    val id: Long,
    val body: String,
    val path: String,
    val position: Int?,
    val line: Int?,
    val side: String?,
    val user: GithubUser,
    val createdAt: String,
    val updatedAt: String,
    val htmlUrl: String,
    val diffHunk: String?
) : Parcelable

/**
 * GitHub pull request file
 */
@Parcelize
data class GithubPullRequestFile(
    val sha: String,
    val filename: String,
    val status: String, // added, removed, modified, renamed, copied, changed, unchanged
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val patch: String?,
    val previousFilename: String?
) : Parcelable

/**
 * GitHub commit model
 */
@Parcelize
data class GithubCommit(
    val sha: String,
    val commit: GithubCommitData,
    val author: GithubUser?,
    val committer: GithubUser?,
    val htmlUrl: String
) : Parcelable

/**
 * GitHub commit data (inner commit info)
 */
@Parcelize
data class GithubCommitData(
    val message: String,
    val author: GithubCommitAuthor,
    val committer: GithubCommitAuthor,
    val tree: GithubCommitRef?
) : Parcelable

/**
 * GitHub commit author
 */
@Parcelize
data class GithubCommitAuthor(
    val name: String,
    val email: String,
    val date: String
) : Parcelable

/**
 * GitHub commit detail (full commit info)
 */
@Parcelize
data class GithubCommitDetail(
    val sha: String,
    val commit: GithubCommitData,
    val author: GithubUser?,
    val committer: GithubUser?,
    val htmlUrl: String,
    val files: List<GithubPullRequestFile>?,
    val stats: GithubCommitStats?
) : Parcelable

/**
 * GitHub commit stats
 */
@Parcelize
data class GithubCommitStats(
    val additions: Int,
    val deletions: Int,
    val total: Int
) : Parcelable

/**
 * GitHub release model
 */
@Parcelize
data class GithubRelease(
    val id: Long,
    val tagName: String,
    val targetCommitish: String,
    val name: String?,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val createdAt: String,
    val publishedAt: String?,
    val htmlUrl: String,
    val author: GithubUser,
    val assets: List<GithubReleaseAsset>
) : Parcelable

/**
 * GitHub release asset
 */
@Parcelize
data class GithubReleaseAsset(
    val id: Long,
    val name: String,
    val label: String?,
    val contentType: String,
    val size: Long,
    val downloadCount: Int,
    val browserDownloadUrl: String,
    val createdAt: String,
    val updatedAt: String
) : Parcelable

/**
 * GitHub content model (file/directory)
 */
@Parcelize
data class GithubContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val type: String, // file, dir, symlink, submodule
    val content: String?,
    val encoding: String?,
    val htmlUrl: String,
    val downloadUrl: String?
) : Parcelable

/**
 * GitHub reaction model
 */
@Parcelize
data class GithubReaction(
    val id: Long,
    val user: GithubUser,
    val content: String, // +1, -1, laugh, confused, heart, hooray, rocket, eyes
    val createdAt: String
) : Parcelable

/**
 * GitHub notification model
 */
@Parcelize
data class GithubNotification(
    val id: String,
    val unread: Boolean,
    val reason: String,
    val updatedAt: String,
    val lastReadAt: String?,
    val subject: GithubNotificationSubject,
    val repository: GithubRepository
) : Parcelable

/**
 * GitHub notification subject
 */
@Parcelize
data class GithubNotificationSubject(
    val title: String,
    val url: String?,
    val latestCommentUrl: String?,
    val type: String // Issue, PullRequest, Commit, Release, etc.
) : Parcelable

/**
 * GitHub subscription model
 */
@Parcelize
data class GithubSubscription(
    val subscribed: Boolean,
    val ignored: Boolean,
    val reason: String?,
    val createdAt: String
) : Parcelable

/**
 * GitHub milestone model
 */
@Parcelize
data class GithubMilestone(
    val id: Long,
    val number: Int,
    val title: String,
    val description: String?,
    val state: String,
    val openIssues: Int,
    val closedIssues: Int,
    val createdAt: String,
    val updatedAt: String,
    val dueOn: String?,
    val closedAt: String?
) : Parcelable
