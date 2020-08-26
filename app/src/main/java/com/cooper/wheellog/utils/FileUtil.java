package com.cooper.wheellog.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;

import timber.log.Timber;

public class FileUtil {
    private Context context;
    private File file;
    private Uri uri;
    private String pathName = "WheelLog Logs";
    private Dictionary<String, Uri> AndroidQCache;

    public FileUtil(Context context) {
        this.context = context;
        AndroidQCache = new Hashtable();
    }

    public File getFile() {
        return file;
    }

    public Uri getUri() {
        return uri;
    }

    public String getAbsolutePath() {
        return uri != null ? uri.getPath() : file.getAbsolutePath();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String getExternalStoreName() {
        Set<String> names = MediaStore.getExternalVolumeNames(context);
        for (String n : names) {
            if (n.contains("external")) {
                return n;
            }
        }
        return names.toArray()[0].toString();
    }

    public boolean isNull() {
        return file == null || file.toString().equals("null");
    }

    private ContentResolver getContentResolver() {
        if (context != null) {
            return context.getContentResolver();
        } else {
            return null;
        }
    }

    public boolean isFileExists(final String filePath) {
        if (isNull()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                Uri uri = Uri.parse(filePath);
                ContentResolver resolver = getContentResolver();
                AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r");
                if (afd == null) return false;
                try {
                    afd.close();
                } catch (IOException ignore) {
                }
            } catch (FileNotFoundException e) {
                return false;
            }
            return true;
        } else {
            return file.exists();
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else {
            return "*/*";
        }
    }

    public boolean prepareFile(String fileName) {
        uri = null;
        file = null;
        // Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Get uri from cashed dictionary
            // It need to not create dublicate files
            uri = AndroidQCache.get(fileName);
            if (uri != null) {
                file = new File(String.valueOf(uri));
                return true;
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.TITLE, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + pathName);
            Uri contentUri = MediaStore.Downloads.getContentUri(getExternalStoreName());

            uri = getContentResolver().insert(contentUri, contentValues);
            file = new File(String.valueOf(uri));
            AndroidQCache.put(fileName, uri);
        } else {
            // api 28 or less
            // Get the directory for the user's public pictures directory.
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), Constants.LOG_FOLDER_NAME);

            if (!dir.mkdirs())
                Timber.i("Directory not created");

            file = new File(dir, fileName);
        }
        return !isNull();
    }

    public boolean writeLine(String line) {
        if (isNull()) {
            Timber.e("Write failed. File is null");
            return false;
        }

        OutputStream f;
        try {
            if (uri != null) {
                f = context.getContentResolver().openOutputStream(Objects.requireNonNull(uri), "wa");
            } else {
                f = new FileOutputStream(file, true);
            }
            PrintWriter pw = new PrintWriter(f);

            pw.println(line);

            // TODO Frequent opening and closing is not a good practice
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
