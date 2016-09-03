package com.cooper.wheellog.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import timber.log.Timber;

public class FileUtil {

    public static File getFile(String filename) {
        // Get the directory for the user's public pictures directory.
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), Constants.LOG_FOLDER_NAME);

        if (!dir.mkdirs())
            Timber.i("Directory not created");

        return new File(dir, filename);
    }

    public static boolean writeLine(String filename, String line) {
        File file = getFile(filename);

        if (file == null) {
            Timber.e("Write failed. File is null");
            return false;
        }

        try {
            FileOutputStream f = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(f);

            pw.println(line);

            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            Timber.e("File not found.");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Timber.e("IOException");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
