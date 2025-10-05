package com.cooper.wheellog

import android.content.Context
import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EventsLoggingTree(var context: Context, var mainAdapter: MainPageAdapter) : Timber.Tree() {

    private var filename: String = "eventsLog"
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val oldLogsDivider = "------------"
    private val separator = "\n"
    private val maxLines = 100
    private val maxLinesInFile = 200
    private var outputStream: FileOutputStream

    init {
        outputStream = if (context.fileList().contains(filename)) {
            val allLines = context.openFileInput(filename).bufferedReader().readLines()
            val lastMessages = allLines.takeLast(maxLines).joinToString(separator = separator) + separator
            logEvent("$lastMessages$oldLogsDivider$separator")
            if (allLines.count() < maxLinesInFile) {
                context.openFileOutput(filename, Context.MODE_APPEND)
            } else {
                // overwrite a file if its size exceeds the maximum number of lines
                context.openFileOutput(filename, Context.MODE_PRIVATE).apply {
                    write(lastMessages.toByteArray())
                    flush()
                }
            }
        } else {
            context.openFileOutput(filename, Context.MODE_APPEND)
        }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority == Log.ASSERT
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val formattedMessage = String.format("%s - %s%n", timeFormatter.format(Date()), message)
            outputStream.write(formattedMessage.toByteArray())
            outputStream.flush()
            logEvent(formattedMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        outputStream.close()
    }

    fun logEvent(message: String) {
        logsCache.append(message)
        if (eventsCurrentCount > eventsMaxCount) {
            val indexOfNewLine = logsCache.indexOfFirst { r -> r == '\n' }
            logsCache.delete(0, indexOfNewLine)
        } else {
            eventsCurrentCount++
        }
    }

    public companion object {
        var logsCache = StringBuffer()
        private var eventsCurrentCount = 0
        private var eventsMaxCount = 500
    }
}