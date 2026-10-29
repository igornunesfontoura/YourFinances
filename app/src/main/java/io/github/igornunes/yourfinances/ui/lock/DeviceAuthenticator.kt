package io.github.igornunes.yourfinances.ui.lock

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Whether the device has a screen lock (PIN, pattern or password) the app can ask for. */
fun interface DeviceLock {
    fun isDeviceSecure(): Boolean
}

class KeyguardDeviceLock(private val context: Context) : DeviceLock {
    override fun isDeviceSecure(): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
}

/**
 * Biometrics with the device PIN/pattern/password as the fallback, so the app keeps no PIN of
 * its own. Before API 30, strong biometrics cannot be combined with the device credential.
 *
 * Create it in `onCreate`: the prompt then reattaches its callback after a configuration change.
 * [onSuccess] receives the request that asked for it.
 */
class DeviceAuthenticator(activity: FragmentActivity, onSuccess: (AuthRequest) -> Unit) {
    private var pending: AuthRequest? = null

    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // After a recreation the pending request is gone; only unlocking is safe to assume.
                onSuccess(pending ?: AuthRequest.UNLOCK)
                pending = null
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                pending = null
            }
        },
    )

    fun authenticate(request: AuthRequest, title: String) {
        pending = request
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
        } else {
            Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
        }
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}

enum class AuthRequest { UNLOCK, DISABLE_LOCK }
