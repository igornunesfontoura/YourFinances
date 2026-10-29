package io.github.igornunesfontoura.yourfinances.ui.category

import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CategoryKind
import io.github.igornunesfontoura.yourfinances.testing.FakeCategoryRepository
import io.github.igornunesfontoura.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelsTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val market = Category(id = 1, name = "Mercado", kind = CategoryKind.EXPENSE, monthlyLimitCents = 80000)
    private val salary = Category(id = 2, name = "Salário", kind = CategoryKind.INCOME)

    @Test
    fun `list splits expense and income categories`() = runTest {
        val viewModel = CategoryListViewModel(FakeCategoryRepository(listOf(salary, market)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertEquals(listOf(market), viewModel.state.value.expense)
        assertEquals(listOf(salary), viewModel.state.value.income)
    }

    @Test
    fun `form creates with a limit, income drops it`() = runTest {
        val repository = FakeCategoryRepository()
        val viewModel = CategoryFormViewModel(null, repository)
        assertFalse(viewModel.state.value.canSave)
        viewModel.onNameChange(" Pets ")
        viewModel.onLimitChange("20000")
        viewModel.save()
        assertEquals(listOf(Category(id = 1, name = "Pets", kind = CategoryKind.EXPENSE, monthlyLimitCents = 20000)), repository.categories.value)

        val income = CategoryFormViewModel(null, repository)
        income.onNameChange("Freela")
        income.onLimitChange("20000")
        income.onKindChange(CategoryKind.INCOME)
        income.save()
        assertEquals(null, repository.categories.value.last().monthlyLimitCents)
    }

    @Test
    fun `editing and deleting`() = runTest {
        val repository = FakeCategoryRepository(listOf(market))
        val viewModel = CategoryFormViewModel(1, repository)
        assertEquals("80000", viewModel.state.value.limitDigits)
        viewModel.onLimitChange("")
        viewModel.save()
        assertEquals(null, repository.categories.value.single().monthlyLimitCents)

        val deleting = CategoryFormViewModel(1, repository)
        deleting.delete()
        assertTrue(deleting.state.value.status.finished)
        assertTrue(repository.categories.value.isEmpty())
    }
}
