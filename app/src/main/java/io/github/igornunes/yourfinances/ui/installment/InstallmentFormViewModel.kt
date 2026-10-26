package io.github.igornunes.yourfinances.ui.installment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.igornunes.yourfinances.data.repository.CreditCardRepository
import io.github.igornunes.yourfinances.data.repository.InstallmentRepository
import io.github.igornunes.yourfinances.domain.card.dueDatesFrom
import io.github.igornunes.yourfinances.domain.installment.MAX_INSTALLMENTS
import io.github.igornunes.yourfinances.domain.installment.defaultFirstPayment
import io.github.igornunes.yourfinances.domain.installment.monthlyPaymentDates
import io.github.igornunes.yourfinances.domain.model.CreditCard
import io.github.igornunes.yourfinances.domain.model.InstallmentPurchase
import io.github.igornunes.yourfinances.domain.money.centsFromAmountDigits
import io.github.igornunes.yourfinances.domain.money.normalizeAmountDigits
import io.github.igornunes.yourfinances.domain.money.splitCents
import io.github.igornunes.yourfinances.ui.components.FormStatus
import io.github.igornunes.yourfinances.ui.components.persist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class InstallmentFormUiState(
    val today: LocalDate,
    val totalDigits: String = "",
    val countText: String = "",
    val description: String = "",
    val purchasedOn: LocalDate = today,
    val firstPaymentOn: LocalDate = defaultFirstPayment(today),
    /** While false, the first payment follows the purchase date. */
    val firstPaymentEdited: Boolean = false,
    val cards: List<CreditCard> = emptyList(),
    val cardId: Long? = null,
    val status: FormStatus = FormStatus(),
) {
    val card: CreditCard? get() = cards.find { it.id == cardId }

    /** On a card the dates come from its invoices; otherwise monthly from the first payment. */
    val paymentDates: List<LocalDate>? get() = count?.let { count ->
        card?.dueDatesFrom(purchasedOn, count) ?: monthlyPaymentDates(firstPaymentOn, count)
    }

    val totalCents: Long get() = centsFromAmountDigits(totalDigits)
    val count: Int? get() = countText.toIntOrNull()?.takeIf { it in 2..MAX_INSTALLMENTS }
    val firstBeforePurchase: Boolean get() = card == null && firstPaymentOn.isBefore(purchasedOn)

    /** First installment and the others, when they differ by the remainder cents. */
    val preview: List<Long>? get() = count?.takeIf { totalCents > 0 }?.let { splitCents(totalCents, it) }

    val canSave: Boolean get() = !status.isBusy && preview != null && !firstBeforePurchase
}

class InstallmentFormViewModel(
    private val installments: InstallmentRepository,
    cards: CreditCardRepository,
    clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(InstallmentFormUiState(today = LocalDate.now(clock)))
    val state: StateFlow<InstallmentFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { cards.observeAll().collect { list -> _state.update { it.copy(cards = list) } } }
    }

    private val updateStatus: ((FormStatus) -> FormStatus) -> Unit = { change ->
        _state.update { it.copy(status = change(it.status)) }
    }

    fun onTotalChange(input: String) = _state.update { it.copy(totalDigits = normalizeAmountDigits(input)) }

    fun onCountChange(count: String) = _state.update { it.copy(countText = count) }

    fun onDescriptionChange(description: String) = _state.update { it.copy(description = description) }

    fun onPurchaseDateChange(date: LocalDate) = _state.update {
        it.copy(
            purchasedOn = date,
            firstPaymentOn = if (it.firstPaymentEdited) it.firstPaymentOn else defaultFirstPayment(date),
        )
    }

    fun onCardChange(cardId: Long?) = _state.update { it.copy(cardId = cardId) }

    fun onFirstPaymentChange(date: LocalDate) = _state.update { it.copy(firstPaymentOn = date, firstPaymentEdited = true) }

    fun save() {
        val current = _state.value
        val count = current.count
        if (!current.canSave || count == null) return
        val card = current.card
        val purchase = InstallmentPurchase(
            description = current.description.trim(),
            totalCents = current.totalCents,
            installmentCount = count,
            purchasedOn = current.purchasedOn,
            creditCardId = card?.id,
        )
        persist(updateStatus) {
            if (card != null) {
                installments.addOnCard(purchase, card)
            } else {
                installments.add(purchase, monthlyPaymentDates(current.firstPaymentOn, count))
            }
        }
    }
}
