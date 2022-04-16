package com.bomber.swan.resource.matrix.internal

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
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
    private val requestExternalStoragePermission: () -> Boolean
) {

    private val context: Context = context.applicationContext

    fun listFiles(filter: FilenameFilter): MutableList<File> {
        if (!hasStoragePermission() && requestExternalStoragePermission()) {
            requestWritePermissionNotification()
        }

        val files = ArrayList<File>()
        val externalFiles = externalStorageDirectory().listFiles(filter)
        if (externalFiles != null) {
            files.addAll(files)
        }

        val appFiles = appStorageDirectory().listFiles(filter)
        if (appFiles != null) {
            files.addAll(appFiles)
        }
        return files
    }

    fun newHeapDumpFile(): File? {
        cleanupOldHeapDumps()

        var storageDirectory = externalStorageDirectory()
        if (!directoryWritableAfterMkdirs(storageDirectory)) {
            if (!hasStoragePermission()) {
                if (requestExternalStoragePermission()) {
                    SwanLog.d(
                        TAG, "WRITE_EXTERNAL_STORAGE permission not granted, requesting"
                    )
                    requestWritePermissionNotification()
                } else {
                    SwanLog.d(TAG, "WRITE_EXTERNAL_STORAGE permission not granted, ignoring")
                }
            } else {
                val state = Environment.getExternalStorageState()
                if (Environment.MEDIA_MOUNTED != state) {
                    SwanLog.d(TAG, "External storage not mounted, state: $state")
                } else {
                    SwanLog.d(
                        TAG,
                        "Could not create heap dump directory in external storage: [${storageDirectory.absolutePath}]"
                    )
                }
            }
            // Fallback to app storage
            storageDirectory = appStorageDirectory()
            if (!directoryWritableAfterMkdirs(storageDirectory)) {
                SwanLog.d(
                    TAG,
                    "Could not create heap dump directory in app storage: [${storageDirectory.absolutePath}]"
                )
                return null
            }
        }

        val fileName =
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS'.hprof'", Locale.CHINA).format(Date())
        return File(storageDirectory, fileName)
    }

    private fun requestWritePermissionNotification() {
        // todo requestPermission

    }

    @Suppress("DEPRECATION")
    private fun externalStorageDirectory(): File {
        val downloadsDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDirectory, "swanresource-" + context.packageName)
    }

    private fun appStorageDirectory(): File {
        val appFilesDirectory = context.cacheDir
        return File(appFilesDirectory, "swanresource")
    }


    @TargetApi(Build.VERSION_CODES.M)
    fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        // Once true, this won't change for the life of the process so we can cache it.
        if (writeExternalStorageGranted) {
            return true
        }
        writeExternalStorageGranted =
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return writeExternalStorageGranted
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
                if (deleted) {
                    filesDeletedTooOld += path
                } else {
                    SwanLog.d(TAG, "Could not delete old hprof file ${hprofFiles[i].path}")
                }
            }
        }
    }


    companion object {

        private const val TAG = "Swan.DirectoryProvider"

        @Volatile
        private var writeExternalStorageGranted: Boolean = false

        @Volatile
        private var permissionNotificationDisplayed: Boolean = false

        private val filesDeletedTooOld = mutableListOf<String>()
        val filesDeletedRemoveLeak = mutableListOf<String>()

        private const val HPROF_SUFFIX = ".hprof"

        fun hprofDeleteReason(file: File): String {
            val path = file.absolutePath
            return when {
                filesDeletedTooOld.contains(path) -> "older than all other hprof files"
                filesDeletedRemoveLeak.contains(path) -> "leak manually removed"
                else -> "unknown"
            }
        }
    }
}