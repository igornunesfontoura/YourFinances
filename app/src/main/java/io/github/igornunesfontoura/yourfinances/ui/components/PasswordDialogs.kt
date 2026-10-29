package io.github.igornunesfontoura.yourfinances.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.settings.MIN_BACKUP_PASSWORD_LENGTH
import io.github.igornunesfontoura.yourfinances.ui.settings.PasswordProblem
import io.github.igornunesfontoura.yourfinances.ui.settings.backupPasswordProblem

// Passwords live in plain `remember`, never `rememberSaveable`: they must not reach the saved
// instance state bundle.

/** Chooses the password of a new backup, typed twice. */
@Composable
fun NewBackupPasswordDialog(onConfirm: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showProblem by remember { mutableStateOf(false) }
    val problem = backupPasswordProblem(password, confirmation)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HintText(R.string.backup_password_hint)
                PasswordField(password, { password = it }, R.string.backup_password)
                PasswordField(confirmation, { confirmation = it }, R.string.backup_password_confirm)
                if (showProblem) {
                    when (problem) {
                        PasswordProblem.TOO_SHORT ->
                            ErrorText(stringResource(R.string.backup_password_too_short, MIN_BACKUP_PASSWORD_LENGTH))
                        PasswordProblem.MISMATCH -> ErrorText(R.string.backup_password_mismatch)
                        null -> {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (problem == null) onConfirm(password.toCharArray()) else showProblem = true
            }) { Text(stringResource(R.string.action_continue)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Asks the password of an existing backup before it replaces all data. */
@Composable
fun RestorePasswordDialog(onConfirm: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.restore_warning))
                PasswordField(password, { password = it }, R.string.backup_password)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.toCharArray()) }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.restore_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}
