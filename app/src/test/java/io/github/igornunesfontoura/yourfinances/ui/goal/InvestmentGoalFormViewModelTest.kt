package io.github.igornunesfontoura.yourfinances.ui.goal

import io.github.igornunesfontoura.yourfinances.domain.model.InvestmentGoal
import io.github.igornunesfontoura.yourfinances.testing.FakeInvestmentGoalRepository
import io.github.igornunesfontoura.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class InvestmentGoalFormViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val march = InvestmentGoal(YearMonth.of(2026, 3), 100000)
    private val may = YearMonth.of(2026, 5)

    @Test
    fun `starts from the goal in force and adds a new one from the month, keeping the past`() = runTest {
        val repository = FakeInvestmentGoalRepository(listOf(march))
        val viewModel = InvestmentGoalFormViewModel(may, repository)
        assertEquals("100000", viewModel.state.value.amountDigits)

        viewModel.onAmountChange("150000")
        viewModel.save()

        assertEquals(listOf(march, InvestmentGoal(may, 150000)), repository.goals.value)
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `saving again in the same month replaces that goal`() = runTest {
        val repository = FakeInvestmentGoalRepository(listOf(march))
        val viewModel = InvestmentGoalFormViewModel(YearMonth.of(2026, 3), repository)
        viewModel.onAmountChange("80000")
        viewModel.save()
        assertEquals(listOf(InvestmentGoal(YearMonth.of(2026, 3), 80000)), repository.goals.value)
    }

    @Test
    fun `a later goal bounds the new one and is kept`() = runTest {
        val june = InvestmentGoal(YearMonth.of(2026, 6), 200000)
        val repository = FakeInvestmentGoalRepository(listOf(march, june))
        val viewModel = InvestmentGoalFormViewModel(may, repository)
        assertEquals(YearMonth.of(2026, 6), viewModel.state.value.endsBefore)

        viewModel.onAmountChange("120000")
        viewModel.save()
        assertEquals(listOf(march, InvestmentGoal(may, 120000), june), repository.goals.value)
    }

    @Test
    fun `saving the amount already in force adds nothing`() = runTest {
        val repository = FakeInvestmentGoalRepository(listOf(march))
        val viewModel = InvestmentGoalFormViewModel(may, repository)
        assertNull(viewModel.state.value.endsBefore)
        viewModel.save()
        assertEquals(listOf(march), repository.goals.value)
        assertTrue(viewModel.state.value.status.finished)
    }

    @Test
    fun `empty amount saves a zero goal`() = runTest {
        val repository = FakeInvestmentGoalRepository()
        val viewModel = InvestmentGoalFormViewModel(may, repository)
        assertEquals("", viewModel.state.value.amountDigits)
        viewModel.save()
        assertEquals(listOf(InvestmentGoal(may, 0)), repository.goals.value)
    }

    @Test
    fun `failed save keeps the form open`() = runTest {
        val repository = FakeInvestmentGoalRepository().apply { failWrites = true }
        val viewModel = InvestmentGoalFormViewModel(may, repository)
        viewModel.onAmountChange("100")
        viewModel.save()
        assertTrue(viewModel.state.value.status.failed)
        assertFalse(viewModel.state.value.status.finished)
    }
}
