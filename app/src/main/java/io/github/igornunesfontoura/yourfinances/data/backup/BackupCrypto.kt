package io.github.igornunesfontoura.yourfinances.data.backup

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based encryption of backup files: AES-256-GCM with a key derived by
 * PBKDF2-HMAC-SHA256. Only the JDK/Android crypto providers are used.
 *
 * File layout: `"YFBK" | version (1) | iterations (4) | salt (16) | iv (12) | ciphertext + tag`.
 * The whole header is authenticated as associated data, so it cannot be altered either.
 */
object BackupCrypto {
    /** OWASP's recommendation for PBKDF2-HMAC-SHA256. Stored in the file, so it can grow later. */
    const val DEFAULT_ITERATIONS = 600_000

    private val MAGIC = "YFBK".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + IV_BYTES

    /** Upper bound accepted when reading, so a crafted file cannot stall the phone for hours. */
    private const val MAX_ITERATIONS = 10_000_000

    fun encrypt(plain: ByteArray, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        require(iterations in 1..MAX_ITERATIONS) { "iterations out of range" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(VERSION).putInt(iterations).put(salt).put(iv)
            .array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    /** @throws BackupUnreadableException for a wrong password, a damaged file or another format. */
    fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        if (file.size < HEADER_BYTES || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupUnreadableException()
        }
        val buffer = ByteBuffer.wrap(file, MAGIC.size, HEADER_BYTES - MAGIC.size)
        if (buffer.get() != VERSION) throw BackupUnreadableException()
        val iterations = buffer.getInt()
        if (iterations !in 1..MAX_ITERATIONS) throw BackupUnreadableException()
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(file, 0, HEADER_BYTES)
            cipher.doFinal(file, HEADER_BYTES, file.size - HEADER_BYTES)
        } catch (_: GeneralSecurityException) {
            throw BackupUnreadableException()
        }
    }

    /**
     * The password is NFC-normalized first: the same accented password typed on different
     * keyboards must open the backup.
     */
    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val normalized = Normalizer.normalize(CharBuffer.wrap(password), Normalizer.Form.NFC).toCharArray()
        val spec = PBEKeySpec(normalized, salt, iterations, KEY_BITS)
        normalized.fill('\u0000')
        val bytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return try {
            SecretKeySpec(bytes, "AES")
        } finally {
            bytes.fill(0)
        }
    }
}

/** The file is not a backup this app can open with the given password. */
class BackupUnreadableException : Exception("backup could not be decrypted")
