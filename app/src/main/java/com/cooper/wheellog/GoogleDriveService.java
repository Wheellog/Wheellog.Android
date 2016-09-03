package com.cooper.wheellog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.cooper.wheellog.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import timber.log.Timber;

public class GoogleDriveService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public GoogleDriveService() {
    }

    private GoogleApiClient mGoogleApiClient;
    private String filePath;
    private DriveId folderDriveId;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateNotification("Log upload started");
        if (intent.hasExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)) {
            filePath = intent.getStringExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION);
            getGoogleApiClient().connect();
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    private GoogleApiClient getGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        return mGoogleApiClient;
    }

    private Notification buildNotification(String text, boolean complete) {
        int icon = complete ? R.drawable.ic_stat_cloud_done : R.drawable.ic_stat_cloud_upload;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(text)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String text) {
        updateNotification(text, false);
    }

    private void updateNotification(String text, boolean complete) {
        Notification notification = buildNotification(text, complete);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Constants.DRIVE_UPLOAD_NOTIFICATION_ID, notification);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        listDriveRootFolderContents();
    }

    private void listDriveRootFolderContents() {
        Drive.DriveApi.getRootFolder(getGoogleApiClient()).listChildren(getGoogleApiClient()).setResultCallback(listDriveRootFolderContentsCallback);
    }

    private void createLogFolderInDriveRoot() {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(Constants.LOG_FOLDER_NAME).build();
        Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(
                getGoogleApiClient(), changeSet).setResultCallback(createLogFolderInDriveRootCallback);
    }

    private void createLogInFolder() {
        // create new contents resource
        if (folderDriveId == null)
            stopSelf();
        else
            Drive.DriveApi.newDriveContents(getGoogleApiClient())
                    .setResultCallback(createLogInFolderCallback);
    }

    ResultCallback<DriveApi.MetadataBufferResult> listDriveRootFolderContentsCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Timber.i("Problem while retrieving files");
                        stopSelf();
                        return;
                    }
                    for (Metadata m: result.getMetadataBuffer()) {
                        if (m.isFolder() &&
                                !m.isTrashed() &&
                                Constants.LOG_FOLDER_NAME.equals(m.getTitle())) {
                            folderDriveId = m.getDriveId();
                            break;
                        }
                    }
                    result.release();

                    Timber.i("Successfully listed files.");

                    if (folderDriveId == null) {
                        Timber.i("Folder DriveId is null");
                        createLogFolderInDriveRoot();
                    } else {
                        Timber.i("Folder DriveId is not null");
                        createLogInFolder();
                    }
                }
            };

    ResultCallback<DriveFolder.DriveFolderResult> createLogFolderInDriveRootCallback = new
            ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFolderResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Timber.i("Error while trying to create the folder");
                        stopSelf();
                        return;
                    }
                    Timber.i("Created a folder: %s", result.getDriveFolder().getDriveId());
                    folderDriveId = result.getDriveFolder().getDriveId();
                    createLogInFolder();
                }
            };

    final private ResultCallback<DriveApi.DriveContentsResult> createLogInFolderCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Timber.i("Error while trying to create new file contents");
                        stopSelf();
                        return;
                    }
                    final DriveContents driveContents = result.getDriveContents();

                    File sourceLogFile = new File(filePath);
                    updateNotification("Uploading file " + sourceLogFile.getName());
                    // write content to DriveContents
                    OutputStream outputStream = driveContents.getOutputStream();
                    Writer writer = new OutputStreamWriter(outputStream);

                    try {
                        BufferedReader br = new BufferedReader(new FileReader(sourceLogFile));
                        String line;

                        while ((line = br.readLine()) != null)
                            writer.append(line).append('\r');

                        br.close();
                        writer.close();
                    } catch (IOException e) {
                        Timber.e(e.getMessage());
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(sourceLogFile.getName())
                            .setMimeType("text/plain")
                            .setStarred(true).build();

                    // create a file in "WheelLog Logs" folder
                    folderDriveId.asDriveFolder()
                        .createFile(getGoogleApiClient(), changeSet, driveContents)
                                    .setResultCallback(fileCreatedCallback);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCreatedCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Timber.i("Error while trying to create the file");
                        updateNotification("Upload failed");
                        stopSelf();
                        return;
                    }
                    Timber.i("Created a file with content: %s", result.getDriveFile().getDriveId());
                    updateNotification("Upload Complete", true);
                    stopSelf();
                }
            };

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        stopSelf();
    }
}
