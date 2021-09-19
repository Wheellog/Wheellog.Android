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

import com.cooper.wheellog.views.TripModel;
import com.google.common.io.ByteStreams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

public class FileUtil {
    private final Context context;
    private File file;
    private Uri uri;
    private final Hashtable<String, CachedFile> AndroidQCache = new Hashtable<>();
    private boolean ignoreTimber = false;
    private OutputStream stream;

    static class CachedFile {
        public File file;
        public Uri uri;
    }

    public FileUtil(Context context) {
        this.context = context;
    }

    public String fileName = "";

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
        return file == null || file.toString().equals("null") || stream == null;
    }

    public boolean prepareFile(String fileName) {
        return prepareFile(fileName, "");
    }

    public boolean prepareFile(String fileName, String folder) {
        this.fileName = fileName;
        uri = null;
        file = null;
        // Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Get uri from cashed dictionary
            // It need to not create duplicate files
            if (AndroidQCache.containsKey(fileName)) {
                CachedFile cache = AndroidQCache.get(fileName);
                uri = cache.uri;
                file = cache.file;
                return true;
            }
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.Downloads.TITLE, fileName);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
                String path = Environment.DIRECTORY_DOWNLOADS + File.separator + Constants.LOG_FOLDER_NAME;
                if (folder != null && !folder.equals("")) {
                    path += File.separator + folder.replace(':', '_');
                }
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, path);
                Uri contentUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

                uri = Objects.requireNonNull(getContentResolver()).insert(contentUri, contentValues);
                file = new File(getPathFromUri(uri));
            } finally {
                CachedFile cache = new CachedFile();
                cache.uri = uri;
                cache.file = file;
                AndroidQCache.put(fileName, cache);
            }
        } else {
            // api 28 or less
            // Get the directory for the user's public pictures directory.
            String path = Constants.LOG_FOLDER_NAME;
            if (folder != null && !folder.equals("")) {
                path += File.separator + folder.replace(':', '_');;
            }
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), path);

            if (!dir.mkdirs() && !ignoreTimber)
                Timber.i("Directory not created");

            file = new File(dir, fileName);
        }
        prepareStream();
        return !isNull();
    }

    public void prepareStream() {
        try {
            close();
            if (uri != null) {
                stream = context.getContentResolver().openOutputStream(Objects.requireNonNull(uri), "wa");
            } else if (file != null) {
                stream = new FileOutputStream(file, true);
            }
        } catch (FileNotFoundException e) {
            if (!ignoreTimber) {
                Timber.e("File not found.");
            }
            //e.printStackTrace();
        }
    }

    public InputStream getInputStream() {
        try {
            if (uri != null) {
                return context.getContentResolver().openInputStream(Objects.requireNonNull(uri));
            } else if (file != null) {
                return new FileInputStream(file);
            }
        } catch (FileNotFoundException e) {
            if (!ignoreTimber) {
                Timber.e("File not found.");
            }
            e.printStackTrace();
        }
        return null;
    }

    public byte[] readBytes() throws IOException {
        InputStream inputStream = null;
        if (uri != null) {
            inputStream = context.getContentResolver().openInputStream(uri);
        } else if (file != null) {
            inputStream = new FileInputStream(file);
        }
        assert inputStream != null;
        return ByteStreams.toByteArray(inputStream);
    }

    public static byte[] readBytes(String filePath) throws IOException {
        InputStream inputStream = new FileInputStream(filePath);
        return ByteStreams.toByteArray(inputStream);
    }

    public static byte[] readBytes(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        return ByteStreams.toByteArray(inputStream);
    }

    static String sizeTokb(Long size) {
        return String.format(Locale.US, "%.2f Kb", size / 1024f);
    }

    public static FileUtil getLastLog(Context context) {
        String fileStartsWith = new SimpleDateFormat("yyyy_MM_dd", Locale.US).format(new Date());

        // Android 9 or less
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), Constants.LOG_FOLDER_NAME);
            File[] filesArray = dir.listFiles();
            if (filesArray == null) {
                return null;
            }
            for (File wheelDir: filesArray) {
                if (wheelDir.isDirectory()) {
                    File[] wheelFiles = wheelDir.listFiles();
                    if (wheelFiles == null) {
                        continue;
                    }
                    for (File f: wheelFiles) {
                        int indexExt = f.getAbsolutePath().lastIndexOf(".");
                        if (f.isDirectory() || indexExt < 1) {
                            continue;
                        }
                        String extension = f.getAbsolutePath().substring(indexExt);
                        if (extension.equals(".csv") && f.getName().startsWith(fileStartsWith)) {
                            FileUtil result = new FileUtil(context);
                            result.file = f;
                            result.fileName = f.getName();
                            return result;
                        }
                    }
                }
            }
            return null;
        }
        // Android 10+
        Uri uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String[] projection = {
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.TITLE,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads._ID
        };
        String where = String.format("%s = 'text/comma-separated-values'", MediaStore.Downloads.MIME_TYPE);
        Cursor cursor = context.getContentResolver().query(uri,
                projection,
                where + " AND " + MediaStore.Downloads.DISPLAY_NAME + " LIKE ?",
                new String[] { fileStartsWith + "%" },
                MediaStore.Downloads.DATE_MODIFIED + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME));
            String mediaId = cursor.getString(cursor.getColumnIndex(MediaStore.Downloads._ID));
            cursor.close();
            FileUtil result = new FileUtil(context);
            Uri downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL);
            result.uri = Uri.withAppendedPath(downloads, mediaId);
            result.file = new File(result.getPathFromUri(result.uri));
            result.fileName = title;
            return result;
        }
        return null;
    }

    public static ArrayList<TripModel> fillTrips(Context context) {
        ArrayList<TripModel> tripModels = new ArrayList<>();
        // Android 9 or less
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), Constants.LOG_FOLDER_NAME);
            File[] filesArray = dir.listFiles();
            if (filesArray == null) {
                return tripModels;
            }
            for (File wheelDir: filesArray) {
                if (wheelDir.isDirectory()) {
                    File[] wheelFiles = wheelDir.listFiles();
                    if (wheelFiles == null) {
                        continue;
                    }
                    for (File f: wheelFiles) {
                        int indexExt = f.getAbsolutePath().lastIndexOf(".");
                        if (f.isDirectory() || indexExt < 1) {
                            continue;
                        }
                        String extension = f.getAbsolutePath().substring(indexExt);
                        if (extension.equals(".csv") && !f.getName().startsWith("RAW")) {
                            tripModels.add(new TripModel(f.getName(), sizeTokb(f.length()), f.getAbsolutePath()));
                        }
                    }
                }
            }
            return tripModels;
        }
        // Android 10+
        Uri uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String[] projection = {
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.TITLE,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads._ID
        };
        String where = String.format("%s = 'text/comma-separated-values'", MediaStore.Downloads.MIME_TYPE);
        Cursor cursor = context.getContentResolver().query(uri,
                projection,
                where + " AND " + MediaStore.Downloads.DISPLAY_NAME + " NOT LIKE ?",
                new String[] { "RAW_%" },
                MediaStore.Downloads.DATE_MODIFIED + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME));
                String description = sizeTokb(cursor.getLong(cursor.getColumnIndex(MediaStore.Downloads.SIZE)));
                String mediaId = cursor.getString(cursor.getColumnIndex(MediaStore.Downloads._ID));
                tripModels.add(new TripModel(title, description, mediaId));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return tripModels;
    }

    public void close() {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
            stream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLine(String line) {
        if (isNull()) {
            if (!ignoreTimber) {
                Timber.e("Write failed. File is null");
            }
            return;
        }
        if (stream == null) {
            if (!ignoreTimber) {
                Timber.e("Write failed. Stream is null. Forgot to call prepareStream()?");
            }
            return;
        }

        try {
            stream.write((line + "\r\n").getBytes());
            stream.flush();
        } catch (IOException e) {
            if (!ignoreTimber) {
                Timber.e("IOException");
            }
            e.printStackTrace();
        }
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected String getPathFromUri(@Nullable Uri uri) {
        if (uri == null) {
            return "";
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn();
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return "";
    }

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
        return "";
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
