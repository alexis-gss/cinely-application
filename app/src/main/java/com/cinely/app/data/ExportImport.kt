package com.cinely.app.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Format d'export : JSON minifié (clés courtes) puis compressé en GZIP.
 * -> Le format le plus simple à parser en cas de changement de version d'appli
 *    (JSON = rétrocompatible, lisible, versionnable), tout en restant compact
 *    grâce à la compression gzip (généralement /5 à /10 sur du JSON répétitif).
 * Extension du fichier généré : .cinely.json.gz
 */
@Serializable
data class ExportBundle(
    val version: Int = 1,
    val followed: List<FollowedItem> = emptyList(),
    val watched: List<WatchedItem> = emptyList()
)

object ExportImportManager {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun writeToUri(context: Context, uri: Uri, bundle: ExportBundle) {
        val text = json.encodeToString(ExportBundle.serializer(), bundle)
        context.contentResolver.openOutputStream(uri)?.use { rawOut ->
            GZIPOutputStream(rawOut).use { gzOut ->
                gzOut.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    fun readFromUri(context: Context, uri: Uri): ExportBundle {
        context.contentResolver.openInputStream(uri)?.use { rawIn ->
            GZIPInputStream(rawIn).use { gzIn ->
                val text = BufferedReader(InputStreamReader(gzIn, Charsets.UTF_8)).readText()
                return json.decodeFromString(ExportBundle.serializer(), text)
            }
        }
        throw IllegalStateException("Impossible de lire le fichier sélectionné")
    }

    fun defaultFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.FRANCE).format(java.util.Date())
        return "cinely-export-$ts.cinely.json.gz"
    }
}
