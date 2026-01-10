package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitAuthor
import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CloneRepositoryUseCase
 */
class CloneRepositoryUseCaseTest {
    
    private lateinit var gitRepository: IGitRepository
    private lateinit var useCase: CloneRepositoryUseCase
    
    @Before
    fun setup() {
        gitRepository = mockk()
        useCase = CloneRepositoryUseCase(gitRepository)
    }
    
    @Test
    fun `when url is blank, returns failure`() = runTest {
        val params = CloneRepositoryParams(
            url = "",
            localPath = "/path/to/repo"
        )
        
        val result = useCase(params)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Repository URL cannot be empty", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `when localPath is blank, returns failure`() = runTest {
        val params = CloneRepositoryParams(
            url = "https://github.com/user/repo.git",
            localPath = ""
        )
        
        val result = useCase(params)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Local path cannot be empty", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `when clone succeeds, returns success with repository`() = runTest {
        val expectedRepo = GitRepository(
            id = "/path/to/repo",
            name = "repo",
            path = "/path/to/repo",
            remoteUrl = "https://github.com/user/repo.git",
            currentBranch = "main",
            lastUpdated = System.currentTimeMillis()
        )
        
        coEvery { 
            gitRepository.cloneRepository(any(), any(), any()) 
        } returns Result.success(expectedRepo)
        
        val params = CloneRepositoryParams(
            url = "https://github.com/user/repo.git",
            localPath = "/path/to/repo",
            credentials = Credentials.Token("ghp_xxx")
        )
        
        val result = useCase(params)
        
        assertTrue(result.isSuccess)
        assertEquals(expectedRepo, result.getOrNull())
        coVerify { 
            gitRepository.cloneRepository(
                url = "https://github.com/user/repo.git",
                localPath = "/path/to/repo",
                credentials = any()
            )
        }
    }
    
    @Test
    fun `when clone fails, returns failure`() = runTest {
        val exception = RuntimeException("Clone failed")
        
        coEvery { 
            gitRepository.cloneRepository(any(), any(), any()) 
        } returns Result.failure(exception)
        
        val params = CloneRepositoryParams(
            url = "https://github.com/user/repo.git",
            localPath = "/path/to/repo"
        )
        
        val result = useCase(params)
        
        assertTrue(result.isFailure)
        assertEquals("Clone failed", result.exceptionOrNull()?.message)
    }
}
