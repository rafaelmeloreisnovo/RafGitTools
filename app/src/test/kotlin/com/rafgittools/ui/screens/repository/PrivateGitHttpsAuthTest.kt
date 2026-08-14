package com.rafgittools.ui.screens.repository

import com.google.common.truth.Truth.assertThat
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.storage.RepoStorage
import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as GitRepositoryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PrivateGitHttpsAuthTest {
    private val dispatcher = StandardTestDispatcher()
    private val gitRepository: GitRepositoryPort = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val repoStorage: RepoStorage = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { authRepository.getPat() } returns Result.success(TEST_CREDENTIAL)
        coEvery { authRepository.getUsername() } returns "rafael"
        every { repoStorage.baseDir } returns File("/tmp/rafgittools-test-repos")
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `private clone passes credential as HTTPS password`() = runTest(dispatcher) {
        val repo = GitRepository(
            id = "/tmp/rafgittools-test-repos/private-repo",
            name = "private-repo",
            path = "/tmp/rafgittools-test-repos/private-repo",
            remoteUrl = PRIVATE_URL,
            currentBranch = "main",
            lastUpdated = 0L
        )
        coEvery { gitRepository.cloneRepository(any(), any(), any()) } returns Result.success(repo)

        val vm = AddRepositoryViewModel(gitRepository, authRepository, repoStorage)
        vm.onRemoteUrlChanged(PRIVATE_URL)
        vm.cloneRepository()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            gitRepository.cloneRepository(
                PRIVATE_URL,
                any(),
                Credentials.UsernamePassword("rafael", TEST_CREDENTIAL)
            )
        }
        assertThat(vm.uiState.value.isSuccess).isTrue()
    }

    @Test
    fun `private push pull fetch pass credential as HTTPS password`() = runTest(dispatcher) {
        val repo = GitRepository(
            id = "/repo",
            name = "repo",
            path = "/repo",
            remoteUrl = PRIVATE_URL,
            currentBranch = "main",
            lastUpdated = 0L
        )
        coEvery { gitRepository.getRepository("/repo") } returns Result.success(repo)
        coEvery { gitRepository.getStatus(any()) } returns Result.failure(Exception("fixture"))
        coEvery { gitRepository.getBranches(any()) } returns Result.success(emptyList())
        coEvery { gitRepository.getCommits(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { gitRepository.push(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { gitRepository.pull(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { gitRepository.fetch(any(), any(), any()) } returns Result.success(Unit)

        val vm = RepositoryDetailViewModel(gitRepository, authRepository)
        vm.loadRepository("/repo")
        dispatcher.scheduler.advanceUntilIdle()

        val expected = Credentials.UsernamePassword("rafael", TEST_CREDENTIAL)
        vm.push(); vm.pull(); vm.fetch()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { gitRepository.push("/repo", "origin", null, expected) }
        coVerify(exactly = 1) { gitRepository.pull("/repo", "origin", null, expected) }
        coVerify(exactly = 1) { gitRepository.fetch("/repo", "origin", expected) }
    }

    companion object {
        private const val PRIVATE_URL = "https://github.com/rafaelmeloreisnovo/private-repo.git"
        private const val TEST_CREDENTIAL = "TEST_CREDENTIAL_VALUE_NOT_A_REAL_TOKEN_1234567890"
    }
}
