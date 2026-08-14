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
class AuthBootstrapIntegrityTest {
    private val dispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val deviceFlowManager: OAuthDeviceFlowManager = mockk(relaxed = true)
    private val ghCliAuthImporter: GhCliAuthImporter = mockk(relaxed = true)
    private val sshKeyManager: SshKeyManager = mockk(relaxed = true)
    private lateinit var authTokenCache: AuthTokenCache

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authTokenCache = AuthTokenCache()
        coEvery { authRepository.clearAuthState() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `online session with method and token but missing username is cleared fail closed`() = runTest(dispatcher) {
        val token = "TEST_VALID_TOKEN_12345678901234567890"
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { authRepository.isOfflineMode() } returns false
        coEvery { authRepository.getAuthMethod() } returns Result.success(AuthMethod.PAT)
        coEvery { authRepository.getPat() } returns Result.success(token)
        coEvery { authRepository.getUsername() } returns null

        val vm = AuthViewModel(
            authRepository,
            authTokenCache,
            deviceFlowManager,
            ghCliAuthImporter,
            sshKeyManager
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.clearAuthState() }
        assertThat(authTokenCache.token).isNull()
        assertThat(vm.isAuthenticated.value).isFalse()
        assertThat(vm.username.value).isNull()
        assertThat(vm.selectedMethod.value).isNull()
        assertThat(vm.uiState.value).isEqualTo(AuthUiState.ChoosingMethod)
    }
}
