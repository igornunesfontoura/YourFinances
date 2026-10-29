package io.github.igornunes.yourfinances.ui.savings

import io.github.igornunes.yourfinances.domain.model.Entry
import io.github.igornunes.yourfinances.domain.model.EntryType
import io.github.igornunes.yourfinances.domain.model.SavingsPocket
import io.github.igornunes.yourfinances.testing.FakeSavingsRepository
import io.github.igornunes.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModelsTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val reserve = SavingsPocket(id = 1, name = "Reserva")
    private val entries = MutableStateFlow(
        listOf(
            Entry(
                id = 1, type = EntryType.INVESTMENT, amountCents = 100000, description = "", occurredOn = today,
                paidOn = today, savingsPocketId = 1,
            ),
        ),
    )

    @Test
    fun `summary follows the movements`() = runTest {
        val viewModel = SavingsViewModel(FakeSavingsRepository(listOf(reserve), entries), clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertFalse(viewModel.state.value.loading)
        assertEquals(100000L, viewModel.state.value.summary.totalCents)
        assertEquals(100000L, viewModel.state.value.summary.pockets.single().savedCents)
    }

    @Test
    fun `form creates a pocket with an optional target`() = runTest {
        val repository = FakeSavingsRepository()
        val viewModel = PocketFormViewModel(null, repository)
        assertFalse(viewModel.state.value.canSave)
        viewModel.onNameChange(" Viagem ")
        viewModel.onTargetChange("500000")
        viewModel.save()
        assertEquals(listOf(SavingsPocket(id = 1, name = "Viagem", targetCents = 500000)), repository.pockets.value)

        val noTarget = PocketFormViewModel(null, repository)
        noTarget.onNameChange("Reserva")
        noTarget.save()
        assertEquals(null, repository.pockets.value.last().targetCents)
    }

    @Test
    fun `a pocket with movements is kept, an empty one is deleted`() = runTest {
        val repository = FakeSavingsRepository(listOf(reserve, SavingsPocket(id = 2, name = "Vazia")), entries)
        val used = PocketFormViewModel(1, repository)
        assertEquals("Reserva", used.state.value.name)
        used.delete()
        assertTrue(used.state.value.deleteBlocked)
        assertEquals(2, repository.pockets.value.size)

        val empty = PocketFormViewModel(2, repository)
        empty.delete()
        assertTrue(empty.state.value.status.finished)
        assertEquals(listOf(reserve), repository.pockets.value)
    }
}
