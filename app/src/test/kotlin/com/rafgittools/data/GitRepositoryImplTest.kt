package com.rafgittools.data.repository

import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.GitAuthor
import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GitRepositoryImpl
 */
class GitRepositoryImplTest {
    
    private lateinit var jGitService: JGitService
    private lateinit var gitRepository: GitRepositoryImpl
    
    @Before
    fun setup() {
        jGitService = mockk()
        gitRepository = GitRepositoryImpl(jGitService)
    }
    
    @Test
    fun `getStatus returns success`() = runTest {
        val expectedStatus = GitStatus(
            branch = "main",
            added = listOf("file1.txt"),
            changed = emptyList(),
            removed = emptyList(),
            modified = listOf("file2.txt"),
            untracked = listOf("file3.txt"),
            conflicting = emptyList(),
            hasUncommittedChanges = true
        )
        
        coEvery { jGitService.getStatus(any()) } returns Result.success(expectedStatus)
        
        val result = gitRepository.getStatus("/path/to/repo")
        
        assertTrue(result.isSuccess)
        assertEquals(expectedStatus, result.getOrNull())
    }
    
    @Test
    fun `getCommits returns success`() = runTest {
        val expectedCommits = listOf(
            GitCommit(
                sha = "abc123",
                message = "Test commit",
                author = GitAuthor("John Doe", "john@example.com"),
                committer = GitAuthor("John Doe", "john@example.com"),
                timestamp = 1234567890L,
                parents = emptyList()
            )
        )
        
        coEvery { 
            jGitService.getCommits(any(), any(), any()) 
        } returns Result.success(expectedCommits)
        
        val result = gitRepository.getCommits("/path/to/repo", "main", 50)
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("abc123", result.getOrNull()?.first()?.sha)
    }
    
    @Test
    fun `getBranches returns success`() = runTest {
        val expectedBranches = listOf(
            GitBranch(
                name = "refs/heads/main",
                shortName = "main",
                isLocal = true,
                isRemote = false,
                isCurrent = true,
                commitSha = "abc123"
            )
        )
        
        coEvery { jGitService.getBranches(any()) } returns Result.success(expectedBranches)
        
        val result = gitRepository.getBranches("/path/to/repo")
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("main", result.getOrNull()?.first()?.shortName)
    }
    
    @Test
    fun `getStatus returns failure on error`() = runTest {
        coEvery { 
            jGitService.getStatus(any()) 
        } returns Result.failure(Exception("Test error"))
        
        val result = gitRepository.getStatus("/path/to/repo")
        
        assertTrue(result.isFailure)
    }
}
