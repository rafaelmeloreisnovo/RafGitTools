package com.rafgittools.ui.screens.auth

import com.google.common.truth.Truth.assertThat
import com.rafgittools.data.auth.AuthMethod
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.auth.GhCliAuthImporter
import com.rafgittools.data.auth.OAuthDeviceFlowManager
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.core.security.SshKeyManager
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
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val githubRepository: GithubDataRepository = mockk(relaxed = true)
    private val authTokenCache = AuthTokenCache()
    private val deviceFlowManager: OAuthDeviceFlowManager = mockk(relaxed = true)
    private val ghCliAuthImporter: GhCliAuthImporter = mockk(relaxed = true)
    private val sshKeyManager: SshKeyManager = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { authRepository.isAuthenticated() } returns false
        coEvery { authRepository.getUsername() } returns null
        coEvery { authRepository.getPat() } returns Result.failure(Exception("No token"))
        coEvery { authRepository.getAuthMethod() } returns Result.failure(Exception("No method"))
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `selection method updates state`() = runTest(dispatcher) {
        val vm = AuthViewModel(authRepository, githubRepository, authTokenCache, deviceFlowManager, ghCliAuthImporter, sshKeyManager)
        vm.selectMethod(AuthMethod.DEVICE_CODE)
        assertThat(vm.selectedMethod.value).isEqualTo(AuthMethod.DEVICE_CODE)
    }

    @Test
    fun `continue offline does not call github api`() = runTest(dispatcher) {
        coEvery { authRepository.saveAuthMethod(AuthMethod.OFFLINE) } returns Result.success(Unit)
        val vm = AuthViewModel(authRepository, githubRepository, authTokenCache, deviceFlowManager, ghCliAuthImporter, sshKeyManager)
        vm.continueOffline()
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { githubRepository.getAuthenticatedUserSync() }
        assertThat(vm.uiState.value).isEqualTo(AuthUiState.Offline)
    }

    @Test
    fun `logout clears token and method state`() = runTest(dispatcher) {
        coEvery { authRepository.logout() } returns Result.success(Unit)
        val vm = AuthViewModel(authRepository, githubRepository, authTokenCache, deviceFlowManager, ghCliAuthImporter, sshKeyManager)
        authTokenCache.token = "abc"
        vm.selectMethod(AuthMethod.PAT)
        vm.logout()
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(authTokenCache.token).isNull()
        assertThat(vm.selectedMethod.value).isNull()
    }

    @Test
    fun `pat auth with empty token stays compatible with validation`() = runTest(dispatcher) {
        val vm = AuthViewModel(authRepository, githubRepository, authTokenCache, deviceFlowManager, ghCliAuthImporter, sshKeyManager)
        vm.authenticateWithPat("")
        val state = vm.uiState.value
        assertThat(state is AuthUiState.Error).isTrue()
        assertThat((state as AuthUiState.Error).message).contains("Token cannot be empty")
    }

    @Test
    fun `import gh cli without gh returns clear message`() = runTest(dispatcher) {
        val vm = AuthViewModel(authRepository, githubRepository, authTokenCache, deviceFlowManager, ghCliAuthImporter, sshKeyManager)
        vm.importGhCliToken()
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state is AuthUiState.Error).isTrue()
        assertThat((state as AuthUiState.Error).message).contains("gh CLI não encontrado")
    }
}
