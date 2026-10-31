package com.cooper.wheellog.data

import android.app.Application
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.FileUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AndroidCsvImportStore(private val context: Context) : CsvImportStore {
    override fun existing() = FileUtil.fillTrips(context).map { trip ->
        CsvImportSource(trip.fileName) { NativeTripFiles.open(context,trip) }
    }
    fun source(uri: Uri): CsvImportSource {
        val name = runCatching {
            context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "WheelLog.csv"
        return CsvImportSource(name) { context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open CSV") }
    }
    override fun publish(name: String, staged: File): String {
        if (Build.VERSION.SDK_INT < 29) {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Constants.LOG_FOLDER_NAME+"/manual")
            check(directory.isDirectory || directory.mkdirs())
            val file = File(directory,name)
            check(file.canonicalFile.parentFile == directory.canonicalFile)
            check(file.createNewFile()) { "Filename already exists" }
            var complete = false
            try {
                FileOutputStream(file).use { output -> staged.inputStream().use { it.copyTo(output) }; output.fd.sync() }
                check(file.length() == staged.length())
                complete = true
                return file.name
            } finally { if (!complete) file.delete() }
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME,name); put(MediaStore.Downloads.MIME_TYPE,"text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/"+Constants.LOG_FOLDER_NAME+"/manual")
            put(MediaStore.Downloads.IS_PENDING,1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection,values) ?: throw IOException("Cannot create CSV")
        var complete = false
        try {
            resolver.openOutputStream(uri,"w")?.use { output ->
                staged.inputStream().use { check(it.copyTo(output) == staged.length()) }
            } ?: throw IOException("Cannot write CSV")
            val actualName = resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                check(it.moveToFirst()); it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } ?: throw IOException("Cannot read saved filename")
            check(resolver.update(uri,ContentValues().apply { put(MediaStore.Downloads.IS_PENDING,0) },null,null) > 0)
            complete = true
            return actualName
        } finally { if (!complete) resolver.delete(uri,null,null) }
    }
}

data class CsvImportState(val running: Boolean = false, val progress: CsvImportProgress? = null, val result: CsvImportResult? = null)
class CsvImportViewModel(application: Application) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(CsvImportState())
    val state = mutable.asStateFlow()
    fun start(uris: List<Uri>): Boolean {
        if (mutable.value.running) return false
        if (uris.isEmpty()) return true
        mutable.value = CsvImportState(running = true,progress = CsvImportProgress(0,uris.size,true))
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    importMutex.withLock {
                        val store = AndroidCsvImportStore(getApplication<Application>())
                        val job = currentCoroutineContext()
                        CsvImportEngine(File(getApplication<Application>().cacheDir,"csv-import"),store).run(
                            uris.map(store::source), { job.ensureActive() }) { progress ->
                            mutable.value = CsvImportState(true,progress)
                        }
                    }
                }
                mutable.value = CsvImportState(result = result)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { mutable.value = CsvImportState(result = CsvImportResult(0,0,uris.map { it.lastPathSegment ?: "CSV" })) }
        }
        return true
    }
    fun acknowledge() { if (!mutable.value.running) mutable.value = CsvImportState() }
    companion object { private val importMutex = Mutex() }
}
