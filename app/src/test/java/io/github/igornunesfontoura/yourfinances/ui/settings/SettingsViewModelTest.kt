package io.github.igornunesfontoura.yourfinances.ui.settings

import io.github.igornunesfontoura.yourfinances.testing.FakeBackupService
import io.github.igornunesfontoura.yourfinances.testing.FakeDocuments
import io.github.igornunesfontoura.yourfinances.testing.FakeSettings
import io.github.igornunesfontoura.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = Clock.fixed(LocalDate.of(2026, 10, 28).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val settings = FakeSettings()
    private val backups = FakeBackupService()
    private val documents = FakeDocuments()

    private fun TestScope.subscribed(deviceSecure: Boolean = true): SettingsViewModel {
        val viewModel = SettingsViewModel(settings, { deviceSecure }, backups, documents, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun `lock toggle and device lock state`() = runTest {
        val viewModel = subscribed(deviceSecure = false)
        assertFalse(viewModel.state.value.deviceSecure)
        viewModel.setLockEnabled(false)
        assertFalse(viewModel.state.value.lockEnabled)
        assertFalse(settings.lockEnabled.value)
    }

    @Test
    fun `backup is written to the chosen file with dated names`() = runTest {
        val viewModel = subscribed()
        assertEquals("yourfinances-2026-10-28.yfbackup", viewModel.backupFileName())
        assertEquals("yourfinances-lancamentos-2026-10-28.csv", viewModel.csvFileName())

        val password = "segredo123".toCharArray()
        viewModel.setBackupPassword(password)
        viewModel.onBackupDestination("content://backup")
        assertEquals("backup:segredo123", documents.files.getValue("content://backup").toString(Charsets.UTF_8))
        assertTrue(password.all { it == '\u0000' })
        assertEquals(SettingsMessage.BACKUP_SAVED, viewModel.state.value.message)
        assertFalse(viewModel.state.value.busy)

        viewModel.dismissMessage()
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `failed writes are reported`() = runTest {
        val viewModel = subscribed()
        documents.failWrites = true
        viewModel.setBackupPassword("segredo123".toCharArray())
        viewModel.onBackupDestination("content://backup")
        assertEquals(SettingsMessage.BACKUP_FAILED, viewModel.state.value.message)
        viewModel.exportCsv("content://csv")
        assertEquals(SettingsMessage.CSV_FAILED, viewModel.state.value.message)
    }

    @Test
    fun `a password lost while the picker was open fails and removes the empty file`() = runTest {
        val viewModel = subscribed()
        documents.files["content://backup"] = ByteArray(0)
        viewModel.onBackupDestination("content://backup")
        assertEquals(SettingsMessage.BACKUP_FAILED, viewModel.state.value.message)
        assertFalse(documents.files.containsKey("content://backup"))
    }

    @Test
    fun `leaving the picker writes nothing`() = runTest {
        val viewModel = subscribed()
        viewModel.setBackupPassword("segredo123".toCharArray())
        viewModel.onBackupDestination(null)
        assertNull(viewModel.state.value.message)
        assertTrue(documents.files.isEmpty())
    }

    @Test
    fun `csv export`() = runTest {
        val viewModel = subscribed()
        viewModel.exportCsv("content://csv")
        assertEquals("occurred_on\r\n", documents.files.getValue("content://csv").toString(Charsets.UTF_8))
        assertEquals(SettingsMessage.CSV_SAVED, viewModel.state.value.message)
    }

    @Test
    fun `restore outcomes`() = runTest {
        val viewModel = subscribed()
        documents.files["content://old"] = "backup:segredo123".toByteArray()

        viewModel.restore("content://old", "errada".toCharArray())
        assertEquals(SettingsMessage.WRONG_PASSWORD, viewModel.state.value.message)
        assertEquals(emptyList<String>(), backups.events)

        viewModel.restore("content://old", "segredo123".toCharArray())
        assertEquals(SettingsMessage.RESTORED, viewModel.state.value.message)
        assertEquals(listOf("restore"), backups.events)

        backups.invalidContent = true
        viewModel.restore("content://old", "segredo123".toCharArray())
        assertEquals(SettingsMessage.INVALID_BACKUP, viewModel.state.value.message)

        backups.invalidContent = false
        backups.failRestore = true
        viewModel.restore("content://old", "segredo123".toCharArray())
        assertEquals(SettingsMessage.RESTORE_FAILED, viewModel.state.value.message)

        viewModel.restore("content://missing", "segredo123".toCharArray())
        assertEquals(SettingsMessage.RESTORE_FAILED, viewModel.state.value.message)
    }

    @Test
    fun `backup passwords need eight characters typed twice`() {
        assertEquals(PasswordProblem.TOO_SHORT, backupPasswordProblem("1234567", "1234567"))
        assertEquals(PasswordProblem.MISMATCH, backupPasswordProblem("12345678", "12345679"))
        assertNull(backupPasswordProblem("12345678", "12345678"))
    }
}
