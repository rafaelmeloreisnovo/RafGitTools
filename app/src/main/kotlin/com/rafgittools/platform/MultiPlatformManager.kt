package com.rafgittools.platform

/**
 * MultiPlatformManager stub.
 *
 * This object provides placeholder functions for integrating with
 * various Git hosting providers beyond GitHub. Each function
 * currently returns a simple message indicating that the feature
 * is not yet implemented but provides a defined contract that
 * callers can use without crashing the application.
 */
object MultiPlatformManager {
    fun getGitLabProjects(): List<String> {
        // In a future release this would call GitLab's API
        return emptyList()
    }

    fun getBitbucketRepositories(): List<String> {
        // Placeholder for Bitbucket API integration
        return emptyList()
    }

    fun getGiteaRepositories(): List<String> {
        // Placeholder for Gitea API integration
        return emptyList()
    }

    fun getAzureDevOpsRepos(): List<String> {
        // Placeholder for Azure DevOps API integration
        return emptyList()
    }
}