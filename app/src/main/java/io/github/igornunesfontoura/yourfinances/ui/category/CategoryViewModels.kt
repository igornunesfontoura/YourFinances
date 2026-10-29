package io.github.igornunesfontoura.yourfinances.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunesfontoura.yourfinances.data.repository.CategoryRepository
import io.github.igornunesfontoura.yourfinances.domain.model.Category
import io.github.igornunesfontoura.yourfinances.domain.model.CategoryKind
import io.github.igornunesfontoura.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunesfontoura.yourfinances.domain.money.normalizeAmountDigits
import io.github.igornunesfontoura.yourfinances.ui.components.FormStatus
import io.github.igornunesfontoura.yourfinances.ui.components.load
import io.github.igornunesfontoura.yourfinances.ui.components.persist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CategoryListUiState(
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
    val loading: Boolean = true,
)

class CategoryListViewModel(categories: CategoryRepository) : ViewModel() {
    val state: StateFlow<CategoryListUiState> = categories.observeAll()
        .map { list ->
            CategoryListUiState(
                expense = list.filter { it.kind == CategoryKind.EXPENSE },
                income = list.filter { it.kind == CategoryKind.INCOME },
                loading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryListUiState())
}

data class CategoryFormUiState(
    val isEditing: Boolean,
    val name: String = "",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    /** Empty means no limit; only expense categories have one. */
    val limitDigits: String = "",
    val status: FormStatus = FormStatus(),
) {
    val limitCents: Long? get() = centsFromAmountDigits(limitDigits).takeIf { it > 0 && kind == CategoryKind.EXPENSE }
    val canSave: Boolean get() = !status.isBusy && name.isNotBlank()
}

/** Creates a category when [categoryId] is null, otherwise edits it. */
class CategoryFormViewModel(categoryId: Long?, private val categories: CategoryRepository) : ViewModel() {
    private var original: Category? = null

    private val _state = MutableStateFlow(CategoryFormUiState(isEditing = categoryId != null))
    val state: StateFlow<CategoryFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    init {
        if (categoryId != null) {
            load(updateStatus, read = { categories.getById(categoryId) }) { category ->
                original = category
                _state.update {
                    it.copy(name = category.name, kind = category.kind, limitDigits = category.monthlyLimitCents?.toString().orEmpty())
                }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }

    /** Only for new categories: an existing one keeps its kind. */
    fun onKindChange(kind: CategoryKind) = _state.update { if (it.isEditing) it else it.copy(kind = kind) }

    fun onLimitChange(input: String) = _state.update { it.copy(limitDigits = normalizeAmountDigits(input)) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val category = Category(
            id = original?.id ?: 0,
            name = current.name.trim(),
            kind = current.kind,
            monthlyLimitCents = current.limitCents,
        )
        persist(updateStatus) { if (original == null) categories.add(category) else categories.update(category) }
    }

    fun delete() {
        val existing = original ?: return
        if (_state.value.status.isBusy) return
        persist(updateStatus) { categories.delete(existing) }
    }
}
