package io.github.igornunes.yourfinances.data.backup

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BackupCryptoTest {
    private val plain = "{\"format\":1}".toByteArray()
    private val password = "correct horse".toCharArray()

    /** Few iterations keep the tests fast; the count is read back from the file. */
    private fun encrypt(data: ByteArray = plain) = BackupCrypto.encrypt(data, password, iterations = 1_000)

    @Test
    fun `round trip with the default iterations`() {
        val file = BackupCrypto.encrypt(plain, password)
        assertArrayEquals(plain, BackupCrypto.decrypt(file, password))
    }

    @Test
    fun `the content is not readable in the file and each file is different`() {
        val first = encrypt()
        val second = encrypt()
        assertFalse(String(first, Charsets.ISO_8859_1).contains("format"))
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `wrong password is rejected`() {
        assertThrows<BackupUnreadableException> { BackupCrypto.decrypt(encrypt(), "wrong horse".toCharArray()) }
    }

    @Test
    fun `any changed byte is rejected, header included`() {
        val file = encrypt()
        for (index in listOf(4, 6, 10, 30, file.size - 1)) {
            val tampered = file.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertThrows<BackupUnreadableException> { BackupCrypto.decrypt(tampered, password) }
        }
    }

    @Test
    fun `other files are rejected`() {
        assertThrows<BackupUnreadableException> { BackupCrypto.decrypt(ByteArray(0), password) }
        assertThrows<BackupUnreadableException> { BackupCrypto.decrypt("date,amount\n".toByteArray(), password) }
        assertThrows<BackupUnreadableException> { BackupCrypto.decrypt(encrypt().copyOf(40), password) }
    }

    @Test
    fun `accented passwords match whatever the keyboard composed`() {
        val composed = "Pa\u00e7oca123".toCharArray()
        val decomposed = "Pac\u0327oca123".toCharArray()
        val file = BackupCrypto.encrypt(plain, composed, iterations = 1_000)
        assertArrayEquals(plain, BackupCrypto.decrypt(file, decomposed))
    }
}
