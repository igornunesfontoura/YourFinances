package io.github.igornunes.yourfinances.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.data.security.DatabaseRecovery
import io.github.igornunes.yourfinances.data.security.KeyStatus
import io.github.igornunes.yourfinances.data.settings.Settings
import io.github.igornunes.yourfinances.ui.lock.AppLock
import io.github.igornunes.yourfinances.ui.lock.AuthRequest
import io.github.igornunes.yourfinances.ui.lock.DeviceAuthenticator
import io.github.igornunes.yourfinances.ui.lock.DeviceLock
import io.github.igornunes.yourfinances.ui.lock.LockScreen
import io.github.igornunes.yourfinances.ui.navigation.AppNavHost
import io.github.igornunes.yourfinances.ui.recovery.RecoveryRoute
import io.github.igornunes.yourfinances.ui.theme.YourFinancesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** A FragmentActivity because BiometricPrompt needs one. */
class MainActivity : FragmentActivity() {
    private val appLock: AppLock by inject()
    private val settings: Settings by inject()
    private val deviceLock: DeviceLock by inject()
    private val recovery: DatabaseRecovery by inject()
    private lateinit var authenticator: DeviceAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Blocks screenshots, screen recording and the recents thumbnail.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        authenticator = DeviceAuthenticator(this) { request ->
            when (request) {
                AuthRequest.UNLOCK -> appLock.unlock()
                AuthRequest.DISABLE_LOCK -> settings.setLockEnabled(false)
            }
        }
        enableEdgeToEdge()
        setContent {
            YourFinancesTheme {
                AppContent()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.onForeground(lockAvailable = settings.lockEnabled.value && deviceLock.isDeviceSecure())
    }

    override fun onStop() {
        super.onStop()
        appLock.onBackground()
    }

    @Composable
    private fun AppContent() {
        val locked by appLock.locked.collectAsStateWithLifecycle()
        var keyStatus by remember { mutableStateOf<KeyStatus?>(null) }
        LaunchedEffect(Unit) { keyStatus = withContext(Dispatchers.IO) { recovery.keyStatus() } }
        // The app leaves composition while locked, so no dialog or popup window can stay on top
        // of the lock screen; the holder keeps its saved state (back stack included) meanwhile.
        // Nothing is read from the database before the first unlock.
        val stateHolder = rememberSaveableStateHolder()

        if (locked) {
            LockScreen(
                autoPrompt = appLock::consumeAutoPrompt,
                onUnlock = { authenticator.authenticate(AuthRequest.UNLOCK, getString(R.string.lock_prompt_title)) },
                onLeave = { moveTaskToBack(true) },
            )
            return
        }
        stateHolder.SaveableStateProvider("app") {
            when (keyStatus) {
                null -> {}
                KeyStatus.AVAILABLE -> AppNavHost(
                    onDisableLock = {
                        authenticator.authenticate(AuthRequest.DISABLE_LOCK, getString(R.string.lock_disable_prompt_title))
                    },
                )
                else -> RecoveryRoute(onRecovered = { keyStatus = KeyStatus.AVAILABLE })
            }
        }
    }
}
