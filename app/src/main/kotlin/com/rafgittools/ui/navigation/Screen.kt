package com.rafgittools.ui.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Auth : Screen("auth")
    object RepositoryList : Screen("repository_list")
    object RepositoryDetail : Screen("repository_detail/{repoPath}") {
        fun createRoute(repoPath: String) = "repository_detail/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object CommitList : Screen("commit_list/{repoPath}") {
        fun createRoute(repoPath: String) = "commit_list/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object BranchList : Screen("branch_list/{repoPath}") {
        fun createRoute(repoPath: String) = "branch_list/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object Settings : Screen("settings")
    object IssueList : Screen("issue_list/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "issue_list/$owner/$repo"
    }
    object IssueDetail : Screen("issue_detail/{owner}/{repo}/{number}") {
        fun createRoute(owner: String, repo: String, number: Int) = "issue_detail/$owner/$repo/$number"
    }
    object PullRequestList : Screen("pr_list/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "pr_list/$owner/$repo"
    }
    object PullRequestDetail : Screen("pr_detail/{owner}/{repo}/{number}") {
        fun createRoute(owner: String, repo: String, number: Int) = "pr_detail/$owner/$repo/$number"
    }
    object Search : Screen("search")
    object Profile : Screen("profile/{username}") {
        fun createRoute(username: String) = "profile/$username"
    }
    object FileBrowser : Screen("file_browser/{repoPath}") {
        fun createRoute(repoPath: String) = "file_browser/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object DiffViewer : Screen("diff_viewer/{repoPath}") {
        fun createRoute(repoPath: String) = "diff_viewer/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object StashList : Screen("stash_list/{repoPath}") {
        fun createRoute(repoPath: String) = "stash_list/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object TagList : Screen("tag_list/{repoPath}") {
        fun createRoute(repoPath: String) = "tag_list/${java.net.URLEncoder.encode(repoPath, "UTF-8")}"
    }
    object Releases : Screen("releases/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "releases/$owner/$repo"
    }
    object ReleaseDetail : Screen("release_detail/{owner}/{repo}/{id}") {
        fun createRoute(owner: String, repo: String, id: Long) = "release_detail/$owner/$repo/$id"
    }
    object Notifications : Screen("notifications")
    object CreateIssue : Screen("create_issue/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "create_issue/$owner/$repo"
    }
    object CreatePullRequest : Screen("create_pr/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "create_pr/$owner/$repo"
    }
}
