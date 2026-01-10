package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitAuthor
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.repository.GitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetCommitHistoryUseCase
 */
class GetCommitHistoryUseCaseTest {
    
    private lateinit var gitRepository: GitRepository
    private lateinit var useCase: GetCommitHistoryUseCase
    
    @Before
    fun setup() {
        gitRepository = mockk()
        useCase = GetCommitHistoryUseCase(gitRepository)
    }
    
    @Test
    fun `when repoPath is blank, returns failure`() = runTest {
        val params = GetCommitHistoryParams(repoPath = "")
        
        val result = useCase(params)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Repository path cannot be empty", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `when getCommits succeeds, returns success with commits`() = runTest {
        val expectedCommits = listOf(
            GitCommit(
                sha = "abc123",
                message = "feat: add new feature",
                author = GitAuthor("John Doe", "john@example.com"),
                committer = GitAuthor("John Doe", "john@example.com"),
                timestamp = 1234567890L,
                parents = emptyList()
            ),
            GitCommit(
                sha = "def456",
                message = "fix: bug fix",
                author = GitAuthor("Jane Doe", "jane@example.com"),
                committer = GitAuthor("Jane Doe", "jane@example.com"),
                timestamp = 1234567800L,
                parents = listOf("abc123")
            )
        )
        
        coEvery { 
            gitRepository.getCommits(any(), any(), any()) 
        } returns Result.success(expectedCommits)
        
        val params = GetCommitHistoryParams(
            repoPath = "/path/to/repo",
            branch = "main",
            limit = 50
        )
        
        val result = useCase(params)
        
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("abc123", result.getOrNull()?.first()?.sha)
        coVerify { 
            gitRepository.getCommits(
                repoPath = "/path/to/repo",
                branch = "main",
                limit = 50
            )
        }
    }
    
    @Test
    fun `when limit exceeds max, it is capped`() = runTest {
        coEvery { 
            gitRepository.getCommits(any(), any(), any()) 
        } returns Result.success(emptyList())
        
        val params = GetCommitHistoryParams(
            repoPath = "/path/to/repo",
            limit = 5000 // Exceeds MAX_LIMIT of 1000
        )
        
        useCase(params)
        
        coVerify { 
            gitRepository.getCommits(
                repoPath = "/path/to/repo",
                branch = null,
                limit = GetCommitHistoryUseCase.MAX_LIMIT // Should be capped to 1000
            )
        }
    }
    
    @Test
    fun `when limit is less than 1, it is set to minimum`() = runTest {
        coEvery { 
            gitRepository.getCommits(any(), any(), any()) 
        } returns Result.success(emptyList())
        
        val params = GetCommitHistoryParams(
            repoPath = "/path/to/repo",
            limit = -5 // Invalid negative limit
        )
        
        useCase(params)
        
        coVerify { 
            gitRepository.getCommits(
                repoPath = "/path/to/repo",
                branch = null,
                limit = 1 // Should be set to minimum
            )
        }
    }
}
