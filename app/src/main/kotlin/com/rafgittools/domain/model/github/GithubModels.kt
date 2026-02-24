package com.rafgittools.domain.model.github

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// ─── CAT-2 FIX: Todos os campos snake_case da API GitHub precisam @SerializedName ───
// Sem isso o GSON não deserializa e TODOS os campos ficam null silenciosamente.

@Parcelize
data class GithubRepository(
    val id: Long,
    val name: String,
    @SerializedName("full_name")         val fullName: String,
    val owner: GithubUser,
    val description: String?,
    @SerializedName("html_url")          val htmlUrl: String,
    @SerializedName("clone_url")         val cloneUrl: String,
    @SerializedName("ssh_url")           val sshUrl: String,
    val language: String?,
    @SerializedName("stargazers_count")  val stargazersCount: Int,
    @SerializedName("forks_count")       val forksCount: Int,
    @SerializedName("watchers_count")    val watchersCount: Int,
    @SerializedName("open_issues_count") val openIssuesCount: Int,
    @SerializedName("private")           val isPrivate: Boolean,
    @SerializedName("fork")              val isFork: Boolean,
    @SerializedName("default_branch")    val defaultBranch: String,
    @SerializedName("created_at")        val createdAt: String,
    @SerializedName("updated_at")        val updatedAt: String,
    @SerializedName("pushed_at")         val pushedAt: String?
) : Parcelable

@Parcelize
data class GithubUser(
    val id: Long,
    val login: String,
    @SerializedName("avatar_url")   val avatarUrl: String,
    @SerializedName("html_url")     val htmlUrl: String,
    val type: String,
    val name: String?     = null,
    val email: String?    = null,
    val bio: String?      = null,
    val location: String? = null,
    val company: String?  = null,
    @SerializedName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
) : Parcelable

@Parcelize
data class GithubSearchUser(
    val id: Long,
    val login: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("html_url") val htmlUrl: String?
) : Parcelable

@Parcelize
data class GithubSearchRepository(
    @SerializedName("full_name") val fullName: String,
    val name: String,
    val owner: GithubSearchUser
) : Parcelable

@Parcelize
data class GithubSearchIssue(
    val id: Long,
    val number: Int,
    val title: String,
    @SerializedName("repository_url") val repositoryUrl: String,
    @SerializedName("html_url") val htmlUrl: String?
) : Parcelable

@Parcelize
data class GithubSearchCode(
    val name: String,
    val path: String,
    @SerializedName("html_url") val htmlUrl: String?,
    val repository: GithubSearchRepository
) : Parcelable

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
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("closed_at")  val closedAt: String?,
    @SerializedName("html_url")   val htmlUrl: String,
    @SerializedName("comments")   val commentsCount: Int
) : Parcelable

@Parcelize
data class GithubLabel(
    val id: Long,
    val name: String,
    val color: String,
    val description: String?
) : Parcelable

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
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("closed_at")  val closedAt: String?,
    @SerializedName("merged_at")  val mergedAt: String?,
    @SerializedName("html_url")   val htmlUrl: String,
    val draft: Boolean
) : Parcelable

@Parcelize
data class GithubBranch(
    val label: String,
    val ref: String,
    val sha: String,
    val user: GithubUser,
    val repo: GithubRepository
) : Parcelable

@Parcelize
data class GithubBranchInfo(
    val name: String,
    val commit: GithubCommitRef,
    val protected: Boolean
) : Parcelable

@Parcelize
data class GithubCommitRef(
    val sha: String,
    val url: String
) : Parcelable

@Parcelize
data class GithubComment(
    val id: Long,
    val body: String,
    val user: GithubUser,
    @SerializedName("created_at")         val createdAt: String,
    @SerializedName("updated_at")         val updatedAt: String,
    @SerializedName("html_url")           val htmlUrl: String,
    @SerializedName("author_association") val authorAssociation: String?
) : Parcelable

@Parcelize
data class GithubReview(
    val id: Long,
    val user: GithubUser,
    val body: String?,
    val state: String,                        // APPROVED, CHANGES_REQUESTED, COMMENTED, PENDING, DISMISSED
    @SerializedName("html_url")      val htmlUrl: String,
    @SerializedName("submitted_at")  val submittedAt: String?
) : Parcelable

@Parcelize
data class GithubReviewComment(
    val id: Long,
    val body: String,
    val path: String,
    val position: Int?,
    val line: Int?,
    val side: String?,
    val user: GithubUser,
    @SerializedName("created_at")         val createdAt: String,
    @SerializedName("updated_at")         val updatedAt: String,
    @SerializedName("html_url")           val htmlUrl: String,
    @SerializedName("diff_hunk")          val diffHunk: String?
) : Parcelable

@Parcelize
data class GithubPullRequestFile(
    val sha: String,
    val filename: String,
    val status: String,              // added, removed, modified, renamed, copied, changed, unchanged
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val patch: String?,
    @SerializedName("previous_filename") val previousFilename: String?
) : Parcelable

@Parcelize
data class GithubCommit(
    val sha: String,
    val commit: GithubCommitData,
    val author: GithubUser?,
    val committer: GithubUser?,
    @SerializedName("html_url") val htmlUrl: String
) : Parcelable

@Parcelize
data class GithubCommitData(
    val message: String,
    val author: GithubCommitAuthor,
    val committer: GithubCommitAuthor,
    val tree: GithubCommitRef?
) : Parcelable

@Parcelize
data class GithubCommitAuthor(
    val name: String,
    val email: String,
    val date: String
) : Parcelable

@Parcelize
data class GithubCommitDetail(
    val sha: String,
    val commit: GithubCommitData,
    val author: GithubUser?,
    val committer: GithubUser?,
    @SerializedName("html_url") val htmlUrl: String,
    val files: List<GithubPullRequestFile>?,
    val stats: GithubCommitStats?
) : Parcelable

@Parcelize
data class GithubCommitStats(
    val additions: Int,
    val deletions: Int,
    val total: Int
) : Parcelable

@Parcelize
data class GithubRelease(
    val id: Long,
    @SerializedName("tag_name")        val tagName: String,
    @SerializedName("target_commitish") val targetCommitish: String,
    val name: String?,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerializedName("created_at")      val createdAt: String,
    @SerializedName("published_at")    val publishedAt: String?,
    @SerializedName("html_url")        val htmlUrl: String,
    val author: GithubUser,
    val assets: List<GithubReleaseAsset>
) : Parcelable

@Parcelize
data class GithubReleaseAsset(
    val id: Long,
    val name: String,
    val label: String?,
    @SerializedName("content_type")       val contentType: String,
    val size: Long,
    @SerializedName("download_count")     val downloadCount: Int,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("created_at")         val createdAt: String,
    @SerializedName("updated_at")         val updatedAt: String
) : Parcelable

@Parcelize
data class GithubContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val type: String,                        // file, dir, symlink, submodule
    val content: String?,
    val encoding: String?,
    @SerializedName("html_url")     val htmlUrl: String,
    @SerializedName("download_url") val downloadUrl: String?
) : Parcelable

@Parcelize
data class GithubReaction(
    val id: Long,
    val user: GithubUser,
    val content: String,                     // +1, -1, laugh, confused, heart, hooray, rocket, eyes
    @SerializedName("created_at") val createdAt: String
) : Parcelable

@Parcelize
data class GithubNotification(
    val id: String,
    val unread: Boolean,
    val reason: String,
    @SerializedName("updated_at")   val updatedAt: String,
    @SerializedName("last_read_at") val lastReadAt: String?,
    val subject: GithubNotificationSubject,
    val repository: GithubRepository
) : Parcelable

@Parcelize
data class GithubNotificationSubject(
    val title: String,
    val url: String?,
    @SerializedName("latest_comment_url") val latestCommentUrl: String?,
    val type: String                         // Issue, PullRequest, Commit, Release, etc.
) : Parcelable

@Parcelize
data class GithubSubscription(
    val subscribed: Boolean,
    val ignored: Boolean,
    val reason: String?,
    @SerializedName("created_at") val createdAt: String
) : Parcelable

@Parcelize
data class GithubMilestone(
    val id: Long,
    val number: Int,
    val title: String,
    val description: String?,
    val state: String,
    @SerializedName("open_issues")   val openIssues: Int,
    @SerializedName("closed_issues") val closedIssues: Int,
    @SerializedName("created_at")    val createdAt: String,
    @SerializedName("updated_at")    val updatedAt: String,
    @SerializedName("due_on")        val dueOn: String?,
    @SerializedName("closed_at")     val closedAt: String?
) : Parcelable
