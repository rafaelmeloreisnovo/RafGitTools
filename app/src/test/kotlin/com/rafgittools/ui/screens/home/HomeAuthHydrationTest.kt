package com.rafgittools.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.cache.LocalRepositoryDao
import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubDataRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

    companion object {
        private const val TEST_CREDENTIAL = "TEST_CREDENTIAL_VALUE_NOT_A_REAL_TOKEN_1234567890"
    }
}
