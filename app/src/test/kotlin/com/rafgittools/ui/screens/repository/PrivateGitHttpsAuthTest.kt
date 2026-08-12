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
        coEvery { authRepository.getPat() } returns Result.success(TEST_PAT)
        coEvery { authRepository.getUsername() } returns "rafael"
        every { repoStorage.baseDir } returns File("/tmp/rafgittools-test-repos")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `private clone passes PAT as HTTPS password`() = runTest(dispatcher) {
        val expected = GitRepository(
            id = "/tmp/rafgittools-test-repos/private-repo",
            name = "private-repo",
            path = "/tmp/rafgittools-test-repos/private-repo",
            remoteUrl = "https://github.com/rafaelmeloreisnovo/private-repo.git",
            currentBranch = "main",
            lastUpdated = 0L
        )
        coEvery { gitRepository.cloneRepository(any(), any(), any()) } returns Result.success(expected)

        val vm = AddRepositoryViewModel(gitRepository, authRepository, repoStorage)
        vm.onRemoteUrlChanged("https://github.com/rafaelmeloreisnovo/private-repo.git")
        vm.cloneRepository()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            gitRepository.cloneRepository(
                "https://github.com/rafaelmeloreisnovo/private-repo.git",
                any(),
                Credentials.UsernamePassword("rafael", TEST_PAT)
            )
        }
        assertThat(vm.uiState.value.isSuccess).isTrue()
    }

    @Test
    fun `private push pull fetch pass PAT as HTTPS password`() = runTest(dispatcher) {
        val repo = GitRepository(
            id = "/repo",
            name = "repo",
            path = "/repo",
            remoteUrl = "https://github.com/rafaelmeloreisnovo/repo.git",
            currentBranch = "main",
            lastUpdated = 0L
        )
        coEvery { gitRepository.getRepository("/repo") } returns Result.success(repo)
        coEvery { gitRepository.push(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { gitRepository.pull(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { gitRepository.fetch(any(), any(), any()) } returns Result.success(Unit)

        val vm = RepositoryDetailViewModel(gitRepository, authRepository)
        vm.loadRepository("/repo")
        dispatcher.scheduler.advanceUntilIdle()

        val expectedCredentials = Credentials.UsernamePassword("rafael", TEST_PAT)

        vm.push()
        vm.pull()
        vm.fetch()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { gitRepository.push("/repo", "origin", null, expectedCredentials) }
        coVerify(exactly = 1) { gitRepository.pull("/repo", "origin", null, expectedCredentials) }
        coVerify(exactly = 1) { gitRepository.fetch("/repo", "origin", expectedCredentials) }
    }

    @Test
    fun `missing username falls back to non-secret x-access-token marker`() = runTest(dispatcher) {
        coEvery { authRepository.getUsername() } returns null
        coEvery { gitRepository.push(any(), any(), any(), any()) } returns Result.success(Unit)

        val vm = RepositoryDetailViewModel(gitRepository, authRepository)
        vm.push()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            gitRepository.push(
                "",
                "origin",
                null,
                Credentials.UsernamePassword("x-access-token", TEST_PAT)
            )
        }
    }

    companion object {
        private const val TEST_PAT = "TEST_CREDENTIAL_VALUE_NOT_A_REAL_TOKEN_1234567890"
    }
}
