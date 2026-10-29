package io.github.igornunes.yourfinances.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Supplies the SQLCipher passphrase: 32 random bytes, stored in [keyFile] wrapped with an
 * AES-GCM key that never leaves the Android Keystore.
 *
 * File layout: `[iv length][iv][ciphertext + tag]`.
 *
 * A new key is only ever created when [databaseFile] does not exist yet: a database without a
 * readable key is reported as [DatabaseKeyUnavailableException], never silently replaced.
 */
class DatabaseKeyProvider(
    private val keyFile: File,
    private val databaseFile: File,
    /** Tests use their own alias, so they never touch the app's real key. */
    private val keyAlias: String = KEY_ALIAS,
) {
    @Volatile
    private var cached: ByteArray? = null

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        cached?.let { return it }
        val passphrase = when {
            keyFile.exists() && databaseFile.exists() -> unwrap(keyFile.readBytes())
            keyFile.exists() -> try {
                unwrap(keyFile.readBytes())
            } catch (_: DatabaseKeyUnavailableException) {
                // No database to open with it (an erase stopped halfway): a stale key is just replaced.
                delete()
                create()
            }
            databaseFile.exists() -> throw DatabaseKeyUnavailableException()
            else -> create()
        }
        cached = passphrase
        return passphrase
    }

    /** Forgets the key for good: only for erasing a database that can no longer be opened. */
    @Synchronized
    fun delete() {
        cached = null
        keyFile.delete()
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(keyAlias)
    }

    private fun create(): ByteArray {
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
        // A wrapped key without its Keystore key (wiped, or the device was restored) is lost too.
        val key = existingKeystoreKey() ?: throw DatabaseKeyUnavailableException()
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(stored, 1 + ivLength, stored.size - 1 - ivLength)
        } catch (_: AEADBadTagException) {
            // Damaged file or a different Keystore key: the passphrase cannot be recovered.
            throw DatabaseKeyUnavailableException()
        } catch (_: KeyPermanentlyInvalidatedException) {
            throw DatabaseKeyUnavailableException()
        }
        // Any other Keystore failure may be transient (busy, system error) and propagates as is,
        // so it is never mistaken for a lost key.
    }

    private fun existingKeystoreKey(): SecretKey? =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.getKey(keyAlias, null) as? SecretKey

    private fun keystoreKey(): SecretKey {
        existingKeystoreKey()?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
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

/**
 * The database key is gone for good (missing, damaged, or its Keystore key was wiped or
 * invalidated); the data can only be recovered from a backup.
 */
class DatabaseKeyUnavailableException : IllegalStateException("database key is unavailable")
