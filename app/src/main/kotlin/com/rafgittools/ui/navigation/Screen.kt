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
}
