package io.github.igornunesfontoura.yourfinances.ui.importing

import io.github.igornunesfontoura.yourfinances.data.repository.ImportRepository
import io.github.igornunesfontoura.yourfinances.domain.importing.CsvMapping
import io.github.igornunesfontoura.yourfinances.domain.importing.DuplicateKind
import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CategoryKind
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.Entry
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import io.github.igornunesfontoura.yourfinances.testing.FakeCategoryRepository
import io.github.igornunesfontoura.yourfinances.testing.FakeCreditCardRepository
import io.github.igornunesfontoura.yourfinances.testing.FakeDocuments
import io.github.igornunesfontoura.yourfinances.testing.FakeSettings
import io.github.igornunesfontoura.yourfinances.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private class FakeImports : ImportRepository {
        val saved = mutableListOf<Entry>()
        var fail = false

        override suspend fun existingKeys(keys: List<String>) = saved.mapNotNull { it.importKey }.filter { it in keys }.toSet()

        override suspend fun entriesOccurredBetween(from: LocalDate, to: LocalDate) =
            saved.filter { !it.occurredOn.isBefore(from) && !it.occurredOn.isAfter(to) }

        override suspend fun importAll(entries: List<Entry>) {
            check(!fail) { "disk full" }
            saved += entries
        }
    }

    private val documents = FakeDocuments()
    private val imports = FakeImports()
    private val settings = FakeSettings()
    private val market = Category(id = 1, name = "Mercado", kind = CategoryKind.EXPENSE)

    private fun viewModel() = ImportViewModel(
        documents,
        imports,
        FakeCreditCardRepository(listOf(CreditCard(id = 3, name = "Nubank", closingDay = 25, dueDay = 5))),
        FakeCategoryRepository(listOf(market)),
        settings,
        UnconfinedTestDispatcher(),
    )

    private val ofx = "<OFX><STMTTRN><DTPOSTED>20260305<TRNAMT>-45,90<FITID>A1<MEMO>Padaria</STMTTRN>" +
        "<STMTTRN><DTPOSTED>20260306<TRNAMT>5000,00<FITID>A2<MEMO>Salário</STMTTRN></OFX>"

    @Test
    fun `ofx goes straight to review, edits are kept and importing twice adds nothing`() = runTest {
        documents.files["content://ofx"] = ofx.toByteArray()
        val viewModel = viewModel()
        viewModel.onFileChosen("content://ofx")
        assertEquals(ImportStage.REVIEW, viewModel.state.value.stage)
        assertEquals(2, viewModel.state.value.selectedCount)

        val padaria = viewModel.state.value.candidates[0].key
        viewModel.onTypeChange(padaria, EntryType.FIXED_EXPENSE)
        viewModel.onCategoryChange(padaria, 1)
        viewModel.onDestinationChange(3)
        // On the card the salary starts unselected; the edited type and category survive.
        assertEquals(1, viewModel.state.value.selectedCount)
        assertEquals(EntryType.FIXED_EXPENSE, viewModel.state.value.candidates[0].type)
        assertEquals(1L, viewModel.state.value.candidates[0].categoryId)

        viewModel.import()
        assertEquals(ImportStage.DONE, viewModel.state.value.stage)
        assertEquals(1, viewModel.state.value.importedCount)
        val saved = imports.saved.single()
        assertEquals(3L, saved.creditCardId)
        assertEquals(1L, saved.categoryId)

        // Into the account this time: the padaria is known, only the salary is new.
        val again = viewModel()
        again.onFileChosen("content://ofx")
        assertEquals(listOf(DuplicateKind.ALREADY_IMPORTED, null), again.state.value.candidates.map { it.duplicate })
        assertEquals(1, again.state.value.selectedCount)
    }

    @Test
    fun `csv asks for the mapping and remembers it`() = runTest {
        documents.files["content://csv"] = "Data;Descrição;Valor\n05/03/2026;Mercado;-123,45".toByteArray()
        val viewModel = viewModel()
        viewModel.onFileChosen("content://csv")
        assertEquals(ImportStage.MAPPING, viewModel.state.value.stage)
        assertEquals(CsvMapping(0, 1, 2), viewModel.state.value.mapping)
        viewModel.confirmMapping()
        assertEquals(ImportStage.REVIEW, viewModel.state.value.stage)
        assertEquals(CsvMapping(0, 1, 2), settings.csvMapping(listOf("Data", "Descrição", "Valor")))
    }

    @Test
    fun `a mapping that reads nothing stays on the mapping step`() = runTest {
        documents.files["content://csv"] = "Data;Descrição;Valor\n05/03/2026;Mercado;-123,45".toByteArray()
        val viewModel = viewModel()
        viewModel.onFileChosen("content://csv")
        viewModel.onMappingChange(CsvMapping(1, 0, 2))
        viewModel.confirmMapping()
        assertEquals(ImportStage.MAPPING, viewModel.state.value.stage)
        assertEquals(ImportProblem.UNMAPPED, viewModel.state.value.problem)
    }

    @Test
    fun `unreadable files and failed saves are reported`() = runTest {
        val viewModel = viewModel()
        viewModel.onFileChosen("content://missing")
        assertEquals(ImportStage.CHOOSE, viewModel.state.value.stage)
        assertEquals(ImportProblem.UNREADABLE, viewModel.state.value.problem)

        documents.files["content://empty"] = "<OFX></OFX>".toByteArray()
        viewModel.onFileChosen("content://empty")
        assertEquals(ImportProblem.NO_TRANSACTIONS, viewModel.state.value.problem)

        documents.files["content://ofx"] = ofx.toByteArray()
        viewModel.onFileChosen("content://ofx")
        imports.fail = true
        viewModel.import()
        assertEquals(ImportStage.REVIEW, viewModel.state.value.stage)
        assertEquals(ImportProblem.FAILED, viewModel.state.value.problem)
        assertTrue(imports.saved.isEmpty())
    }

    @Test
    fun `a selection made by the user survives a destination change`() = runTest {
        documents.files["content://ofx"] = ofx.toByteArray()
        val viewModel = viewModel()
        viewModel.onFileChosen("content://ofx")
        val padaria = viewModel.state.value.candidates[0].key
        viewModel.onToggle(padaria)
        viewModel.onDestinationChange(3)
        assertEquals(false, viewModel.state.value.candidates[0].selected)
        // The salary was not touched, so it follows the card default (unselected).
        assertEquals(false, viewModel.state.value.candidates[1].selected)
        viewModel.onDestinationChange(null)
        assertEquals(listOf(false, true), viewModel.state.value.candidates.map { it.selected })
    }
}
