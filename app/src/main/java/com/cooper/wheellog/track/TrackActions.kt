package com.cooper.wheellog.track

import android.content.*
import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.cooper.wheellog.R
import com.cooper.wheellog.data.NativeTripFiles
import com.cooper.wheellog.views.TripModel
import kotlinx.coroutines.*
import java.io.File

object TrackActions {
    fun rename(context: Context, title: String, save: (String) -> Unit, changed: () -> Unit) {
        val field = EditText(context).apply { setSingleLine(); setText(title); selectAll(); filters = arrayOf(InputFilter.LengthFilter(100)) }
        val dialog = AlertDialog.Builder(context).setTitle(R.string.track_rename).setView(field)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = field.text.toString().trim()
                if (value.isEmpty() || value.any { it.isISOControl() }) field.error = context.getString(R.string.track_name_error)
                else CoroutineScope(Dispatchers.Main).launch {
                    runCatching { withContext(Dispatchers.IO) { save(value) } }
                        .onSuccess { dialog.dismiss(); changed() }
                        .onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
                }
            }
        }
        dialog.show()
    }
    fun exportGpx(context: Context, source: TripModel): File {
        val title = context.getSharedPreferences("trip_titles", Context.MODE_PRIVATE).getString(source.fileName, source.title)!!
        val safeName = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim(' ', '.').take(70).ifBlank { "WheelLog" }
        val directory = File(context.cacheDir, "gpx").apply { mkdirs() }
        val output = File(directory, safeName + "_" + source.fileName.hashCode().toUInt().toString(16) + ".gpx")
        val atomic = android.util.AtomicFile(output)
        val stream = atomic.startWrite()
        try {
            val writer = stream.writer(Charsets.UTF_8)
            GpxFormat.write(writer, title, NativeTripFiles.points(context, source).asSequence())
            writer.flush(); atomic.finishWrite(stream)
        } catch (error: Exception) { atomic.failWrite(stream); throw error }
        return output
    }
    fun shareGpx(context: Context, source: TripModel) {
        CoroutineScope(Dispatchers.Main).launch {
            runCatching { withContext(Dispatchers.IO) { exportGpx(context, source) } }.onSuccess { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/gpx+xml"; putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, context.getString(R.string.track_share)))
            }.onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
        }
    }
}
