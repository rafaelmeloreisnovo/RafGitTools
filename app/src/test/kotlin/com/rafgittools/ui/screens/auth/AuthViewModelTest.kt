package com.rafgittools.ui.screens.auth

import com.google.common.truth.Truth.assertThat
import com.rafgittools.core.security.SshKeyManager
import com.rafgittools.data.auth.AuthMethod
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.auth.GhCliAuthImporter
import com.rafgittools.data.auth.OAuthDeviceFlowManager
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
    private val authTokenCache = AuthTokenCache()
    private val deviceFlowManager: OAuthDeviceFlowManager = mockk(relaxed = true)
    private val ghCliAuthImporter: GhCliAuthImporter = mockk(relaxed = true)
    private val sshKeyManager: SshKeyManager = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { authRepository.isAuthenticated() } returns false
        coEvery { authRepository.isOfflineMode() } returns false
        coEvery { authRepository.getUsername() } returns null
        coEvery { authRepository.getPat() } returns Result.failure(Exception("No token"))
        coEvery { authRepository.getAuthMethod() } returns Result.failure(Exception("No method"))
        coEvery { authRepository.setOfflineMode(any()) } returns Result.success(Unit)
        coEvery { authRepository.saveAuthMethod(any()) } returns Result.success(Unit)
        coEvery { authRepository.clearAuthState() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selection method updates state`() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.selectMethod(AuthMethod.DEVICE_CODE)
        assertThat(vm.selectedMethod.value).isEqualTo(AuthMethod.DEVICE_CODE)
    }

    @Test
    fun `continue offline does not call github api`() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.continueOffline()
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value).isEqualTo(AuthUiState.Offline)
    }

    @Test
    fun `logout clears token and method state`() = runTest(dispatcher) {
        val vm = createViewModel()
        authTokenCache.token = "abc"
        vm.selectMethod(AuthMethod.PAT)
        vm.logout()
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(authTokenCache.token).isNull()
        assertThat(vm.selectedMethod.value).isNull()
    }

    @Test
    fun `pat auth with empty token returns explicit validation error`() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.authenticateWithPat("")
        val state = vm.uiState.value
        assertThat(state is AuthUiState.Error).isTrue()
        assertThat((state as AuthUiState.Error).message).contains("não pode ficar vazio")
    }

    @Test
    fun `pat auth validates remotely before persisting session`() = runTest(dispatcher) {
        val token = "github_pat_123456789012345678901234567890"
        coEvery { deviceFlowManager.validateToken(token) } returns Result.success("rafael")
        coEvery { authRepository.savePat(token, "rafael") } returns Result.success(Unit)

        val vm = createViewModel()
        vm.authenticateWithPat("  $token  ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { deviceFlowManager.validateToken(token) }
        coVerify(exactly = 1) { authRepository.savePat(token, "rafael") }
        assertThat(authTokenCache.token).isEqualTo(token)
        assertThat(vm.isAuthenticated.value).isTrue()
        assertThat(vm.username.value).isEqualTo("rafael")
        assertThat(vm.uiState.value).isEqualTo(AuthUiState.Success("rafael", AuthMethod.PAT))
    }

    @Test
    fun `rejected token is never persisted`() = runTest(dispatcher) {
        val token = "invalid-token-value"
        coEvery { deviceFlowManager.validateToken(token) } returns
            Result.failure(IllegalArgumentException("GitHub recusou o token"))

        val vm = createViewModel()
        vm.authenticateWithPat(token)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { authRepository.savePat(any(), any()) }
        assertThat(authTokenCache.token).isNull()
        assertThat(vm.isAuthenticated.value).isFalse()
        assertThat(vm.uiState.value is AuthUiState.Error).isTrue()
    }

    @Test
    fun `import gh cli without gh returns clear message`() = runTest(dispatcher) {
        coEvery { ghCliAuthImporter.importToken() } returns
            Result.failure(Exception("gh CLI não encontrado. Use PAT ou Device Code."))
        val vm = createViewModel()
        vm.importGhCliToken()
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state is AuthUiState.Error).isTrue()
        assertThat((state as AuthUiState.Error).message).contains("gh CLI não encontrado")
    }

    private fun createViewModel() = AuthViewModel(
        authRepository,
        authTokenCache,
        deviceFlowManager,
        ghCliAuthImporter,
        sshKeyManager
    )
}
