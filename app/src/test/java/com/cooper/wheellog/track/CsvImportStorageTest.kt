package com.cooper.wheellog.track

import android.app.Application
import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.data.AndroidCsvImportStore
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[29])
class CsvImportStorageTest {
    @get:Rule val temporary=TemporaryFolder()
    private lateinit var provider: DownloadsProvider
    private lateinit var store: AndroidCsvImportStore
    @Before fun setup() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        provider=DownloadsProvider().apply {
            directory=temporary.newFolder()
            attachInfo(context,android.content.pm.ProviderInfo().apply {
                authority="media"; exported=true; applicationInfo=context.applicationInfo
            })
        }
        ShadowContentResolver.registerProviderInternal("media",provider)
        store=AndroidCsvImportStore(context)
    }
    private fun staged()=temporary.newFile().apply { writeText("complete CSV bytes") }
    @Test fun modernImportIsPendingUntilCompleteAndKeepsExactBytes() {
        assertEquals("trip.csv",store.publish("trip.csv",staged()))
        assertTrue(provider.createdPending)
        assertEquals(0,provider.values!!.getAsInteger(MediaStore.Downloads.IS_PENDING))
        assertEquals("complete CSV bytes",provider.saved!!.readText())
    }
    @Test fun failingToOpenOutputRollsBackOnlyTheNewPendingItem() {
        provider.failWrite=true
        assertThrows(FileNotFoundException::class.java) { store.publish("trip.csv",staged()) }
        assertTrue(provider.deleted); assertFalse(provider.saved!!.exists())
    }
    @Test fun publicationFailureRollsBackTheNewItem() {
        provider.failPublish=true
        assertThrows(IllegalStateException::class.java) { store.publish("trip.csv",staged()) }
        assertTrue(provider.deleted); assertFalse(provider.saved!!.exists())
    }
    @Test @Config(sdk=[28]) fun legacyImportDoesNotAppendToOrOverwriteAnExistingFile() {
        val name="legacy-"+java.util.UUID.randomUUID()+".csv"
        val directory=File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            com.cooper.wheellog.utils.Constants.LOG_FOLDER_NAME+"/manual")
        val output=File(directory,name)
        try {
            assertEquals(name,store.publish(name,staged()))
            assertEquals("complete CSV bytes",output.readText())
            assertThrows(IllegalStateException::class.java) { store.publish(name,staged().apply { writeText("different") }) }
            assertEquals("complete CSV bytes",output.readText())
        } finally { output.delete() }
    }
    @Test @Config(sdk=[28]) fun legacyReadFailureRemovesOnlyTheNewIncompleteFile() {
        val name="legacy-"+java.util.UUID.randomUUID()+".csv"
        val directory=File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            com.cooper.wheellog.utils.Constants.LOG_FOLDER_NAME+"/manual")
        val missing=temporary.newFile().apply { delete() }
        assertThrows(FileNotFoundException::class.java) { store.publish(name,missing) }
        assertFalse(File(directory,name).exists())
    }
    class DownloadsProvider:ContentProvider() {
        lateinit var directory:File
        var saved:File?=null
        var values:ContentValues?=null
        var createdPending=false
        var deleted=false
        var failWrite=false
        var failPublish=false
        override fun onCreate()=true
        override fun getType(uri:Uri)="text/csv"
        override fun insert(uri:Uri,values:ContentValues?):Uri {
            this.values=ContentValues(values!!)
            createdPending=values.getAsInteger(MediaStore.Downloads.IS_PENDING)==1
            saved=File(directory,"data")
            return Uri.parse("content://media/external_primary/downloads/1")
        }
        override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor =
            MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf(values!!.getAsString(MediaStore.Downloads.DISPLAY_NAME))) }
        override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor {
            if(failWrite) throw FileNotFoundException()
            return ParcelFileDescriptor.open(saved!!,ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
        }
        override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int {
            if(failPublish)return 0
            this.values!!.putAll(values!!);return 1
        }
        override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?):Int {
            deleted=true;saved?.delete();return 1
        }
    }
}
