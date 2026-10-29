package io.github.igornunesfontoura.yourfinances.domain.money

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SplitTest {
    @Test
    fun `exact division gives equal parts`() {
        assertEquals(listOf(5000L, 5000L, 5000L), splitCents(15000, 3))
    }

    @Test
    fun `remainder goes to the first part`() {
        assertEquals(listOf(3334L, 3333L, 3333L), splitCents(10000, 3))
        assertEquals(listOf(17L, 14L, 14L, 14L, 14L, 14L, 14L), splitCents(101, 7))
    }

    @ParameterizedTest
    @CsvSource("10000,3", "1,12", "0,4", "99999,10", "123456789,24", "7,1")
    fun `parts always add up to the total`(total: Long, parts: Int) {
        val split = splitCents(total, parts)
        assertEquals(parts, split.size)
        assertEquals(total, split.sum())
    }

    @Test
    fun `fewer cents than parts leaves zeros after the first`() {
        assertEquals(listOf(2L, 0L, 0L, 0L, 0L), splitCents(2, 5))
    }

    @Test
    fun `rejects invalid input`() {
        assertThrows<IllegalArgumentException> { splitCents(100, 0) }
        assertThrows<IllegalArgumentException> { splitCents(-1, 2) }
    }
}
