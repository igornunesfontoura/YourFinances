package io.github.igornunesfontoura.yourfinances.ui.recovery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.ConfirmDialog
import io.github.igornunesfontoura.yourfinances.ui.components.ErrorText
import io.github.igornunesfontoura.yourfinances.ui.components.HintText
import io.github.igornunesfontoura.yourfinances.ui.components.RestorePasswordDialog
import org.koin.androidx.compose.koinViewModel

/** Shown instead of the app while the database key cannot be read. */
@Composable
fun RecoveryRoute(onRecovered: () -> Unit, viewModel: RecoveryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnRecovered by rememberUpdatedState(onRecovered)
    LaunchedEffect(state.recovered) { if (state.recovered) currentOnRecovered() }

    var restoreUri by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmStartOver by rememberSaveable { mutableStateOf(false) }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri?.toString()
    }
    val enabled = !state.busy

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(if (state.keyLost) R.string.recovery_title else R.string.recovery_temporary_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(if (state.keyLost) R.string.recovery_message else R.string.recovery_temporary_message))
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.problem?.let { ErrorText(it.text) }
            if (state.canRestore) {
                Button(onClick = { pickBackup.launch(arrayOf("*/*")) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recovery_restore))
                }
            }
            OutlinedButton(onClick = viewModel::retry, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recovery_retry))
            }
            if (state.keyLost) {
                HintText(R.string.recovery_start_over_hint)
                TextButton(onClick = { confirmStartOver = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recovery_start_over), color = MaterialTheme.colorScheme.error)
                }
            }
        }
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
    if (confirmStartOver) {
        ConfirmDialog(
            title = R.string.recovery_start_over_confirm_title,
            message = R.string.recovery_start_over_confirm_message,
            confirmLabel = R.string.recovery_start_over,
            onConfirm = {
                confirmStartOver = false
                viewModel.startOver()
            },
            onDismiss = { confirmStartOver = false },
        )
    }
}

private val RecoveryProblem.text: Int
    get() = when (this) {
        RecoveryProblem.WRONG_PASSWORD -> R.string.restore_wrong_password
        RecoveryProblem.INVALID_BACKUP -> R.string.restore_invalid
        RecoveryProblem.RESTORE_FAILED -> R.string.restore_failed
        RecoveryProblem.RESTORE_FAILED_AFTER_ERASE -> R.string.recovery_restore_failed_after_erase
        RecoveryProblem.STILL_UNAVAILABLE -> R.string.recovery_still_unavailable
        RecoveryProblem.FAILED -> R.string.recovery_failed
    }
