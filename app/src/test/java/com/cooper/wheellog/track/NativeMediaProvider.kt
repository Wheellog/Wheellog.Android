package com.cooper.wheellog.track

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File

/** Real appendable files behind a MediaStore provider for Android 10/15 recorder tests. */
class NativeMediaProvider(private val directory: File) : ContentProvider() {
    val files = linkedMapOf<Long, Pair<File, ContentValues>>()
    override fun onCreate() = true
    override fun getType(uri: Uri) = "text/comma-separated-values"
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val id = (files.keys.maxOrNull() ?: 0) + 1
        directory.mkdirs()
        val data = ContentValues(values ?: ContentValues())
        val file = File(directory, "${id}_${data.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)}").apply { createNewFile() }
        files[id] = file to data
        return ContentUris.withAppendedId(uri, id)
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val columns = projection?.toList() ?: listOf("_id", "_display_name", "_data", "mime_type", "_size", "title")
        val selectedId = uri.lastPathSegment?.toLongOrNull()
        return MatrixCursor(columns.toTypedArray()).apply {
            files.forEach { (id, pair) -> if (selectedId == null || selectedId == id) {
                val (file, data) = pair
                addRow(columns.map { column -> when (column) {
                    "_id" -> id
                    "_data" -> file.absolutePath
                    "_size" -> file.length()
                    "date_modified" -> file.lastModified() / 1_000
                    else -> data[column]
                } }.toTypedArray())
            } }
        }
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = files[uri.lastPathSegment!!.toLong()]?.first ?: throw java.io.FileNotFoundException()
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int =
        files.remove(uri.lastPathSegment?.toLongOrNull())?.let { it.first.delete(); 1 } ?: 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
}
