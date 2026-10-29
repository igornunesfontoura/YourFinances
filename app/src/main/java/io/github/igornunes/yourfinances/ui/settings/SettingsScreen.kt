package io.github.igornunes.yourfinances.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunes.yourfinances.R
import io.github.igornunes.yourfinances.ui.components.NewBackupPasswordDialog
import io.github.igornunes.yourfinances.ui.components.RestorePasswordDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(onBack: () -> Unit, onDisableLock: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            SectionTitle(R.string.settings_security)
            ListItem(
                modifier = Modifier.clickable { setLock(!state.lockEnabled) },
                headlineContent = { Text(stringResource(R.string.settings_lock)) },
                supportingContent = {
                    Text(
                        stringResource(if (state.deviceSecure) R.string.settings_lock_hint else R.string.settings_lock_no_device_lock),
                    )
                },
                trailingContent = { Switch(checked = state.lockEnabled, onCheckedChange = setLock) },
            )
            HorizontalDivider()
            SectionTitle(R.string.settings_data)
            ActionItem(R.string.settings_backup, R.string.settings_backup_hint, enabled) { askBackupPassword = true }
            ActionItem(R.string.settings_restore, R.string.settings_restore_hint, enabled) { pickBackup.launch(arrayOf("*/*")) }
            ActionItem(R.string.settings_export_csv, R.string.settings_export_csv_hint, enabled) {
                exportCsv.launch(viewModel.csvFileName())
            }
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

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ActionItem(@StringRes title: Int, @StringRes hint: Int, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(hint)) },
    )
}

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
