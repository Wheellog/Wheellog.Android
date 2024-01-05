package com.cooper.wheellog.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cooper.wheellog.views.TripModel
import com.google.common.io.ByteStreams
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Hashtable
import java.util.Locale

class FileUtil(val context: Context) {
    var file: File? = null
        private set
    private var uri: Uri? = null
    private val androidQCache = Hashtable<String, CachedFile>()
    private var ignoreTimber = false
    private var stream: OutputStream? = null

    internal class CachedFile {
        var file: File? = null
        var uri: Uri? = null
    }

    var fileName = ""
    fun setIgnoreTimber(value: Boolean) {
        ignoreTimber = value
    }

    val absolutePath: String?
        get() = if (isNull) {
            null
        } else file!!.absolutePath
    val isNull: Boolean
        get() = file == null || file.toString() == "null" || stream == null

    @JvmOverloads
    fun prepareFile(fileName: String, folder: String? = ""): Boolean {
        this.fileName = fileName
        uri = null
        file = null
        // Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Get uri from cashed dictionary
            // It need to not create duplicate files
            if (androidQCache.containsKey(fileName)) {
                val cache = androidQCache[fileName]
                uri = cache!!.uri
                file = cache.file
                return true
            }
            try {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                contentValues.put(MediaStore.Downloads.TITLE, fileName)
                contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName))
                var path =
                    Environment.DIRECTORY_DOWNLOADS + File.separator + Constants.LOG_FOLDER_NAME
                if (folder != null && folder != "") {
                    path += File.separator + folder.replace(':', '_')
                }
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, path)
                val contentUri =
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                uri = contentResolver?.insert(contentUri, contentValues)
                file = getPathFromUri(uri)?.let { File(it) }
            } finally {
                val cache = CachedFile()
                cache.uri = uri
                cache.file = file
                androidQCache[fileName] = cache
            }
        } else {
            // api 28 or less
            // Get the directory for the user's public pictures directory.
            var path = Constants.LOG_FOLDER_NAME
            if (folder != null && folder != "") {
                path += File.separator + folder.replace(':', '_')
            }
            val dir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ), path
            )
            if (!dir.mkdirs() && !ignoreTimber) Timber.i("Directory not created")
            file = File(dir, fileName)
        }
        prepareStream()
        return !isNull
    }

    fun prepareStream() {
        try {
            close()
            if (uri != null) {
                stream = context.contentResolver.openOutputStream(uri!!, "wa")
            } else if (file != null) {
                stream = FileOutputStream(file, true)
            }
        } catch (e: FileNotFoundException) {
            if (!ignoreTimber) {
                Timber.e("File not found.")
            }
            //e.printStackTrace();
        }
    }

    val inputStream: InputStream?
        get() {
            try {
                if (uri != null) {
                    return context.contentResolver.openInputStream(uri!!)
                } else if (file != null) {
                    return FileInputStream(file)
                }
            } catch (e: FileNotFoundException) {
                if (!ignoreTimber) {
                    Timber.e("File not found.")
                }
                e.printStackTrace()
            }
            return null
        }

    @Throws(IOException::class)
    fun readBytes(): ByteArray? {
        var inputStream: InputStream? = null
        if (uri != null) {
            inputStream = context.contentResolver.openInputStream(uri!!)
        } else if (file != null) {
            inputStream = FileInputStream(file)
        }
        return inputStream?.let { ByteStreams.toByteArray(it) }
    }

    fun close() {
        if (stream == null) {
            return
        }
        try {
            stream!!.close()
            stream = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeLine(line: String) {
        if (isNull) {
            if (!ignoreTimber) {
                Timber.wtf("Write failed. File is null")
            }
            return
        }
        if (stream == null) {
            if (!ignoreTimber) {
                Timber.wtf("Write failed. Stream is null. Forgot to call prepareStream()?")
            }
            return
        }
        try {
            stream!!.write((line + "\r\n").toByteArray())
            stream!!.flush()
        } catch (e: IOException) {
            if (!ignoreTimber) {
                Timber.wtf("IOException %s", e.message)
            }
            e.printStackTrace()
        }
    }

    private val contentResolver: ContentResolver?
        get() = context.contentResolver

    private fun getMimeType(fileName: String): String {
        return if (fileName.endsWith(".csv")) {
            "text/csv"
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            "text/html"
        } else {
            "*/*"
        }
    }

    fun getPathFromUri(uri: Uri?): String? {
        if (uri == null) {
            return ""
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            return dataColumn
        }
        return if ("file".equals(uri.scheme, ignoreCase = true)) {
            uri.path
        } else ""
    }

    private val dataColumn: String
        get() {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )
            try {
                cursor = context.contentResolver.query(
                    uri!!, projection, null, null,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return ""
        }

    @Throws(Throwable::class)
    protected fun finalize() {
        close()
    }

    companion object {
        private fun sizeToKb(size: Long): String {
            return String.format(Locale.US, "%.2f Kb", size / 1024f)
        }

        fun getLastLog(context: Context): FileUtil? {
            val fileStartsWith = SimpleDateFormat("yyyy_MM_dd", Locale.US).format(Date())

            // Android 9 or less
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ), Constants.LOG_FOLDER_NAME
                )
                val filesArray = dir.listFiles() ?: return null
                for (wheelDir in filesArray) {
                    if (wheelDir.isDirectory) {
                        val wheelFiles = wheelDir.listFiles() ?: continue
                        for (f in wheelFiles) {
                            val indexExt = f.absolutePath.lastIndexOf(".")
                            if (f.isDirectory || indexExt < 1) {
                                continue
                            }
                            val extension = f.absolutePath.substring(indexExt)
                            if (extension == ".csv" && f.name.startsWith(fileStartsWith)) {
                                val result = FileUtil(context)
                                result.file = f
                                result.fileName = f.name
                                return result
                            }
                        }
                    }
                }
                return null
            }
            // Android 10+
            val uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.TITLE,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads._ID
            )
            val where =
                String.format("%s = 'text/comma-separated-values'", MediaStore.Downloads.MIME_TYPE)
            val cursor = context.contentResolver.query(
                uri,
                projection,
                where + " AND " + MediaStore.Downloads.DISPLAY_NAME + " LIKE ?", arrayOf(
                    "$fileStartsWith%"
                ),
                MediaStore.Downloads.DATE_MODIFIED + " DESC"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val title =
                    cursor.getString(0.coerceAtLeast(cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)))
                val mediaId =
                    cursor.getString(0.coerceAtLeast(cursor.getColumnIndex(MediaStore.Downloads._ID)))
                cursor.close()
                val result = FileUtil(context)
                val downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
                result.uri = Uri.withAppendedPath(downloads, mediaId)
                result.file = result.getPathFromUri(result.uri)?.let { File(it) }
                result.fileName = title
                return result
            }
            return null
        }

        fun fillTrips(context: Context): ArrayList<TripModel> {
            val tripModels = ArrayList<TripModel>()
            // Android 9 or less
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ), Constants.LOG_FOLDER_NAME
                )
                var filesArray = dir.listFiles() ?: arrayOf<File>()

                val dirOld = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ), Constants.LOG_FOLDER_OLD_NAME
                )
                filesArray += dirOld.listFiles() ?: arrayOf<File>()
                if (filesArray.isEmpty()) {
                    return tripModels
                }
                for (wheelDir in filesArray) {
                    if (wheelDir.isDirectory) {
                        val wheelFiles = wheelDir.listFiles() ?: continue
                        for (f in wheelFiles) {
                            val indexExt = f.absolutePath.lastIndexOf(".")
                            if (f.isDirectory || indexExt < 1) {
                                continue
                            }
                            val extension = f.absolutePath.substring(indexExt)
                            if (extension == ".csv" && !f.name.startsWith("RAW")) {
                                tripModels.add(
                                    TripModel(
                                        f.name,
                                        sizeToKb(f.length()),
                                        f.absolutePath
                                    )
                                )
                            }
                        }
                    }
                }
                return tripModels
            }
            // Android 10+
            val uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.TITLE,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads._ID
            )
            val where =
                String.format("%s = 'text/comma-separated-values'", MediaStore.Downloads.MIME_TYPE)
            val cursor = context.contentResolver.query(
                uri,
                projection,
                where + " AND " + MediaStore.Downloads.DISPLAY_NAME + " NOT LIKE ?",
                arrayOf("RAW_%"),
                MediaStore.Downloads.DATE_MODIFIED + " DESC"
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val title =
                        cursor.getString(0.coerceAtLeast(cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)))
                    val description =
                        sizeToKb(cursor.getLong(0.coerceAtLeast(cursor.getColumnIndex(MediaStore.Downloads.SIZE))))
                    val mediaId =
                        cursor.getString(0.coerceAtLeast(cursor.getColumnIndex(MediaStore.Downloads._ID)))
                    tripModels.add(TripModel(title, description, mediaId))
                } while (cursor.moveToNext())
                cursor.close()
            }
            return tripModels
        }
    }
}
