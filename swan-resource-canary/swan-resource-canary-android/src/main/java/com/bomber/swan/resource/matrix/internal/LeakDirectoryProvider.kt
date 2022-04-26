package com.bomber.swan.resource.matrix.internal

import android.content.Context
import com.bomber.swan.util.SwanLog
import java.io.File
import java.io.FilenameFilter
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author youngtr
 * @data 2022/4/16
 */
internal class LeakDirectoryProvider(
    context: Context,
    private val maxStoredHeapDumps: () -> Int,
    private val rootDirectory: ((String) -> File)? = null,
) {

    private val context: Context = context.applicationContext

    fun listFiles(filter: FilenameFilter): MutableList<File> {

        val files = ArrayList<File>()
        val externalFiles = storageDirectory().listFiles(filter)
        if (externalFiles != null) {
            files.addAll(files)
        }
        return files
    }

    fun newHeapDumpFile(): File? {
        cleanupOldHeapDumps()

        val storageDirectory = storageDirectory()

        if (!directoryWritableAfterMkdirs(storageDirectory)) {
            return null
        }

        val fileName =
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS'.hprof'", Locale.CHINA).format(Date())
        return File(storageDirectory, fileName)
    }

    private fun storageDirectory(): File {
        return rootDirectory?.invoke(RESOURCE_DIRECTORY)
            ?: File(context.cacheDir, RESOURCE_DIRECTORY)

    }

    private fun directoryWritableAfterMkdirs(directory: File): Boolean {
        val success = directory.mkdirs()
        return (success || directory.exists()) && directory.canWrite()
    }


    private fun cleanupOldHeapDumps() {
        val hprofFiles = listFiles { _, name ->
            name.endsWith(
                HPROF_SUFFIX
            )
        }
        val maxStoredHeapDumps = maxStoredHeapDumps()
        if (maxStoredHeapDumps < 1) {
            throw IllegalArgumentException("maxStoredHeapDumps must be at least 1")
        }

        val filesToRemove = hprofFiles.size - maxStoredHeapDumps
        if (filesToRemove > 0) {
            SwanLog.d(TAG, "Removing $filesToRemove heap dumps")
            // Sort with oldest modified first.
            hprofFiles.sortWith { lhs, rhs ->
                java.lang.Long.valueOf(lhs.lastModified())
                    .compareTo(rhs.lastModified())
            }
            for (i in 0 until filesToRemove) {
                val path = hprofFiles[i].absolutePath
                val deleted = hprofFiles[i].delete()
                if (!deleted) {
                    SwanLog.d(TAG, "Could not delete old hprof file ${hprofFiles[i].path}")
                }
                File(path.hprofToJson()).apply {
                    if (exists())
                        delete()
                }
            }
        }
    }

    companion object {
        private const val TAG = "Swan.DirectoryProvider"
        private const val RESOURCE_DIRECTORY = "resource"
    }
}

private const val HPROF_SUFFIX = ".hprof"
private const val JSON_SUFFIX = ".json"

internal fun String.hprofToJson() = replace(
    HPROF_SUFFIX,
    JSON_SUFFIX
)
