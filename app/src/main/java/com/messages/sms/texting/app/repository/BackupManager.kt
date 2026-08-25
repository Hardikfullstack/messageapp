package com.messages.sms.texting.app.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Telephony
import com.messages.sms.texting.app.model.BackupGroup
import com.messages.sms.texting.app.model.BackupGroupMessage
import com.messages.sms.texting.app.model.BackupMessage
import com.messages.sms.texting.app.model.BackupPayload
import com.messages.sms.texting.app.model.Group
import com.messages.sms.texting.app.model.GroupMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * [uri] is set on API 29+ (backup lives in the public Download/<AppName>/Backup folder via MediaStore).
 * [file] is set below API 29 (direct File I/O, needs WRITE_EXTERNAL_STORAGE).
 */
data class BackupFileInfo(
    val displayName: String,
    val uri: Uri?,
    val file: File?,
    val createdAt: Long,
    val messageCount: Int,
    val sizeBytes: Long
)

data class RestoreResult(
    val messagesRestored: Int,
    val groupsRestored: Int
)

class BackupManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("messages_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val groupDao = AppDatabase.getDatabase(context).groupDao()
    private val groupMessageDao = AppDatabase.getDatabase(context).groupMessageDao()

    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val appName = context.applicationInfo.loadLabel(context.packageManager).toString().ifBlank { "Messages" }
    private val safeAppName = appName.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Messages" }
    private val relativeBackupPath = "Download/$safeAppName/Backup/"

    fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun getBackupFolderDisplayPath(): String = relativeBackupPath.trimEnd('/')

    fun getLastBackupTimestamp(): Long? {
        val time = prefs.getLong("last_backup_time", -1L)
        return if (time > 0) time else null
    }

    private fun legacyBackupsDir(): File {
        val dir = File(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), safeAppName),
            "Backup"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun createBackup(): Result<BackupFileInfo> = withContext(Dispatchers.IO) {
        try {
            val messages = readAllSmsFromProvider()
            val groups = readAllGroupsForBackup()
            val createdAt = System.currentTimeMillis()
            val payload = BackupPayload(
                appName = appName,
                createdAt = createdAt,
                messageCount = messages.size,
                messages = messages,
                groups = groups
            )
            val jsonString = json.encodeToString(BackupPayload.serializer(), payload)

            val fileName = "${safeAppName}_Backup_${fileNameDateFormat.format(createdAt)}_${messages.size}msgs.json"

            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, relativeBackupPath)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Could not create backup file")
                context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }

                BackupFileInfo(
                    displayName = fileName,
                    uri = uri,
                    file = null,
                    createdAt = createdAt,
                    messageCount = messages.size,
                    sizeBytes = jsonString.toByteArray().size.toLong()
                )
            } else {
                val file = File(legacyBackupsDir(), fileName)
                file.writeText(jsonString)

                BackupFileInfo(
                    displayName = fileName,
                    uri = null,
                    file = file,
                    createdAt = createdAt,
                    messageCount = messages.size,
                    sizeBytes = file.length()
                )
            }

            prefs.edit().putLong("last_backup_time", createdAt).apply()
            Result.success(info)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun listBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listBackupsMediaStore()
        } else {
            listBackupsLegacy()
        }
    }

    private fun listBackupsMediaStore(): List<BackupFileInfo> {
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(relativeBackupPath, "%.json")

        val results = mutableListOf<BackupFileInfo>()
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Downloads.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: continue
                val size = cursor.getLong(sizeIndex)
                val dateAddedSeconds = cursor.getLong(dateIndex)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val count = extractMessageCountFromName(name) ?: 0

                results.add(
                    BackupFileInfo(
                        displayName = name,
                        uri = uri,
                        file = null,
                        createdAt = if (dateAddedSeconds > 0) dateAddedSeconds * 1000 else System.currentTimeMillis(),
                        messageCount = count,
                        sizeBytes = size
                    )
                )
            }
        }
        return results
    }

    private fun listBackupsLegacy(): List<BackupFileInfo> {
        val dir = legacyBackupsDir()
        val files = dir.listFiles { f -> f.isFile && f.extension == "json" } ?: emptyArray()

        return files.mapNotNull { file ->
            try {
                val messageCount = extractMessageCountFromName(file.name) ?: run {
                    json.decodeFromString(BackupPayload.serializer(), file.readText()).messageCount
                }
                BackupFileInfo(
                    displayName = file.name,
                    uri = null,
                    file = file,
                    createdAt = file.lastModified(),
                    messageCount = messageCount,
                    sizeBytes = file.length()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.sortedByDescending { it.createdAt }
    }

    private fun extractMessageCountFromName(name: String): Int? {
        return Regex("_(\\d+)msgs\\.json$").find(name)?.groupValues?.get(1)?.toIntOrNull()
    }

    suspend fun deleteBackup(info: BackupFileInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            when {
                info.uri != null -> context.contentResolver.delete(info.uri, null, null) > 0
                info.file != null -> info.file.delete()
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(info: BackupFileInfo): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val jsonString = readBackupContent(info)
                ?: return@withContext Result.failure(IllegalStateException("Could not read backup file"))
            val payload = json.decodeFromString(BackupPayload.serializer(), jsonString)

            val existingKeys = readAllSmsFromProvider()
                .mapTo(HashSet()) { messageKey(it.address, it.body, it.date) }
            val threadIdCache = HashMap<String, Long?>()

            var restoredCount = 0
            for (msg in payload.messages) {
                val key = messageKey(msg.address, msg.body, msg.date)
                if (existingKeys.contains(key)) continue

                val threadId = threadIdCache.getOrPut(msg.address) {
                    try {
                        Telephony.Threads.getOrCreateThreadId(context, msg.address)
                    } catch (e: Exception) {
                        null
                    }
                }

                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, msg.address)
                    put(Telephony.Sms.BODY, msg.body)
                    put(Telephony.Sms.DATE, msg.date)
                    put(Telephony.Sms.READ, if (msg.read) 1 else 0)
                    put(Telephony.Sms.TYPE, msg.type)
                    if (threadId != null) put(Telephony.Sms.THREAD_ID, threadId)
                }

                val targetUri = if (msg.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                    Telephony.Sms.Sent.CONTENT_URI
                } else {
                    Telephony.Sms.Inbox.CONTENT_URI
                }

                val insertedUri = context.contentResolver.insert(targetUri, values)
                if (insertedUri != null) {
                    restoredCount++
                    existingKeys.add(key)
                }
            }

            val groupsRestoredCount = restoreGroups(payload.groups)

            Result.success(RestoreResult(messagesRestored = restoredCount, groupsRestored = groupsRestoredCount))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun readAllGroupsForBackup(): List<BackupGroup> {
        val groups = groupDao.getAllGroups().first()
        return groups.map { g ->
            val messages = groupMessageDao.getMessagesForGroup(g.id).first()
            BackupGroup(
                name = g.name,
                avatarUri = g.avatarUri,
                members = g.members,
                createdAt = g.createdAt,
                isArchived = g.isArchived,
                isPinned = g.isPinned,
                messages = messages.map { BackupGroupMessage(body = it.body, date = it.date) }
            )
        }
    }

    private fun groupSignature(name: String, addresses: List<String>): String {
        return "$name|${addresses.sorted().joinToString(",")}"
    }

    private suspend fun restoreGroups(backupGroups: List<BackupGroup>): Int {
        if (backupGroups.isEmpty()) return 0

        val existingSignatures = groupDao.getAllGroups().first()
            .mapTo(HashSet()) { groupSignature(it.name, it.members.map { m -> m.address }) }

        var restoredCount = 0
        for (bg in backupGroups) {
            val signature = groupSignature(bg.name, bg.members.map { it.address })
            if (existingSignatures.contains(signature)) continue

            val newGroupId = groupDao.insert(
                Group(
                    name = bg.name,
                    avatarUri = bg.avatarUri,
                    members = bg.members,
                    createdAt = bg.createdAt,
                    isArchived = bg.isArchived,
                    isPinned = bg.isPinned
                )
            )
            for (m in bg.messages) {
                groupMessageDao.insert(GroupMessage(groupId = newGroupId, body = m.body, date = m.date))
            }
            restoredCount++
            existingSignatures.add(signature)
        }
        return restoredCount
    }

    private fun readBackupContent(info: BackupFileInfo): String? {
        return when {
            info.uri != null -> context.contentResolver.openInputStream(info.uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
            info.file != null -> info.file.readText()
            else -> null
        }
    }

    private fun messageKey(address: String, body: String, date: Long): String {
        return "$address|$date|${body.hashCode()}"
    }

    private fun readAllSmsFromProvider(): List<BackupMessage> {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        val messages = mutableListOf<BackupMessage>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        cursor?.use {
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                messages.add(
                    BackupMessage(
                        address = it.getString(addressIndex) ?: "",
                        body = it.getString(bodyIndex) ?: "",
                        date = it.getLong(dateIndex),
                        read = it.getInt(readIndex) == 1,
                        type = it.getInt(typeIndex)
                    )
                )
            }
        }
        return messages
    }
}
