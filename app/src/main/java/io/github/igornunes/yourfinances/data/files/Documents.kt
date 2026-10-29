package io.github.igornunes.yourfinances.data.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Files the user picked through the Storage Access Framework, addressed by their URI string, so
 * no storage permission is needed.
 */
interface Documents {
    suspend fun write(uri: String, bytes: ByteArray)

    /** @throws IOException when the file cannot be read or is larger than any backup could be. */
    suspend fun read(uri: String): ByteArray

    suspend fun delete(uri: String)
}

class ContentResolverDocuments(private val resolver: ContentResolver) : Documents {
    override suspend fun write(uri: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        // "wt" truncates: overwriting a longer old file must not leave its tail behind.
        val stream = resolver.openOutputStream(Uri.parse(uri), "wt") ?: throw IOException("cannot open document")
        stream.use { it.write(bytes) }
    }

    override suspend fun read(uri: String): ByteArray = withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(Uri.parse(uri)) ?: throw IOException("cannot open document")
        stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > MAX_BYTES) throw IOException("document too large")
            }
            out.toByteArray()
        }
    }

    override suspend fun delete(uri: String) = withContext(Dispatchers.IO) {
        if (!DocumentsContract.deleteDocument(resolver, Uri.parse(uri))) throw IOException("cannot delete document")
    }

    private companion object {
        /** Years of entries take a few MB; the limit only stops absurd files before they exhaust memory. */
        const val MAX_BYTES = 16 * 1024 * 1024
    }
}
