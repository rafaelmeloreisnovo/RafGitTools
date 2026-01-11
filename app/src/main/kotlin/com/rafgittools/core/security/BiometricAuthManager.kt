package com.rafgittools.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Biometric Authentication Manager implementing Feature #67 from the roadmap.
 * 
 * Provides biometric authentication capabilities for the app including:
 * - Fingerprint authentication
 * - Face recognition (on supported devices)
 * - Device credential fallback
 * 
 * Standards compliance:
 * - FIDO2/WebAuthn
 * - Android BiometricPrompt API
 * - OWASP MASVS (Mobile Application Security Verification Standard)
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val biometricManager = BiometricManager.from(context)
    
    private val _authenticationState = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    val authenticationState: Flow<BiometricAuthState> = _authenticationState.asStateFlow()
    
    /**
     * Check if biometric authentication is available on this device.
     */
    fun canAuthenticate(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            else -> BiometricAvailability.Unknown
        }
    }
    
    /**
     * Check if device credentials (PIN/Pattern/Password) are available.
     */
    fun canAuthenticateWithDeviceCredential(): Boolean {
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Check if either biometric or device credential authentication is available.
     */
    fun canAuthenticateWithAny(): Boolean {
        val combined = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(combined) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Authenticate using biometrics.
     * 
     * @param activity The FragmentActivity to attach the prompt to
     * @param title The title shown in the biometric prompt
     * @param subtitle The subtitle shown in the biometric prompt
     * @param negativeButtonText The text for the cancel button
     * @param allowDeviceCredential Whether to allow fallback to device credentials
     * @param callback Callback for authentication result
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        negativeButtonText: String = "Cancel",
        allowDeviceCredential: Boolean = false,
        callback: BiometricAuthCallback
    ) {
        _authenticationState.value = BiometricAuthState.Authenticating
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _authenticationState.value = BiometricAuthState.Success
                callback.onSuccess(result)
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't update state here - failed just means the attempt didn't work
                // The user can try again
                callback.onFailed()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val error = BiometricAuthError.fromErrorCode(errorCode, errString.toString())
                _authenticationState.value = BiometricAuthState.Error(error)
                callback.onError(error)
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, authCallback)
        
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
        
        if (allowDeviceCredential && canAuthenticateWithDeviceCredential()) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            promptInfoBuilder
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        }
        
        val promptInfo = promptInfoBuilder.build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Reset authentication state to idle.
     */
    fun resetState() {
        _authenticationState.value = BiometricAuthState.Idle
    }
    
    /**
     * Get a human-readable description of biometric availability.
     */
    fun getAvailabilityDescription(): String {
        return when (canAuthenticate()) {
            BiometricAvailability.Available -> "Biometric authentication is available"
            BiometricAvailability.NoHardware -> "No biometric hardware detected"
            BiometricAvailability.HardwareUnavailable -> "Biometric hardware is currently unavailable"
            BiometricAvailability.NoneEnrolled -> "No biometric credentials enrolled. Please set up fingerprint or face recognition in device settings."
            BiometricAvailability.SecurityUpdateRequired -> "A security update is required for biometric authentication"
            BiometricAvailability.Unknown -> "Unable to determine biometric availability"
        }
    }
}

/**
 * Represents the availability status of biometric authentication.
 */
sealed class BiometricAvailability {
    data object Available : BiometricAvailability()
    data object NoHardware : BiometricAvailability()
    data object HardwareUnavailable : BiometricAvailability()
    data object NoneEnrolled : BiometricAvailability()
    data object SecurityUpdateRequired : BiometricAvailability()
    data object Unknown : BiometricAvailability()
}

/**
 * Represents the current state of biometric authentication.
 */
sealed class BiometricAuthState {
    data object Idle : BiometricAuthState()
    data object Authenticating : BiometricAuthState()
    data object Success : BiometricAuthState()
    data class Error(val error: BiometricAuthError) : BiometricAuthState()
}

/**
 * Represents a biometric authentication error.
 */
data class BiometricAuthError(
    val code: Int,
    val message: String,
    val isRecoverable: Boolean
) {
    companion object {
        fun fromErrorCode(code: Int, message: String): BiometricAuthError {
            val isRecoverable = when (code) {
                BiometricPrompt.ERROR_LOCKOUT -> true // Temporary lockout
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> false
                BiometricPrompt.ERROR_CANCELED -> true
                BiometricPrompt.ERROR_USER_CANCELED -> true
                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> true
                BiometricPrompt.ERROR_NO_BIOMETRICS -> false
                BiometricPrompt.ERROR_HW_NOT_PRESENT -> false
                BiometricPrompt.ERROR_HW_UNAVAILABLE -> true
                BiometricPrompt.ERROR_TIMEOUT -> true
                BiometricPrompt.ERROR_NO_SPACE -> false
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> true
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> false
                BiometricPrompt.ERROR_VENDOR -> false
                else -> false
            }
            return BiometricAuthError(code, message, isRecoverable)
        }
    }
}

/**
 * Callback interface for biometric authentication results.
 */
interface BiometricAuthCallback {
    /**
     * Called when authentication succeeds.
     */
    fun onSuccess(result: BiometricPrompt.AuthenticationResult)
    
    /**
     * Called when an authentication attempt fails (but more attempts are allowed).
     */
    fun onFailed()
    
    /**
     * Called when an error occurs during authentication.
     */
    fun onError(error: BiometricAuthError)
}

/**
 * Simple callback implementation that can be used with lambdas.
 */
class SimpleBiometricAuthCallback(
    private val onSuccessAction: (BiometricPrompt.AuthenticationResult) -> Unit = {},
    private val onFailedAction: () -> Unit = {},
    private val onErrorAction: (BiometricAuthError) -> Unit = {}
) : BiometricAuthCallback {
    override fun onSuccess(result: BiometricPrompt.AuthenticationResult) = onSuccessAction(result)
    override fun onFailed() = onFailedAction()
    override fun onError(error: BiometricAuthError) = onErrorAction(error)
}
