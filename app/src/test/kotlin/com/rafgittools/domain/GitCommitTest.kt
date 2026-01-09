package com.rafgittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for GitCommit domain model
 */
class GitCommitTest {
    
    @Test
    fun `create GitCommit with all fields`() {
        val author = GitAuthor(
            name = "John Doe",
            email = "john@example.com"
        )
        
        val committer = GitAuthor(
            name = "Jane Doe",
            email = "jane@example.com"
        )
        
        val commit = GitCommit(
            sha = "abc123",
            message = "Initial commit",
            author = author,
            committer = committer,
            timestamp = 1234567890L,
            parents = listOf("parent1", "parent2")
        )
        
        assertEquals("abc123", commit.sha)
        assertEquals("Initial commit", commit.message)
        assertEquals("John Doe", commit.author.name)
        assertEquals("john@example.com", commit.author.email)
        assertEquals("Jane Doe", commit.committer.name)
        assertEquals("jane@example.com", commit.committer.email)
        assertEquals(1234567890L, commit.timestamp)
        assertEquals(2, commit.parents.size)
    }
    
    @Test
    fun `create GitAuthor`() {
        val author = GitAuthor(
            name = "Test User",
            email = "test@example.com"
        )
        
        assertEquals("Test User", author.name)
        assertEquals("test@example.com", author.email)
    }
}
