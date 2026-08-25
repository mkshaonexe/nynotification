package com.quietinbox.feature.settings.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.quietinbox.core.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages App Lock authentication state, background timeout tracking (60 seconds),
 * and biometric prompt invocation.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val clock: Clock
) {

    companion object {
        const val LOCK_TIMEOUT_MS = 60_000L // 60 seconds background timeout
    }

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundEpochMs: Long = 0L
    private var isInitialized = false

    /**
     * Initializes lock state on application cold start.
     */
    fun onColdStart(appLockEnabled: Boolean) {
        if (appLockEnabled) {
            _isLocked.value = true
        }
        isInitialized = true
    }

    /**
     * Called when the application enters the background.
     */
    fun onAppBackgrounded() {
        lastBackgroundEpochMs = clock.now()
    }

    /**
     * Called when the application returns to the foreground.
     */
    fun onAppForegrounded(appLockEnabled: Boolean) {
        if (!appLockEnabled) {
            _isLocked.value = false
            return
        }

        if (!isInitialized) {
            _isLocked.value = true
            isInitialized = true
            return
        }

        val elapsedInBackground = clock.now() - lastBackgroundEpochMs
        if (lastBackgroundEpochMs > 0L && elapsedInBackground >= LOCK_TIMEOUT_MS) {
            _isLocked.value = true
        }
    }

    /**
     * Manually locks the application.
     */
    fun lock() {
        _isLocked.value = true
    }

    /**
     * Unlocks the application upon successful authentication.
     */
    fun unlock() {
        _isLocked.value = false
        lastBackgroundEpochMs = 0L
    }

    /**
     * Checks if biometric or device credential authentication is available on the device.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Triggers the biometric prompt on the provided [activity].
     */
    fun promptBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                unlock()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Transient biometric failure (fingerprint not recognized)
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
