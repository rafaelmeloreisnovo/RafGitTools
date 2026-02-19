package com.rafgittools.ui.navigation

import java.net.URLEncoder

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Auth : Screen("auth")
    object RepositoryList : Screen("repository_list")
    object RepositoryDetail : Screen("repository_detail/{repoPath}") {
        fun createRoute(repoPath: String) = "repository_detail/${encodeArg(repoPath)}"
    }
    object CommitList : Screen("commit_list/{repoPath}") {
        fun createRoute(repoPath: String) = "commit_list/${encodeArg(repoPath)}"
    }
    object CommitDetail : Screen("commit_detail/{repoPath}/{commitSha}") {
        fun createRoute(repoPath: String, commitSha: String) =
            "commit_detail/${encodeArg(repoPath)}/${encodeArg(commitSha)}"
    }
    object BranchList : Screen("branch_list/{repoPath}") {
        fun createRoute(repoPath: String) = "branch_list/${encodeArg(repoPath)}"
    }
    object Settings : Screen("settings")
    object AddRepository : Screen("add_repository")
    object IssueList : Screen("issue_list/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "issue_list/${encodeArg(owner)}/${encodeArg(repo)}"
    }
    object IssueDetail : Screen("issue_detail/{owner}/{repo}/{number}") {
        fun createRoute(owner: String, repo: String, number: Int) = "issue_detail/${encodeArg(owner)}/${encodeArg(repo)}/$number"
    }
    object PullRequestList : Screen("pr_list/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "pr_list/${encodeArg(owner)}/${encodeArg(repo)}"
    }
    object PullRequestDetail : Screen("pr_detail/{owner}/{repo}/{number}") {
        fun createRoute(owner: String, repo: String, number: Int) = "pr_detail/${encodeArg(owner)}/${encodeArg(repo)}/$number"
    }
    object Search : Screen("search")
    object Profile : Screen("profile/{username}") {
        fun createRoute(username: String) = "profile/${encodeArg(username)}"
    }
    object FileBrowser : Screen("file_browser/{repoPath}") {
        fun createRoute(repoPath: String) = "file_browser/${encodeArg(repoPath)}"
    }
    object DiffViewer : Screen("diff_viewer/{repoPath}") {
        fun createRoute(repoPath: String) = "diff_viewer/${encodeArg(repoPath)}"
    }
    object StashList : Screen("stash_list/{repoPath}") {
        fun createRoute(repoPath: String) = "stash_list/${encodeArg(repoPath)}"
    }
    object TagList : Screen("tag_list/{repoPath}") {
        fun createRoute(repoPath: String) = "tag_list/${encodeArg(repoPath)}"
    }
    object Releases : Screen("releases/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "releases/${encodeArg(owner)}/${encodeArg(repo)}"
    }
    object ReleaseDetail : Screen("release_detail/{owner}/{repo}/{id}") {
        fun createRoute(owner: String, repo: String, id: Long) = "release_detail/${encodeArg(owner)}/${encodeArg(repo)}/$id"
    }
    object Notifications : Screen("notifications")
    object Terminal : Screen("terminal/{repoPath}") {
        fun createRoute(repoPath: String) = "terminal/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object CreateIssue : Screen("create_issue/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "create_issue/${encodeArg(owner)}/${encodeArg(repo)}"
    }
    object CreatePullRequest : Screen("create_pr/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "create_pr/${encodeArg(owner)}/${encodeArg(repo)}"
    }
}

private fun encodeArg(value: String): String = URLEncoder.encode(value, "UTF-8")
