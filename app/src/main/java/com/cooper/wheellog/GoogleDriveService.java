package com.cooper.wheellog;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cooper.wheellog.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.internal.MetadataBundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;

import timber.log.Timber;

public class GoogleDriveService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public GoogleDriveService() {
    }

    private GoogleApiClient mGoogleApiClient;
    private String filePath;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // create new contents resource
        Drive.DriveApi.newDriveContents(getGoogleApiClient())
                .setResultCallback(driveContentsCallback);
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Timber.i("Error while trying to create new file contents");
                        stopSelf();
                        return;
                    }
                    final DriveContents driveContents = result.getDriveContents();

                    // Perform I/O off the UI thread.
                    new Thread() {
                        @Override
                        public void run() {
                            File sourceLogFile = new File(filePath);
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

                            // create a file on root folder
                            Drive.DriveApi.getRootFolder(getGoogleApiClient())
                                    .createFile(getGoogleApiClient(), changeSet, driveContents)
                                    .setResultCallback(fileCallback);
                        }
                    }.start();
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Timber.i("Error while trying to create the file");
                        stopSelf();
                        return;
                    }
                    Timber.i("Created a file with content: %s", result.getDriveFile().getDriveId());
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
