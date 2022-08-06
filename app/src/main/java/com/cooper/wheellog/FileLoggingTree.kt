package com.cooper.wheellog

import android.content.Context
import timber.log.Timber.DebugTree
import com.cooper.wheellog.utils.FileUtil
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(context: Context) : DebugTree() {

    //private static final String TAG = FileLoggingTree.class.getSimpleName();
    private val fileUtil: FileUtil
    private val fileName: String
    private val logFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        fileUtil = FileUtil(context)
        fileUtil.setIgnoreTimber(true)

        val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fileNameTimeStamp = fileNameFormat.format(Date())
        fileName = "$fileNameTimeStamp.html"

        if (fileUtil.prepareFile(fileName)) {
            fileUtil.writeLine("<style>p { background:lightgray; padding: 2; margin:2 } b { background:lightblue; padding: 2; margin-left: 10 }</style>")
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            if (fileUtil.prepareFile(fileName)) {
                fileUtil.writeLine(String.format("<p><b>%s</b>%s</p>", logFormat.format(Date()), message))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //Log.e(TAG, "Error while logging into file : " + e);
        }
    }
}