package com.cooper.wheellog;

import timber.log.Timber;
import android.content.Context;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import com.cooper.wheellog.utils.FileUtil;

public class FileLoggingTree extends Timber.DebugTree {

    //private static final String TAG = FileLoggingTree.class.getSimpleName();

    private FileUtil fileUtil;
    private String fileName;
    private SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat logFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public FileLoggingTree(Context context) {
        fileUtil = new FileUtil(context);
        fileUtil.setIgnoreTimber(true);
        String fileNameTimeStamp = fileNameFormat.format(new Date());
        fileName = fileNameTimeStamp + ".html";
        if (fileUtil.prepareFile(fileName)) {
            fileUtil.writeLine("<style>p { background:lightgray; padding: 2; margin:2 } b { background:lightblue; padding: 2; margin-left: 10 }</style>");
        }
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        try {
            if (fileUtil.prepareFile(fileName)) {
                fileUtil.writeLine(String.format("<p><b>%s</b>%s</p>", logFormat.format(new Date()), message));
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, "Error while logging into file : " + e);
        }
    }
}