package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitStatus
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
 * Unit tests for GetRepositoryStatusUseCase
 */
class GetRepositoryStatusUseCaseTest {
    
    private lateinit var gitRepository: GitRepository
    private lateinit var useCase: GetRepositoryStatusUseCase
    
    @Before
    fun setup() {
        gitRepository = mockk()
        useCase = GetRepositoryStatusUseCase(gitRepository)
    }
    
    @Test
    fun `when repoPath is blank, returns failure`() = runTest {
        val params = GetRepositoryStatusParams(repoPath = "")
        
        val result = useCase(params)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Repository path cannot be empty", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `when getStatus succeeds, returns success with status`() = runTest {
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
        
        coEvery { gitRepository.getStatus(any()) } returns Result.success(expectedStatus)
        
        val params = GetRepositoryStatusParams(repoPath = "/path/to/repo")
        
        val result = useCase(params)
        
        assertTrue(result.isSuccess)
        assertEquals(expectedStatus, result.getOrNull())
        coVerify { gitRepository.getStatus("/path/to/repo") }
    }
    
    @Test
    fun `when getStatus fails, returns failure`() = runTest {
        val exception = RuntimeException("Repository not found")
        
        coEvery { gitRepository.getStatus(any()) } returns Result.failure(exception)
        
        val params = GetRepositoryStatusParams(repoPath = "/path/to/repo")
        
        val result = useCase(params)
        
        assertTrue(result.isFailure)
        assertEquals("Repository not found", result.exceptionOrNull()?.message)
    }
}
