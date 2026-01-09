package com.rafgittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for GitRepository domain model
 */
class GitRepositoryTest {
    
    @Test
    fun `create GitRepository with all fields`() {
        val repository = GitRepository(
            id = "repo-1",
            name = "test-repo",
            path = "/path/to/repo",
            remoteUrl = "https://github.com/user/repo.git",
            currentBranch = "main",
            lastUpdated = 1234567890L,
            description = "Test repository",
            isPrivate = false
        )
        
        assertEquals("repo-1", repository.id)
        assertEquals("test-repo", repository.name)
        assertEquals("/path/to/repo", repository.path)
        assertEquals("https://github.com/user/repo.git", repository.remoteUrl)
        assertEquals("main", repository.currentBranch)
        assertEquals(1234567890L, repository.lastUpdated)
        assertEquals("Test repository", repository.description)
        assertEquals(false, repository.isPrivate)
    }
    
    @Test
    fun `create GitRepository with minimal fields`() {
        val repository = GitRepository(
            id = "repo-1",
            name = "test-repo",
            path = "/path/to/repo",
            remoteUrl = null,
            currentBranch = null,
            lastUpdated = 1234567890L
        )
        
        assertEquals("repo-1", repository.id)
        assertEquals("test-repo", repository.name)
        assertEquals("/path/to/repo", repository.path)
        assertEquals(null, repository.remoteUrl)
        assertEquals(null, repository.currentBranch)
    }
}
