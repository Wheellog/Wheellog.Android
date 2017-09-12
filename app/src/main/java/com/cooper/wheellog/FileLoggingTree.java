package com.cooper.wheellog;

import timber.log.Timber;
import android.content.Context;
import java.io.File;
import android.os.Environment;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import com.cooper.wheellog.utils.Constants;

public class FileLoggingTree extends Timber.DebugTree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();

    private Context context;

    public FileLoggingTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {

        try {

            File direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + Constants.LOG_FOLDER_NAME);

            if (!direct.exists()) {
                direct.mkdir();
            }

            String fileNameTimeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String logTimeStamp = new SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.US).format(new Date
                    ());

            String fileName = fileNameTimeStamp + ".html";

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + Constants.LOG_FOLDER_NAME + File.separator + fileName);

            file.createNewFile();

            if (file.exists()) {

                FileOutputStream fileOutputStream = new FileOutputStream(file, true);

                fileOutputStream.write(("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp" + logTimeStamp + " :&nbsp&nbsp</strong>&nbsp&nbsp" + message + "</p>").getBytes());
                fileOutputStream.close();

            }

            //if (context != null)
                //MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);

        } catch (Exception e) {
            //Log.e(TAG, "Error while logging into file : " + e);
        }

    }
}