package com.messages.sms.texting.app.mms

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * A custom exported ContentProvider used exclusively by the system MmsService
 * to read/write MMS PDU files. We cannot use the standard FileProvider because
 * it is strictly exported="false", which blocks MmsService from accessing the files.
 */
class MmsFileProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "application/vnd.wap.mms-message"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val file = getFile(uri) ?: return 0
        return if (file.exists() && file.delete()) 1 else 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = getFile(uri) ?: return null
        
        if (!file.exists()) {
            if (mode.contains("w") || mode.contains("t")) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
        }
        
        val pfdMode = if (mode.contains("w")) {
            ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
        } else {
            ParcelFileDescriptor.MODE_READ_ONLY
        }
        
        return ParcelFileDescriptor.open(file, pfdMode)
    }

    /**
     * Resolves [uri] to a file strictly inside cacheDir/mms_in or cacheDir/mms_out — this
     * provider is exported=true (required for MmsService to reach it), so any other app on the
     * device can craft an arbitrary content:// URI against it. A plain path.startsWith() check
     * (the previous implementation) doesn't stop a path like "/mms_out/../../../../data/data/..."
     * — it still starts with "/mms_out/" but resolves outside that directory once "../" segments
     * are followed, letting a malicious caller read/write/delete files elsewhere. Using only
     * File(path).name — the final path segment — collapses any such traversal down to a bare
     * filename, so the resolved file can never escape its intended directory.
     */
    private fun getFile(uri: Uri): File? {
        val path = uri.path ?: return null
        val baseDirName = when {
            path.startsWith("/mms_in/") -> "mms_in"
            path.startsWith("/mms_out/") -> "mms_out"
            else -> return null
        }
        val fileName = File(path).name
        if (fileName.isBlank() || fileName == "." || fileName == "..") return null
        return File(File(context?.cacheDir, baseDirName), fileName)
    }
}
