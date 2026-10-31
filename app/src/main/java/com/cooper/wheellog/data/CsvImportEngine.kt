package com.cooper.wheellog.data

import java.io.File
import java.io.InputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException

data class CsvImportSource(val name: String, val open: () -> InputStream)
data class CsvImportProgress(val done: Int, val total: Int, val indexing: Boolean = false)
data class CsvImportResult(val added: Int, val existing: Int, val errors: List<String>, val uncheckedExisting: Int = 0)
interface CsvImportStore {
    fun existing(): List<CsvImportSource>
    /** Returns the actual new filename; must not overwrite any existing file. */
    fun publish(name: String, staged: File): String
}

/** Streaming comparison and staging keep batch memory independent of the CSV sizes. */
class CsvImportEngine(private val temporary: File, private val store: CsvImportStore) {
    private data class Fingerprint(val bytes: Long, val sha256: String)
    fun run(sources: List<CsvImportSource>, checkCancelled: () -> Unit = {},
        progress: (CsvImportProgress) -> Unit = {}): CsvImportResult {
        check(temporary.isDirectory || temporary.mkdirs())
        val known = mutableSetOf<Fingerprint>()
        val names = mutableSetOf<String>()
        var unchecked = 0
        progress(CsvImportProgress(0,sources.size,true))
        for (source in store.existing()) {
            checkCancelled()
            names.add(source.name.lowercase(Locale.ROOT))
            try { source.open().use { known.add(copy(it,null,checkCancelled)) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { unchecked++ }
        }
        var added = 0
        var existing = 0
        val errors = mutableListOf<String>()
        sources.forEachIndexed { index, source ->
            checkCancelled()
            var staged: File? = null
            try {
                staged = File.createTempFile("csv-import-", ".part", temporary)
                val fingerprint = source.open().use { copy(it,staged,checkCancelled) }
                if (!validLog(staged,checkCancelled)) throw IOException("Not a WheelLog CSV")
                if (fingerprint in known) existing++
                else {
                    val name = uniqueName(source.name,names)
                    val saved = store.publish(name,staged)
                    names.add(saved.lowercase(Locale.ROOT)); known.add(fingerprint); added++
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { errors.add(source.name) }
            finally { staged?.delete() }
            progress(CsvImportProgress(index+1,sources.size))
        }
        return CsvImportResult(added,existing,errors,unchecked)
    }
    private fun copy(input: InputStream, target: File?, checkCancelled: () -> Unit): Fingerprint {
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        val output = target?.outputStream()
        try {
            val buffer = ByteArray(64 * 1024)
            while (true) {
                checkCancelled()
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                output?.write(buffer,0,count); digest.update(buffer,0,count); bytes += count
            }
            output?.flush()
        } finally { output?.close() }
        return Fingerprint(bytes,digest.digest().joinToString("") { "%02x".format(it) })
    }
    private fun validLog(file: File, checkCancelled: () -> Unit): Boolean = file.bufferedReader().use { reader ->
        val columns = (reader.readLine() ?: return false).trimStart('\uFEFF').split(',').map { it.lowercase(Locale.ROOT) }
        if (!columns.containsAll(listOf("date","time","speed"))) return false
        val date = columns.indexOf("date"); val time = columns.indexOf("time")
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).apply { isLenient = false }
        while (true) {
            checkCancelled()
            val values = (reader.readLine() ?: break).split(',')
            if (values.size >= columns.size && runCatching { format.parse(values[date]+" "+values[time]) }.getOrNull() != null) return true
        }
        false
    }
    companion object {
        fun uniqueName(original: String, used: Set<String>): String {
            val leaf = original.replace('\\','/').substringAfterLast('/')
            val safe = leaf.map { if (it.isISOControl() || it in ":*?\"<>|") '_' else it }.joinToString("").trim(' ','.')
            var stem = (if (safe.endsWith(".csv",true)) safe.dropLast(4) else safe).take(100).ifBlank { "WheelLog" }
            if (stem.startsWith("RAW",true)) stem = "import_"+stem
            var name = stem+".csv"; var index = 1
            while (name.lowercase(Locale.ROOT) in used) { name = stem+" ("+index+").csv"; index++ }
            return name
        }
    }
}
