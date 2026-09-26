package com.rafgittools.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.cache.LocalRepositoryDao
import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.domain.model.github.GithubIssue
import com.rafgittools.domain.model.github.GithubUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAuthHydrationTest {
    private val dispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val githubRepository: GithubDataRepository = mockk(relaxed = true)
    private val jGitService: JGitService = mockk(relaxed = true)
    private val localRepositoryDao: LocalRepositoryDao = mockk(relaxed = true)
    private lateinit var authTokenCache: AuthTokenCache

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authTokenCache = AuthTokenCache()
        every { localRepositoryDao.observeAll() } returns flowOf(emptyList())
        coEvery { localRepositoryDao.loadAll() } returns emptyList()
        coEvery { githubRepository.getAuthenticatedUserSync() } returns Result.failure(Exception("fixture"))
        coEvery { githubRepository.getUserRepositoriesSync(any(), any()) } returns Result.failure(Exception("fixture"))
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `persisted online session hydrates interceptor cache before remote load`() = runTest(dispatcher) {
        coEvery { authRepository.isOfflineMode() } returns false
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { authRepository.getPat() } returns Result.success(TEST_CREDENTIAL)

        HomeViewModel(
            authRepository,
            githubRepository,
            authTokenCache,
            jGitService,
            localRepositoryDao
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(authTokenCache.token).isEqualTo(TEST_CREDENTIAL)
        coVerify(exactly = 1) { authRepository.getPat() }
        coVerify(atLeast = 1) { githubRepository.getAuthenticatedUserSync() }
    }

    @Test
    fun `online session without persisted credential fails closed before github api`() = runTest(dispatcher) {
        coEvery { authRepository.isOfflineMode() } returns false
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { authRepository.getPat() } returns Result.failure(Exception("missing"))

        val vm = HomeViewModel(
            authRepository,
            githubRepository,
            authTokenCache,
            jGitService,
            localRepositoryDao
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(authTokenCache.token).isNull()
        assertThat(vm.isAuthenticated.value).isFalse()
        assertThat(vm.uiState.value).isNotEqualTo(HomeUiState.Loading)
        coVerify(exactly = 0) { githubRepository.getAuthenticatedUserSync() }
        coVerify(exactly = 0) { githubRepository.getUserRepositoriesSync(any(), any()) }
    }

    @Test
    fun `live github probe reports pass only from uncached probe`() = runTest(dispatcher) {
        coEvery { authRepository.isOfflineMode() } returns false
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { authRepository.getPat() } returns Result.success(TEST_CREDENTIAL)
        coEvery { githubRepository.probeAuthenticatedUserLive() } returns Result.success(TEST_USER)

        val vm = HomeViewModel(
            authRepository,
            githubRepository,
            authTokenCache,
            jGitService,
            localRepositoryDao
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.runGithubConnectivityTest()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.githubProbeState.value)
            .isEqualTo(GithubConnectivityState.Passed(TEST_USER.login, 0))
        coVerify(exactly = 1) { githubRepository.probeAuthenticatedUserLive() }
    }

    @Test
    fun `remote receipt reports issue number after explicit write`() = runTest(dispatcher) {
        coEvery { authRepository.isOfflineMode() } returns false
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { authRepository.getPat() } returns Result.success(TEST_CREDENTIAL)
        coEvery { githubRepository.probeAuthenticatedUserLive() } returns Result.success(TEST_USER)
        coEvery {
            githubRepository.createConnectivityReceiptIssue(
                owner = "rafaelmeloreisnovo",
                repo = "RafGitTools",
                title = any(),
                body = any()
            )
        } returns Result.success(TEST_ISSUE)

        val vm = HomeViewModel(
            authRepository,
            githubRepository,
            authTokenCache,
            jGitService,
            localRepositoryDao
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.createRemoteConnectivityReceipt()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.remoteReceiptState.value)
            .isEqualTo(RemoteReceiptState.Passed(TEST_ISSUE.number, TEST_ISSUE.htmlUrl))
    }

    companion object {
        private const val TEST_CREDENTIAL = "TEST_CREDENTIAL_VALUE_NOT_A_REAL_TOKEN_1234567890"

        private val TEST_USER = GithubUser(
            id = 1,
            login = "fixture-user",
            avatarUrl = "",
            htmlUrl = "https://example.invalid/fixture-user",
            type = "User"
        )

        private val TEST_ISSUE = GithubIssue(
            id = 494,
            number = 494,
            title = "fixture receipt",
            body = "RAFGITTOOLS_CONNECTIVITY_RECEIPT_V1",
            state = "open",
            user = TEST_USER,
            labels = emptyList(),
            assignees = emptyList(),
            createdAt = "2026-09-26T12:00:05Z",
            updatedAt = "2026-09-26T12:00:05Z",
            closedAt = null,
            htmlUrl = "https://example.invalid/issues/494",
            commentsCount = 0
        )
    }
}
