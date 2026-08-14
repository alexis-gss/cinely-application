package com.orion.app.cinema.data

import android.content.Context
import android.net.Uri
import com.orion.app.R
import com.orion.app.core.data.GzipJsonExportImport
import kotlinx.serialization.Serializable

/**
 * Cinema export bundle. See [GzipJsonExportImport] for the shared gzip+JSON format
 * (extension of the generated file: .orion.json.gz).
 */
@Serializable
data class ExportBundle(
    val version: Int = 1,
    val followed: List<FollowedItem> = emptyList(),
    val watched: List<WatchedItem> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList()
)

/** Cinema-domain façade over [GzipJsonExportImport], fixing the bundle serializer and file-name prefix. */
object CinemaExportImportManager {

    fun writeToUri(context: Context, uri: Uri, bundle: ExportBundle) =
        GzipJsonExportImport.writeToUri(context, uri, ExportBundle.serializer(), bundle)

    fun readFromUri(context: Context, uri: Uri): ExportBundle =
        GzipJsonExportImport.readFromUri(
            context, uri, ExportBundle.serializer(),
            readErrorMessage = context.getString(R.string.settings_data_export_import_read_error)
        )

    fun defaultFileName(): String = GzipJsonExportImport.defaultFileName("orion-export", "orion.json.gz")
}
