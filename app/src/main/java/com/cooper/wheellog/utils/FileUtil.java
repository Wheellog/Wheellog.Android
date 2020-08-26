package com.cooper.wheellog.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private Dictionary<String, Uri> AndroidQCache;
    private boolean ignoreTimber = false;

    public FileUtil(Context context) {
        this.context = context;
        AndroidQCache = new Hashtable();
    }

    public void setIgnoreTimber(boolean value) {
        ignoreTimber = value;
    }

    public File getFile() {
        return file;
    }

    public String getAbsolutePath() {
        if (isNull()) {
            return null;
        }

        return file.getAbsolutePath();
    }

    public boolean isNull() {
        return file == null || file.toString().equals("null");
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
                file = new File(getPathFromUri(uri));
                return true;
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.TITLE, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + Constants.LOG_FOLDER_NAME);
            Uri contentUri = MediaStore.Downloads.getContentUri(getExternalStoreName());

            uri = getContentResolver().insert(contentUri, contentValues);
            file = new File(getPathFromUri(uri));
            AndroidQCache.put(fileName, uri);
        } else {
            // api 28 or less
            // Get the directory for the user's public pictures directory.
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), Constants.LOG_FOLDER_NAME);

            if (!dir.mkdirs() && !ignoreTimber)
                Timber.i("Directory not created");

            file = new File(dir, fileName);
        }
        return !isNull();
    }

    public boolean writeLine(String line) {
        if (isNull()) {
            if (!ignoreTimber) {
                Timber.e("Write failed. File is null");
            }
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
            if (!ignoreTimber) {
                Timber.e("File not found.");
            }
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            if (!ignoreTimber) {
                Timber.e("IOException");
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @NotNull
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

    @Nullable
    private ContentResolver getContentResolver() {
        if (context != null) {
            return context.getContentResolver();
        } else {
            return null;
        }
    }

    @NotNull
    private String getMimeType(@NotNull String fileName) {
        if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else {
            return "*/*";
        }
    }

    @Nullable
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getPathFromUri(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn();
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @Nullable
    private String getDataColumn() {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
