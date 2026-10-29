package io.github.igornunes.yourfinances.ui.goal

import androidx.lifecycle.ViewModel
import io.github.igornunes.yourfinances.data.repository.InvestmentGoalRepository
import io.github.igornunes.yourfinances.domain.budget.goalFor
import io.github.igornunes.yourfinances.domain.budget.nextGoalAfter
import io.github.igornunes.yourfinances.domain.model.InvestmentGoal
import io.github.igornunes.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunes.yourfinances.domain.money.normalizeAmountDigits
import io.github.igornunes.yourfinances.ui.components.FormStatus
import io.github.igornunes.yourfinances.ui.components.load
import io.github.igornunes.yourfinances.ui.components.persist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.YearMonth

data class InvestmentGoalFormUiState(
    val month: YearMonth,
    val amountDigits: String = "",
    /** When a later goal exists, this one only holds until that month. */
    val endsBefore: YearMonth? = null,
    val status: FormStatus = FormStatus(),
) {
    /** Zero is a valid goal: it stops reserving money from [month] on. */
    val canSave: Boolean get() = !status.isBusy
}

/** Sets the goal in force from [month] on, starting from the goal currently in force there. */
class InvestmentGoalFormViewModel(
    month: YearMonth,
    private val goals: InvestmentGoalRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(InvestmentGoalFormUiState(month))
    val state: StateFlow<InvestmentGoalFormUiState> = _state.asStateFlow()

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    private var inForce: InvestmentGoal? = null

    init {
        load(updateStatus, read = { goals.observeAll().first() }) { list ->
            inForce = list.goalFor(month)
            val cents = inForce?.amountCents ?: 0
            _state.update {
                it.copy(
                    amountDigits = if (cents == 0L) "" else cents.toString(),
                    endsBefore = list.nextGoalAfter(month)?.startsIn,
                )
            }
        }
    }

    fun onAmountChange(input: String) = _state.update { it.copy(amountDigits = normalizeAmountDigits(input)) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val goal = InvestmentGoal(current.month, centsFromAmountDigits(current.amountDigits))
        // The same amount already holds here: a new row would only repeat it.
        val unchanged = inForce?.amountCents == goal.amountCents
        persist(updateStatus) { if (!unchanged) goals.set(goal) }
    }
}
