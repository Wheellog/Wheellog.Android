package com.cooper.wheellog.track

import com.cooper.wheellog.data.*
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class CsvImportEngineTest {
    @get:Rule val temporary = TemporaryFolder()
    private val csv = "date,time,speed\n2026-10-29,07:54:49.000,20\n"
    private fun source(name: String, text: String = csv) = CsvImportSource(name) { text.byteInputStream() }
    private inner class Store : CsvImportStore {
        val files = linkedMapOf<String,String>()
        var failNext = false
        var inaccessible = false
        override fun existing() = files.map { (name,text) -> source(name,text) } +
            if (inaccessible) listOf(CsvImportSource("private.csv") { throw IOException() }) else emptyList()
        override fun publish(name: String, staged: File): String {
            if (failNext) { failNext = false; throw IOException() }
            check(name !in files)
            files[name] = staged.readText()
            return name
        }
    }
    private fun run(store: Store, vararg input: CsvImportSource): CsvImportResult =
        CsvImportEngine(temporary.newFolder(),store).run(input.toList())

    @Test fun duplicateDetectionUsesBytesNotFileNamesAndDoesNotModifyOriginals() {
        val store = Store(); store.files["recorded.csv"] = csv
        val result = run(store,source("renamed.csv"),source("recorded.csv"))
        assertEquals(0,result.added); assertEquals(2,result.existing); assertTrue(result.errors.isEmpty())
        assertEquals(mapOf("recorded.csv" to csv),store.files)
    }
    @Test fun duplicatesWithinOneSelectionAndOnARepeatedImportAreSkipped() {
        val store = Store()
        val result = run(store,source("a.csv"),source("b.csv"),source("c.csv"))
        assertEquals(1,result.added); assertEquals(2,result.existing)
        assertEquals(1,store.files.size)
        val repeated = run(store,source("again.csv"))
        assertEquals(0,repeated.added); assertEquals(1,repeated.existing)
    }
    @Test fun equalNamesWithDifferentContentsRemainSeparateAndNeverAppend() {
        val store = Store(); store.files["a.csv"] = csv
        val changed = csv.replace(",20",",30")
        val result = run(store,source("a.csv",changed))
        assertEquals(1,result.added)
        assertEquals(csv,store.files["a.csv"]); assertEquals(changed,store.files["a (1).csv"])
    }
    @Test fun unreadableOrInvalidSelectedFilesDoNotPreventLaterValidLogs() {
        val store = Store()
        val result = run(store,source("empty.csv",""),source("header.csv","date,time,speed\n"),
            source("not-csv.csv","Hello"),CsvImportSource("broken.csv") { throw IOException() },source("good.csv"))
        assertEquals(1,result.added); assertEquals(4,result.errors.size)
        assertEquals(setOf("good.csv"),store.files.keys)
    }
    @Test fun failedPublishIsNotAddedToDuplicateIndexAndLeavesNoTemporaryFile() {
        val store = Store(); store.failNext = true
        val directory = temporary.newFolder()
        val result = CsvImportEngine(directory,store).run(listOf(source("first.csv"),source("second.csv")))
        assertEquals(1,result.added); assertEquals(1,result.errors.size); assertEquals(0,result.existing)
        assertTrue(directory.listFiles()!!.isEmpty())
    }
    @Test fun sourceReadFailureAndCancellationCleanUpTheirStagingFiles() {
        val store = Store(); val directory = temporary.newFolder()
        val interrupted = CsvImportSource("broken.csv") { object : java.io.InputStream() {
            override fun read(): Int = throw IOException()
        } }
        assertEquals(1,CsvImportEngine(directory,store).run(listOf(interrupted)).errors.size)
        assertTrue(directory.listFiles()!!.isEmpty())
        var calls = 0
        assertThrows(CancellationException::class.java) {
            CsvImportEngine(directory,store).run(listOf(source("cancel.csv")), { if (++calls >= 2) throw CancellationException() })
        }
        assertTrue(store.files.isEmpty()); assertTrue(directory.listFiles()!!.isEmpty())
    }
    @Test fun progressCountsEverySelectedFileAndReportsUnreadableExistingFiles() {
        val store = Store(); store.inaccessible = true
        val progress = mutableListOf<CsvImportProgress>()
        val result = CsvImportEngine(temporary.newFolder(),store).run(listOf(source("a.csv"),source("b.csv")),progress={progress.add(it)})
        assertTrue(progress.first().indexing)
        assertEquals(CsvImportProgress(2,2),progress.last())
        assertEquals(1,result.uncheckedExisting)
        assertEquals(1,result.added); assertEquals(1,result.existing)
    }
    @Test fun importedNamesStayInsideManualAndUseALowercaseCsvExtension() {
        assertEquals("trip.csv",CsvImportEngine.uniqueName("../../trip.CSV",emptySet()))
        assertEquals("trip (1).csv",CsvImportEngine.uniqueName("TRIP.CSV",setOf("trip.csv")).lowercase())
        assertTrue(CsvImportEngine.uniqueName("RAW_ride.csv",emptySet()).startsWith("import_"))
        assertFalse(CsvImportEngine.uniqueName("..\\bad:name.csv",emptySet()).contains(":"))
    }
}
