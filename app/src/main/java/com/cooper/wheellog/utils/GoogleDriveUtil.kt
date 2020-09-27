package com.cooper.wheellog.utils

import android.app.Activity
import android.content.Intent
import com.cooper.wheellog.R
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import timber.log.Timber
import java.util.*

class GoogleDriveUtil(private val activity: Activity) {
    private var googleClient: GoogleSignInClient? = null
    private lateinit var driveService: Drive
    private var credential: GoogleAccountCredential? = null

    private val mimeFolder = "application/vnd.google-apps.folder"
    private val mimeCsvFile = "application/vnd.google-apps.spreadsheet"

    private fun buildGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
        googleClient = GoogleSignIn.getClient(activity, gso)
    }

    fun requestSignIn(requestId: Int) {
        buildGoogleSignInClient()
        activity.startActivityForResult(googleClient?.signInIntent, requestId)
    }

    fun requestSignOut() {
        googleClient?.signOut()
    }

    private fun createFolder(folderName: String): String {
        val fileMetadata = File()
        fileMetadata.name = folderName
        fileMetadata.mimeType = mimeFolder

        val file: File = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute()
        return file.id
    }

    private fun getOrCreateFolder(folderName: String): String {
        val fileList = driveService.files().list()
                .setQ("mimeType = '$mimeFolder' and name = '$folderName'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .execute()
        if (fileList.files.size == 0) {
            return createFolder(folderName)
        }
        return fileList.files[0].id
    }

    fun uploadFile(filePath: String, folderName: String): String {
        val file = java.io.File(filePath)
        val metadata = File()
        metadata.mimeType = mimeCsvFile
        metadata.name = file.name
        val folderId = getOrCreateFolder(folderName)
        metadata.parents = Collections.singletonList(folderId)

        val content = FileContent("text/csv", file)
        val driveFile = driveService.files()
                .create(metadata, content)
                .setFields("id")
                .execute()
        return driveFile.id
    }

    fun alreadyLoggedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        return account != null
    }

    fun handleSignInResult(data: Intent?, callback: (result: Boolean) -> Unit) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                    credential = GoogleAccountCredential.usingOAuth2(activity, setOf(DriveScopes.DRIVE_FILE))
                    credential?.selectedAccount = googleAccount.account
                    driveService = Drive.Builder(
                            NetHttpTransport(),
                            JacksonFactory.getDefaultInstance())
                    { httpRequest ->
                        credential?.initialize(httpRequest)
                        httpRequest.connectTimeout = 3 * 60000  // 3 minutes connect timeout
                        httpRequest.readTimeout = 3 * 60000  // 3 minutes read timeout
                    }
                            .setApplicationName(activity.getString(R.string.app_name))
                            .build()
                    callback(true)
                }.addOnFailureListener { e: Exception? ->
                    Timber.e(e, "signIn failed")
                    callback(true)
                }
    }
}