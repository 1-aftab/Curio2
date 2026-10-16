package com.aftab.curioalarm.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class ImportedSound(val path: String, val displayName: String)

/**
 * Copies a user-picked audio file into app-private storage so the alarm never depends on
 * a content URI permission, an SD card, or the original file staying where it was.
 */
object SoundStore {
    private const val MAX_BYTES = 40L * 1024 * 1024

    private fun dir(context: Context): File = File(context.filesDir, "sounds").apply { mkdirs() }

    /** Blocking; call from a background thread. */
    fun importFrom(context: Context, uri: Uri): Result<ImportedSound> = runCatching {
        val resolver = context.contentResolver
        var name = "Custom sound"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { if (it.isNotBlank()) name = it }
        }
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(60)
        val target = File(dir(context), "${System.currentTimeMillis()}_$safe")
        try {
            val input = resolver.openInputStream(uri) ?: error("Could not open the file")
            input.use { src ->
                target.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = src.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) error("File is larger than 40 MB")
                        out.write(buffer, 0, n)
                    }
                }
            }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(target.absolutePath)
                if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != "yes") {
                    error("That file has no playable audio")
                }
            } finally {
                runCatching { retriever.release() }
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        ImportedSound(target.absolutePath, name)
    }

    fun cleanup(context: Context, keep: Set<String>) {
        val now = System.currentTimeMillis()
        dir(context).listFiles()?.forEach { f ->
            // Skip very fresh files: they may belong to an editor session that is still open.
            if (f.absolutePath !in keep && now - f.lastModified() > 10 * 60 * 1000L) f.delete()
        }
    }
}
