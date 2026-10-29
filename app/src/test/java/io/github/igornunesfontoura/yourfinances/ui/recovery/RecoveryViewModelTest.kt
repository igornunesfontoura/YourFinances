package io.github.igornunesfontoura.yourfinances.ui.recovery

import io.github.igornunesfontoura.yourfinances.data.security.KeyStatus
import io.github.igornunesfontoura.yourfinances.testing.FakeBackupService
import io.github.igornunesfontoura.yourfinances.testing.FakeDatabaseRecovery
import io.github.igornunesfontoura.yourfinances.testing.FakeDocuments
import io.github.igornunesfontoura.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val backups = FakeBackupService()
    private val recovery = FakeDatabaseRecovery(backups.events)
    private val documents = FakeDocuments().apply { files["content://backup"] = "backup:segredo123".toByteArray() }

    private fun viewModel() = RecoveryViewModel(recovery, backups, documents, UnconfinedTestDispatcher())

    @Test
    fun `a lost key offers restore and start over`() = runTest {
        val state = viewModel().state.value
        assertTrue(state.keyLost)
        assertFalse(state.busy)
        assertNull(state.problem)
    }

    @Test
    fun `a temporary failure is retried first, then restoring is offered`() = runTest {
        recovery.status = KeyStatus.TEMPORARILY_UNAVAILABLE
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.keyLost)
        assertFalse(viewModel.state.value.canRestore)

        viewModel.retry()
        assertFalse(viewModel.state.value.canRestore)
        viewModel.retry()
        assertEquals(RecoveryProblem.STILL_UNAVAILABLE, viewModel.state.value.problem)
        assertTrue(viewModel.state.value.canRestore)
        assertFalse(viewModel.state.value.keyLost)

        viewModel.restore("content://backup", "segredo123".toCharArray())
        assertTrue(viewModel.state.value.recovered)
        assertEquals(listOf("erase", "restore"), backups.events)
    }

    @Test
    fun `a key that comes back recovers without erasing`() = runTest {
        recovery.status = KeyStatus.TEMPORARILY_UNAVAILABLE
        val viewModel = viewModel()
        recovery.status = KeyStatus.AVAILABLE
        viewModel.retry()
        assertTrue(viewModel.state.value.recovered)
        assertEquals(emptyList<String>(), backups.events)
    }

    @Test
    fun `a wrong password erases nothing`() = runTest {
        val viewModel = viewModel()
        val password = "errada".toCharArray()
        viewModel.restore("content://backup", password)
        assertEquals(RecoveryProblem.WRONG_PASSWORD, viewModel.state.value.problem)
        assertFalse(viewModel.state.value.recovered)
        assertEquals(emptyList<String>(), backups.events)
        assertTrue(password.all { it == '\u0000' })
    }

    @Test
    fun `restoring erases the unreadable database first`() = runTest {
        val viewModel = viewModel()
        viewModel.restore("content://backup", "segredo123".toCharArray())
        assertTrue(viewModel.state.value.recovered)
        assertEquals(listOf("erase", "restore"), backups.events)
    }

    @Test
    fun `a restore failing after the erase says so and can be tried again`() = runTest {
        val viewModel = viewModel()
        backups.failRestore = true
        viewModel.restore("content://backup", "segredo123".toCharArray())
        assertEquals(RecoveryProblem.RESTORE_FAILED_AFTER_ERASE, viewModel.state.value.problem)

        backups.failRestore = false
        viewModel.restore("content://backup", "segredo123".toCharArray())
        assertTrue(viewModel.state.value.recovered)
        assertEquals(listOf("erase", "restore"), backups.events)
    }

    @Test
    fun `starting over only erases`() = runTest {
        val viewModel = viewModel()
        viewModel.startOver()
        assertTrue(viewModel.state.value.recovered)
        assertEquals(listOf("erase"), backups.events)
    }
}
