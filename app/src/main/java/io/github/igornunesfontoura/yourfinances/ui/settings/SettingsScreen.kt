package io.github.igornunesfontoura.yourfinances.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.AppCard
import io.github.igornunesfontoura.yourfinances.ui.components.CardListItem
import io.github.igornunesfontoura.yourfinances.ui.components.IconBadge
import io.github.igornunesfontoura.yourfinances.ui.components.ListScaffold
import io.github.igornunesfontoura.yourfinances.ui.components.NewBackupPasswordDialog
import io.github.igornunesfontoura.yourfinances.ui.components.RestorePasswordDialog
import io.github.igornunesfontoura.yourfinances.ui.components.SectionHeader
import io.github.igornunesfontoura.yourfinances.ui.components.cardGroup
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onDisableLock: () -> Unit,
    onOpenImport: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(context.getString(it.text))
            viewModel.dismissMessage()
        }
    }

    var askBackupPassword by rememberSaveable { mutableStateOf(false) }
    var restoreUri by rememberSaveable { mutableStateOf<String?>(null) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        viewModel.onBackupDestination(uri?.toString())
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri?.toString()
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri.toString())
    }
    val enabled = !state.busy
    // Turning the lock off takes the device credential; with no screen lock there is none to ask.
    val setLock: (Boolean) -> Unit = { on ->
        if (!on && state.deviceSecure) onDisableLock() else viewModel.setLockEnabled(on)
    }

    ListScaffold(title = R.string.settings_title, onBack = onBack, snackbarHost = { SnackbarHost(snackbar) }) {
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            Column {
                SectionHeader(stringResource(R.string.settings_security))
                AppCard(Modifier.fillMaxWidth()) {
                    CardListItem(
                        headline = stringResource(R.string.settings_lock),
                        supporting = {
                            Text(stringResource(if (state.deviceSecure) R.string.settings_lock_hint else R.string.settings_lock_no_device_lock))
                        },
                        leading = { IconBadge(Icons.Outlined.Fingerprint, MaterialTheme.colorScheme.primary) },
                        trailing = { Switch(checked = state.lockEnabled, onCheckedChange = setLock) },
                        onClick = { setLock(!state.lockEnabled) },
                    )
                }
            }
        }
        cardGroup(
            "data",
            listOf(
                SettingsAction(R.string.settings_backup, R.string.settings_backup_hint, Icons.Outlined.Lock) { askBackupPassword = true },
                SettingsAction(R.string.settings_restore, R.string.settings_restore_hint, Icons.Outlined.Restore) { pickBackup.launch(arrayOf("*/*")) },
                SettingsAction(R.string.settings_import, R.string.settings_import_hint, Icons.Outlined.FileUpload, onOpenImport),
                SettingsAction(R.string.settings_export_csv, R.string.settings_export_csv_hint, Icons.Outlined.FileDownload) {
                    exportCsv.launch(viewModel.csvFileName())
                },
            ),
            header = R.string.settings_data,
        ) { action ->
            CardListItem(
                headline = stringResource(action.title),
                supporting = { Text(stringResource(action.hint)) },
                leading = { IconBadge(action.icon, MaterialTheme.colorScheme.primary) },
                onClick = action.onClick,
                enabled = enabled,
            )
        }
    }

    if (askBackupPassword) {
        NewBackupPasswordDialog(
            onConfirm = { password ->
                askBackupPassword = false
                viewModel.setBackupPassword(password)
                createBackup.launch(viewModel.backupFileName())
            },
            onDismiss = { askBackupPassword = false },
        )
    }
    restoreUri?.let { uri ->
        RestorePasswordDialog(
            onConfirm = { password ->
                restoreUri = null
                viewModel.restore(uri, password)
            },
            onDismiss = { restoreUri = null },
        )
    }
}

private class SettingsAction(
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

private val SettingsMessage.text: Int
    get() = when (this) {
        SettingsMessage.BACKUP_SAVED -> R.string.backup_saved
        SettingsMessage.BACKUP_FAILED -> R.string.backup_failed
        SettingsMessage.RESTORED -> R.string.restore_done
        SettingsMessage.WRONG_PASSWORD -> R.string.restore_wrong_password
        SettingsMessage.INVALID_BACKUP -> R.string.restore_invalid
        SettingsMessage.RESTORE_FAILED -> R.string.restore_failed
        SettingsMessage.CSV_SAVED -> R.string.csv_saved
        SettingsMessage.CSV_FAILED -> R.string.csv_failed
    }
