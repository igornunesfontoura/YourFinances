package io.github.igornunes.yourfinances.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val type: EntryType,
)
