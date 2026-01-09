package com.rafgittools.ui.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object RepositoryList : Screen("repository_list")
    object RepositoryDetail : Screen("repository_detail/{repoPath}") {
        fun createRoute(repoPath: String) = "repository_detail/$repoPath"
    }
    object CommitList : Screen("commit_list/{repoPath}") {
        fun createRoute(repoPath: String) = "commit_list/$repoPath"
    }
    object BranchList : Screen("branch_list/{repoPath}") {
        fun createRoute(repoPath: String) = "branch_list/$repoPath"
    }
    object Settings : Screen("settings")
}
