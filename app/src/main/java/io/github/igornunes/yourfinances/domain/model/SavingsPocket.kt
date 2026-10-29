package io.github.igornunes.yourfinances.domain.model

/**
 * A "caixinha": where money set aside is meant for ("Reserva de emergência", "Viagem").
 * [targetCents] is an optional amount to reach. The app tracks only what was put in and taken
 * out, never yields: those depend on each bank and product.
 */
data class SavingsPocket(
    val id: Long = 0,
    val name: String,
    val targetCents: Long? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(targetCents == null || targetCents > 0) { "targetCents must be positive" }
    }
}
