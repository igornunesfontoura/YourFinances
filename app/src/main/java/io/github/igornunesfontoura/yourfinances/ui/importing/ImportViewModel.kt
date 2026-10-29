package io.github.igornunesfontoura.yourfinances.ui.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.files.Documents
import io.github.igornunesfontoura.yourfinances.data.repository.CategoryRepository
import io.github.igornunesfontoura.yourfinances.data.repository.CreditCardRepository
import io.github.igornunesfontoura.yourfinances.data.repository.ImportRepository
import io.github.igornunesfontoura.yourfinances.data.settings.Settings
import io.github.igornunesfontoura.yourfinances.domain.importing.CsvMapping
import io.github.igornunesfontoura.yourfinances.domain.importing.CsvTable
import io.github.igornunesfontoura.yourfinances.domain.importing.ImportCandidate
import io.github.igornunesfontoura.yourfinances.domain.importing.InvalidStatementException
import io.github.igornunesfontoura.yourfinances.domain.importing.StatementLine
import io.github.igornunesfontoura.yourfinances.domain.importing.decodeOfx
import io.github.igornunesfontoura.yourfinances.domain.importing.decodeStatementText
import io.github.igornunesfontoura.yourfinances.domain.importing.guessCsvMapping
import io.github.igornunesfontoura.yourfinances.domain.importing.importKeys
import io.github.igornunesfontoura.yourfinances.domain.importing.looksLikeOfx
import io.github.igornunesfontoura.yourfinances.domain.importing.parseCsv
import io.github.igornunesfontoura.yourfinances.domain.importing.parseOfx
import io.github.igornunesfontoura.yourfinances.domain.importing.planImport
import io.github.igornunesfontoura.yourfinances.domain.importing.readCsv
import io.github.igornunesfontoura.yourfinances.domain.importing.toEntry
import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CreditCard
import io.github.igornunesfontoura.yourfinances.domain.model.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportStage { CHOOSE, WORKING, MAPPING, REVIEW, DONE }

enum class ImportProblem { UNREADABLE, NO_TRANSACTIONS, UNMAPPED, FAILED }

data class ImportUiState(
    val stage: ImportStage = ImportStage.CHOOSE,
    val table: CsvTable? = null,
    val mapping: CsvMapping? = null,
    val skippedRows: Int = 0,
    val candidates: List<ImportCandidate> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
    /** Null: the statement is of an account; otherwise of this card. */
    val cardId: Long? = null,
    val categories: List<Category> = emptyList(),
    val importedCount: Int = 0,
    val problem: ImportProblem? = null,
) {
    val selectedCount: Int get() = candidates.count { it.selected }
    val card: CreditCard? get() = cards.find { it.id == cardId }
}

/** Reads an OFX or CSV statement, lets the user review it, and saves the chosen lines as entries. */
class ImportViewModel(
    private val documents: Documents,
    private val imports: ImportRepository,
    cards: CreditCardRepository,
    categories: CategoryRepository,
    private val settings: Settings,
    private val work: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var lines: List<StatementLine> = emptyList()

    /** Where an error returns to: the last stage the user saw. */
    private var lastStage = ImportStage.CHOOSE
    private var job: Job? = null

    /** Lines the user checked or unchecked: their choice outlives a new plan. */
    private val toggled = mutableMapOf<String, Boolean>()

    init {
        viewModelScope.launch { cards.observeAll().collect { list -> _state.update { it.copy(cards = list) } } }
        viewModelScope.launch {
            categories.observeAll().collect { list ->
                // A category deleted during the review cannot be saved into.
                val ids = list.map { it.id }.toSet()
                _state.update { state ->
                    state.copy(
                        categories = list,
                        candidates = state.candidates.map { if (it.categoryId != null && it.categoryId !in ids) it.copy(categoryId = null) else it },
                    )
                }
            }
        }
    }

    fun onFileChosen(uri: String) = launchWork {
        val bytes = documents.read(uri)
        if (looksLikeOfx(bytes)) {
            lines = withContext(work) { parseOfx(decodeOfx(bytes)) }
            review(skippedRows = 0)
        } else {
            val table = withContext(work) { parseCsv(decodeStatementText(bytes)) }
            if (table.rows.isEmpty()) throw InvalidStatementException("empty")
            val known = settings.csvMapping(table.header) ?: guessCsvMapping(table)
            // Unrecognized columns: start from the first three and let the user pick.
            val last = table.header.lastIndex
            val mapping = known ?: CsvMapping(0, minOf(1, last), minOf(2, last))
            _state.update {
                it.copy(
                    stage = ImportStage.MAPPING,
                    table = table,
                    mapping = mapping,
                    problem = if (known == null) ImportProblem.UNMAPPED else null,
                )
            }
        }
    }

    fun onMappingChange(mapping: CsvMapping) = _state.update { it.copy(mapping = mapping, problem = null) }

    fun confirmMapping() {
        val table = _state.value.table ?: return
        val mapping = _state.value.mapping ?: return
        launchWork {
            val reading = withContext(work) { readCsv(table, mapping) }
            if (reading.lines.isEmpty()) {
                _state.update { it.copy(stage = ImportStage.MAPPING, problem = ImportProblem.UNMAPPED) }
                return@launchWork
            }
            settings.saveCsvMapping(table.header, mapping)
            lines = reading.lines
            review(reading.skippedRows)
        }
    }

    /** Plans again for the new destination, keeping the user's choices for each line. */
    fun onDestinationChange(cardId: Long?) {
        _state.update { it.copy(cardId = cardId) }
        launchWork { review(_state.value.skippedRows) }
    }

    fun onToggle(key: String) = updateCandidate(key) {
        toggled[key] = !it.selected
        it.copy(selected = !it.selected)
    }

    fun onTypeChange(key: String, type: EntryType) = updateCandidate(key) {
        it.copy(type = type, categoryId = it.categoryId.takeIf { _ -> type.categoryKind == it.type.categoryKind })
    }

    fun onCategoryChange(key: String, categoryId: Long?) = updateCandidate(key) { it.copy(categoryId = categoryId) }

    fun import() {
        val current = _state.value
        if (current.stage != ImportStage.REVIEW || current.selectedCount == 0) return
        launchWork {
            val entries = withContext(work) { current.candidates.filter { it.selected }.map { it.toEntry(current.card) } }
            imports.importAll(entries)
            _state.update { it.copy(stage = ImportStage.DONE, importedCount = entries.size) }
        }
    }

    fun dismissProblem() = _state.update { it.copy(problem = null) }

    private suspend fun review(skippedRows: Int) {
        val keys = withContext(work) { importKeys(lines) }
        val existingKeys = imports.existingKeys(keys)
        val existing = if (lines.isEmpty()) emptyList() else imports.entriesOccurredBetween(lines.minOf { it.date }, lines.maxOf { it.date })
        val previous = _state.value.candidates.associateBy { it.key }
        val planned = withContext(work) { planImport(lines, existingKeys, existing, _state.value.card) }
            .map { candidate ->
                val edited = previous[candidate.key]?.let { candidate.copy(type = it.type, categoryId = it.categoryId) } ?: candidate
                edited.copy(selected = toggled[candidate.key] ?: candidate.selected)
            }
        _state.update { it.copy(stage = ImportStage.REVIEW, candidates = planned, skippedRows = skippedRows) }
    }

    private fun updateCandidate(key: String, change: (ImportCandidate) -> ImportCandidate) = _state.update { state ->
        state.copy(candidates = state.candidates.map { if (it.key == key) change(it) else it })
    }

    /**
     * One job at a time: a new one (a second destination tap) replaces the running one. Errors go
     * back to where the user was; nothing about the file's content is logged.
     */
    private fun launchWork(block: suspend () -> Unit) {
        _state.value.stage.takeIf { it != ImportStage.WORKING }?.let { lastStage = it }
        val back = lastStage
        job?.cancel()
        _state.update { it.copy(stage = ImportStage.WORKING, problem = null) }
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidStatementException) {
                _state.update { it.copy(stage = back, problem = ImportProblem.NO_TRANSACTIONS) }
            } catch (e: java.io.IOException) {
                _state.update { it.copy(stage = back, problem = ImportProblem.UNREADABLE) }
            } catch (e: Exception) {
                _state.update { it.copy(stage = back, problem = ImportProblem.FAILED) }
            } catch (e: OutOfMemoryError) {
                // A file too large for this phone: nothing was saved, the app keeps running.
                _state.update { it.copy(stage = back, problem = ImportProblem.FAILED) }
            }
        }
    }
}
