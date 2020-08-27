package com.cooper.wheellog;

import timber.log.Timber;
import android.content.Context;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import com.cooper.wheellog.utils.FileUtil;

public class FileLoggingTree extends Timber.DebugTree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();

    private Context context;
    private FileUtil fileUtil;

    public FileLoggingTree(Context context) {
        this.context = context;
        fileUtil = new FileUtil(context);
        fileUtil.setIgnoreTimber(true);
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        try {
            String fileNameTimeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String logTimeStamp = new SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.US).format(new Date());
            String fileName = fileNameTimeStamp + ".html";

            if (fileUtil.prepareFile(fileName)) {
                fileUtil.writeLine("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp" + logTimeStamp + " :&nbsp&nbsp</strong>&nbsp&nbsp" + message + "</p>");
            }
        } catch (Exception e) {
            //Log.e(TAG, "Error while logging into file : " + e);
        }
    }
}