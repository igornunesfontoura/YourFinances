package io.github.igornunes.yourfinances.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Supplies the SQLCipher passphrase: 32 random bytes, stored in [keyFile] wrapped with an
 * AES-GCM key that never leaves the Android Keystore.
 *
 * File layout: `[iv length][iv][ciphertext + tag]`.
 */
class DatabaseKeyProvider(private val keyFile: File) {

    fun getOrCreatePassphrase(): ByteArray {
        if (keyFile.exists()) return unwrap(keyFile.readBytes())

        val passphrase = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        val tmp = File(keyFile.parentFile, "${keyFile.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(wrap(passphrase))
            out.fd.sync()
        }
        check(tmp.renameTo(keyFile)) { "could not store the database key" }
        return passphrase
    }

    private fun wrap(passphrase: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(passphrase)
    }

    private fun unwrap(stored: ByteArray): ByteArray {
        val ivLength = stored.firstOrNull()?.toInt() ?: 0
        if (ivLength <= 0 || stored.size <= 1 + ivLength) throw DatabaseKeyUnavailableException()
        val iv = stored.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(stored, 1 + ivLength, stored.size - 1 - ivLength)
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "your_finances_database_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PASSPHRASE_BYTES = 32
    }
}

/** The stored database key is unreadable; the data can only be recovered from a backup. */
class DatabaseKeyUnavailableException : IllegalStateException("database key file is corrupted")
