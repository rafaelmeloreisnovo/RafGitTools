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
    val company: String? = null
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
 * GitHub branch info
 */
@Parcelize
data class GithubBranch(
    val label: String,
    val ref: String,
    val sha: String,
    val user: GithubUser,
    val repo: GithubRepository
) : Parcelable
