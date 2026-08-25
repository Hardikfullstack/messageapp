package com.messages.sms.texting.app.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.net.Uri
import androidx.core.content.ContextCompat
import com.messages.sms.texting.app.model.SmsMessage
import com.messages.sms.texting.app.utils.AnalyticsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.paging.PagingSource

class SmsRepository(private val context: Context) {

    private val smsDao = AppDatabase.getDatabase(context).smsDao()
    private val scheduledMessageDao = AppDatabase.getDatabase(context).scheduledMessageDao()
    private val blockedContactDao = AppDatabase.getDatabase(context).blockedContactDao()
    private val draftDao = AppDatabase.getDatabase(context).draftDao()
    private val groupDao = AppDatabase.getDatabase(context).groupDao()
    private val groupMessageDao = AppDatabase.getDatabase(context).groupMessageDao()
    private val reminderDao = AppDatabase.getDatabase(context).reminderDao()

    private val prefs = context.getSharedPreferences("messages_prefs", Context.MODE_PRIVATE)

    fun isDropMessagesEnabled(): Boolean {
        return prefs.getBoolean("drop_messages_enabled", false)
    }

    fun setDropMessagesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("drop_messages_enabled", enabled).apply()
    }

    suspend fun blockContact(address: String, contactName: String?) = withContext(Dispatchers.IO) {
        blockedContactDao.insert(com.messages.sms.texting.app.model.BlockedContact(address, contactName))
        smsDao.updateIsBlockedStatus(address, true)
        AnalyticsManager.logEventWithAction("chat_blocked", "Chat", "block")
    }

    suspend fun blockContacts(contacts: List<com.messages.sms.texting.app.model.BlockedContact>) = withContext(Dispatchers.IO) {
        blockedContactDao.insertAll(contacts)
        smsDao.updateIsBlockedStatuses(contacts.map { it.address }, true)
        AnalyticsManager.logEventWithAction("chat_blocked", "Home", "block", mapOf("count" to contacts.size))
    }

    suspend fun isAddressBlockedSuspend(address: String): Boolean = withContext(Dispatchers.IO) {
        blockedContactDao.isBlocked(address)
    }

    suspend fun unblockContact(address: String) = withContext(Dispatchers.IO) {
        blockedContactDao.deleteByAddress(address)
        smsDao.updateIsBlockedStatus(address, false)
        AnalyticsManager.logEventWithAction("chat_blocked", "BlockedMessages", "unblock")
    }

    suspend fun isContactBlocked(address: String): Boolean = withContext(Dispatchers.IO) {
        blockedContactDao.isBlocked(address)
    }

    fun isContactBlockedFlow(address: String): Flow<Boolean> {
        return blockedContactDao.isBlockedFlow(address)
    }

    fun getBlockedContactsFlow() = blockedContactDao.getAllBlockedContactsFlow()

    fun getBlockedContactsWithLatestMessageFlow() = blockedContactDao.getBlockedContactsWithLatestMessageFlow()

    fun getMessagesPagingSource(category: Int = 0, startDate: Long = 0L, endDate: Long = Long.MAX_VALUE): PagingSource<Int, SmsMessage> {
        return if (category == 0) {
            smsDao.getAllMessagesFlow(startDate, endDate)
        } else {
            smsDao.getMessagesByCategoryFlow(category, startDate, endDate)
        }
    }

    suspend fun getMessagesList(category: Int = 0, startDate: Long = 0L, endDate: Long = Long.MAX_VALUE): List<SmsMessage> {
        return if (category == 0) {
            smsDao.getAllMessagesList(startDate, endDate)
        } else {
            smsDao.getMessagesByCategoryList(category, startDate, endDate)
        }
    }

    fun getArchivedMessagesPagingSource(): PagingSource<Int, SmsMessage> {
        return smsDao.getArchivedMessagesFlow()
    }

    private var smsObserver: ContentObserver? = null

    fun setupSmsObserver(onChanged: () -> Unit) {
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onChanged()
            }
        }
        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )
    }

    suspend fun sendSms(address: String, body: String, threadId: Long, contactName: String?, delayMillis: Long = 0) = withContext(Dispatchers.IO) {
        // Negative IDs can never collide with a real Telephony provider row id (always positive),
        // so this placeholder is unambiguous and safe to delete once the real send completes.
        val placeholderId = -System.currentTimeMillis()

        if (delayMillis > 0) {
            val pendingMsg = SmsMessage(
                id = placeholderId,
                threadId = threadId,
                address = address,
                body = body,
                date = System.currentTimeMillis(),
                read = true,
                type = Telephony.Sms.MESSAGE_TYPE_SENT,
                isPinned = false,
                unreadCount = 0,
                category = 0,
                contactName = contactName,
                photoUri = null,
                isArchived = false,
                sendStatus = SmsMessage.STATUS_DELAYED,
                delayedUntilMillis = System.currentTimeMillis() + delayMillis
            )
            smsDao.insertMessages(listOf(pendingMsg))
            kotlinx.coroutines.delay(delayMillis)
        }

        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }

            // Write to Telephony Provider first so we have the real row id to correlate
            // the carrier delivery-report broadcast back to this specific message.
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.THREAD_ID, threadId)
            }
            val uri = context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            val realId = uri?.lastPathSegment?.toLongOrNull() ?: System.currentTimeMillis()

            val deliveryIntent = if (com.messages.sms.texting.app.ui.theme.DeliveryConfirmationState.enabled.value) {
                val intent = android.content.Intent(context, com.messages.sms.texting.app.receiver.SmsDeliveredReceiver::class.java).apply {
                    data = Uri.parse("smsdelivered://message/$realId")
                }
                android.app.PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            smsManager.sendTextMessage(address, null, body, null, deliveryIntent)

            // Insert locally for instant UI update
            val newMsg = SmsMessage(
                id = realId,
                threadId = threadId,
                address = address,
                body = body,
                date = System.currentTimeMillis(),
                read = true,
                type = Telephony.Sms.MESSAGE_TYPE_SENT,
                isPinned = false,
                unreadCount = 0,
                category = 0,
                contactName = contactName,
                photoUri = null,
                isArchived = false
            )
            if (delayMillis > 0) smsDao.deleteMessages(listOf(placeholderId))
            smsDao.insertMessages(listOf(newMsg))
        } catch (e: Exception) {
            e.printStackTrace()
            if (delayMillis > 0) smsDao.updateSendStatus(placeholderId, SmsMessage.STATUS_FAILED)
        }
    }

    private fun saveMmsImageBitmap(bitmap: android.graphics.Bitmap): java.io.File {
        val dir = java.io.File(context.filesDir, "mms_images").apply { if (!exists()) mkdirs() }
        val file = java.io.File(dir, "mms_${System.currentTimeMillis()}.jpg")

        val mode = com.messages.sms.texting.app.ui.theme.MmsCompressionState.mode.value
        val targetBytes = com.messages.sms.texting.app.ui.theme.MmsCompressionState.targetBytesFor(mode)

        when {
            mode == "none" -> {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
            targetBytes == null -> {
                // "automatic" — sensible default quality, no strict size target.
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                }
            }
            else -> {
                file.writeBytes(compressToTargetSize(bitmap, targetBytes))
            }
        }
        return file
    }

    /** Iteratively lowers JPEG quality, then downscales dimensions if quality alone can't hit the target. */
    private fun compressToTargetSize(bitmap: android.graphics.Bitmap, targetBytes: Int): ByteArray {
        var workingBitmap = bitmap
        var quality = 90
        var bytes: ByteArray

        while (true) {
            val stream = java.io.ByteArrayOutputStream()
            workingBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()

            if (bytes.size <= targetBytes || quality <= 10) {
                if (bytes.size > targetBytes && workingBitmap.width > 200 && workingBitmap.height > 200) {
                    val scaled = android.graphics.Bitmap.createScaledBitmap(
                        workingBitmap,
                        (workingBitmap.width * 0.8f).toInt(),
                        (workingBitmap.height * 0.8f).toInt(),
                        true
                    )
                    if (workingBitmap != bitmap) workingBitmap.recycle()
                    workingBitmap = scaled
                    quality = 90
                    continue
                }
                break
            }
            quality -= 10
        }

        if (workingBitmap != bitmap) workingBitmap.recycle()
        return bytes
    }

    suspend fun sendMms(
        address: String,
        body: String,
        bitmap: android.graphics.Bitmap,
        threadId: Long,
        contactName: String?,
        delayMillis: Long = 0
    ): Long = withContext(Dispatchers.IO) {
        val imageFile = saveMmsImageBitmap(bitmap)
        val localId = System.currentTimeMillis()

        val newMsg = SmsMessage(
            id = localId,
            threadId = threadId,
            address = address,
            body = body,
            date = System.currentTimeMillis(),
            read = true,
            type = Telephony.Sms.MESSAGE_TYPE_SENT,
            isPinned = false,
            unreadCount = 0,
            category = 0,
            contactName = contactName,
            photoUri = null,
            isArchived = false,
            isMms = true,
            mmsImagePath = imageFile.absolutePath,
            sendStatus = if (delayMillis > 0) SmsMessage.STATUS_DELAYED else SmsMessage.STATUS_SENDING,
            delayedUntilMillis = if (delayMillis > 0) System.currentTimeMillis() + delayMillis else null
        )
        smsDao.insertMessages(listOf(newMsg))

        if (delayMillis > 0) {
            kotlinx.coroutines.delay(delayMillis)
            smsDao.updateSendStatus(localId, SmsMessage.STATUS_SENDING)
        }

        val success = try {
            val imageBytes = imageFile.readBytes()
            com.messages.sms.texting.app.mms.MmsSender.send(context, address, body.ifBlank { null }, imageBytes, "image/jpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

        smsDao.updateSendStatus(localId, if (success) SmsMessage.STATUS_NONE else SmsMessage.STATUS_FAILED)
        localId
    }

    suspend fun insertIncomingMms(
        address: String,
        body: String,
        imageBytes: ByteArray?,
        imageMimeType: String?,
        threadId: Long,
        isRead: Boolean
    ) = withContext(Dispatchers.IO) {
        val imagePath = imageBytes?.let { bytes ->
            val dir = java.io.File(context.filesDir, "mms_images").apply { if (!exists()) mkdirs() }
            val ext = if (imageMimeType?.contains("png") == true) "png" else "jpg"
            val file = java.io.File(dir, "mms_in_${System.currentTimeMillis()}.$ext")
            file.writeBytes(bytes)
            file.absolutePath
        }

        val knownContacts = loadKnownContacts()
        val normalizedAddress = normalizedPhoneOrNull(address)
        var contactName: String? = null
        if (normalizedAddress != null) {
            for ((number, info) in knownContacts) {
                if (number.endsWith(normalizedAddress) || normalizedAddress.endsWith(number)) {
                    contactName = info.name
                    break
                }
            }
        }

        val newMsg = SmsMessage(
            id = System.currentTimeMillis(),
            threadId = threadId,
            address = address,
            body = body,
            date = System.currentTimeMillis(),
            read = isRead,
            type = Telephony.Sms.MESSAGE_TYPE_INBOX,
            isPinned = false,
            unreadCount = 0,
            category = determineCategory(address, body, contactName != null),
            contactName = contactName,
            photoUri = null,
            isArchived = false,
            isMms = imagePath != null,
            mmsImagePath = imagePath,
            sendStatus = SmsMessage.STATUS_NONE
        )
        smsDao.insertMessages(listOf(newMsg))
    }

    suspend fun countMessagesOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        smsDao.getMessageIdsOlderThan(cutoff).size
    }

    suspend fun purgeMessagesOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        val ids = smsDao.getMessageIdsOlderThan(cutoff)
        if (ids.isNotEmpty()) {
            deleteMessages(ids)
        }
        ids.size
    }

    suspend fun markSmsDelivered(id: Long) = withContext(Dispatchers.IO) {
        smsDao.setDeliveredAt(id, System.currentTimeMillis())
    }

    suspend fun retrySms(id: Long) = withContext(Dispatchers.IO) {
        val msg = smsDao.getMessageById(id) ?: return@withContext
        smsDao.deleteMessages(listOf(id))
        sendSms(msg.address, msg.body, msg.threadId, msg.contactName)
    }

    suspend fun retryMms(id: Long) = withContext(Dispatchers.IO) {
        val msg = smsDao.getMessageById(id) ?: return@withContext
        val imagePath = msg.mmsImagePath ?: return@withContext
        val imageFile = java.io.File(imagePath)
        if (!imageFile.exists()) {
            smsDao.updateSendStatus(id, SmsMessage.STATUS_FAILED)
            return@withContext
        }

        smsDao.updateSendStatus(id, SmsMessage.STATUS_SENDING)
        val success = try {
            com.messages.sms.texting.app.mms.MmsSender.send(context, msg.address, msg.body.ifBlank { null }, imageFile.readBytes(), "image/jpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        smsDao.updateSendStatus(id, if (success) SmsMessage.STATUS_NONE else SmsMessage.STATUS_FAILED)
    }

    fun searchContacts(query: String): Flow<List<SmsMessage>> {
        return smsDao.searchContacts(query)
    }

    fun searchMessageContent(query: String): Flow<List<com.messages.sms.texting.app.model.MessageSearchResult>> {
        return smsDao.searchMessageContent(query)
    }

    data class ContactInfo(val name: String, val photoUri: String?)

    suspend fun performSync() = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext
        }

        val dbCount = smsDao.getMessageCount()
        val limit = if (dbCount > 0) 500 else 10000

        val messages = mutableListOf<SmsMessage>()
        val unreadCounts = mutableMapOf<Long, Int>()

        // 1. Get unread counts by thread
        val unreadCursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.READ} = 0",
            null,
            null
        )
        
        unreadCursor?.use {
            val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (it.moveToNext()) {
                val tId = it.getLong(threadIdIndex)
                unreadCounts[tId] = (unreadCounts[tId] ?: 0) + 1
            }
        }

        // Load all contacts into a hashset for fast lookup
        val knownContacts = loadKnownContacts()
        val localStates = smsDao.getLocalStates().associateBy { it.id }
        val deletedIds = smsDao.getDeletedMessageIds().toSet()
        val blockedAddresses = blockedContactDao.getAllBlockedAddresses().toSet()
        val dropMessages = isDropMessagesEnabled()

        // Sync deletions from other apps
        val systemIds = mutableSetOf<Long>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            null,
            null,
            null
        )?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            while (it.moveToNext()) {
                systemIds.add(it.getLong(idIndex))
            }
        }

        // MMS messages live only in our local Room table (not in the system SMS provider),
        // so they must be excluded here or every sync would treat them as "deleted from system".
        val localIds = localStates.values.filterNot { it.isMms }.map { it.id }.toSet()
        val deletedFromSystem = localIds - systemIds

        // Guard against a provider query that comes back empty/incomplete (e.g. right after a
        // permission grant, before the provider is fully ready) being misread as "every message
        // was deleted" — only trust this reconciliation when the system actually reported rows.
        if (deletedFromSystem.isNotEmpty() && systemIds.isNotEmpty()) {
            smsDao.deleteMessages(deletedFromSystem.toList())
        }

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )
        
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT $limit"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            val seenThreads = mutableSetOf<Long>()

            while (it.moveToNext()) {
                val threadId = it.getLong(threadIdIndex)
                
                // Grouping by thread id
                val msgId = it.getLong(idIndex)
                if (!seenThreads.contains(threadId) && !deletedIds.contains(msgId)) {
                    seenThreads.add(threadId)
                    
                    val localState = localStates[msgId]

                    val isRead = localState?.read ?: (it.getInt(readIndex) == 1)
                    val realUnreadCount = unreadCounts[threadId] ?: 0
                    
                    val address = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    
                    val normalizedAddress = normalizedPhoneOrNull(address)
                    var contactName: String? = null
                    var photoUri: String? = null
                    if (normalizedAddress != null) {
                        for ((number, info) in knownContacts) {
                            if (number.endsWith(normalizedAddress) || normalizedAddress.endsWith(number)) {
                                contactName = info.name
                                photoUri = info.photoUri
                                break
                            }
                        }
                    }
                    
                    // Categorization Logic
                    val category = determineCategory(address, body, contactName != null)

                    val isBlocked = blockedAddresses.contains(address)

                    if (isBlocked && dropMessages) {
                        continue
                    }

                    messages.add(
                        SmsMessage(
                            id = msgId,
                            threadId = threadId,
                            address = address,
                            body = body,
                            date = it.getLong(dateIndex),
                            read = isRead,
                            type = it.getInt(typeIndex),
                            isPinned = localState?.isPinned ?: false,
                            unreadCount = if (isRead) 0 else realUnreadCount,
                            category = category,
                            contactName = contactName,
                            photoUri = photoUri,
                            isArchived = localState?.isArchived ?: false,
                            isStarred = localState?.isStarred ?: false,
                            isBlocked = isBlocked,
                            deliveredAtMillis = localState?.deliveredAtMillis
                        )
                    )
                }
            }
        }
        
        if (messages.isNotEmpty()) {
            smsDao.insertMessages(messages)
        }
    }
    
    suspend fun getDbMessageCount(): Int = withContext(Dispatchers.IO) {
        smsDao.getMessageCount()
    }

    suspend fun deleteMessagesByAddress(address: String) = withContext(Dispatchers.IO) {
        val ids = smsDao.getMessageIdsByAddress(address)
        if (ids.isNotEmpty()) {
            try {
                ids.forEach { id ->
                    val uri = Uri.parse("content://sms/$id")
                    context.contentResolver.delete(uri, null, null)
                }
            } catch (e: Exception) {
                // If content provider delete fails, still mark them locally as deleted
            }
            smsDao.deleteMessagesByAddress(address)
            val deletedMsgs = ids.map { com.messages.sms.texting.app.model.DeletedMessage(it) }
            smsDao.insertDeletedMessages(deletedMsgs)
        }
    }

    suspend fun markAsUnread(ids: List<Long>) = withContext(Dispatchers.IO) {
        smsDao.markAsUnread(ids)
    }

    suspend fun markAsRead(ids: List<Long>) = withContext(Dispatchers.IO) {
        smsDao.markAsRead(ids)
    }

    suspend fun markThreadAsRead(threadId: Long) = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply { put(Telephony.Sms.READ, 1) }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )
            smsDao.markThreadAsRead(threadId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setPinned(ids: List<Long>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        if (isPinned) {
            smsDao.pinMessages(ids)
        } else {
            smsDao.unpinMessages(ids)
        }
    }

    suspend fun setArchived(ids: List<Long>, isArchived: Boolean) = withContext(Dispatchers.IO) {
        smsDao.setArchived(ids, isArchived)
        AnalyticsManager.logEventWithAction("chat_archived", "Home", if (isArchived) "archive" else "unarchive", mapOf("count" to ids.size))
    }

    suspend fun isThreadArchived(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        smsDao.isThreadArchived(threadId) ?: false
    }

    suspend fun deleteMessages(ids: List<Long>) = withContext(Dispatchers.IO) {
        try {
            ids.forEach { id ->
                val uri = Uri.parse("content://sms/$id")
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        smsDao.deleteMessages(ids)
        smsDao.insertDeletedMessages(ids.map { com.messages.sms.texting.app.model.DeletedMessage(it) })
    }

    suspend fun markThreadsAsRead(threadIds: List<Long>) = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply { put(Telephony.Sms.READ, 1) }
            threadIds.forEach { threadId ->
                context.contentResolver.update(
                    Telephony.Sms.CONTENT_URI,
                    values,
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                    arrayOf(threadId.toString())
                )
            }
            smsDao.markThreadsAsRead(threadIds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead(category: Int = 0, startDate: Long = 0L, endDate: Long = Long.MAX_VALUE) = withContext(Dispatchers.IO) {
        val threadIds = if (category == 0) {
            smsDao.getAllUnreadThreadIds(startDate, endDate)
        } else {
            smsDao.getUnreadThreadIdsByCategory(category, startDate, endDate)
        }
        
        if (threadIds.isNotEmpty()) {
            markThreadsAsRead(threadIds)
        }
    }

    suspend fun setThreadsPinned(threadIds: List<Long>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        if (isPinned) {
            smsDao.pinThreads(threadIds)
        } else {
            smsDao.unpinThreads(threadIds)
        }
    }

    suspend fun setThreadsArchived(threadIds: List<Long>, isArchived: Boolean) = withContext(Dispatchers.IO) {
        smsDao.setThreadsArchived(threadIds, isArchived)
        AnalyticsManager.logEventWithAction("chat_archived", "Home", if (isArchived) "archive" else "unarchive", mapOf("count" to threadIds.size))
    }

    suspend fun deleteThreads(threadIds: List<Long>) = withContext(Dispatchers.IO) {
        val messageIds = smsDao.getMessageIdsByThreadIds(threadIds)
        try {
            messageIds.forEach { id ->
                val uri = Uri.parse("content://sms/$id")
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        smsDao.deleteThreads(threadIds)
        smsDao.insertDeletedMessages(messageIds.map { com.messages.sms.texting.app.model.DeletedMessage(it) })
    }

    /** Normalizes [address] to bare digits (+ leading "+") for contact matching — returns null if
     * it can't plausibly be a real phone number, so it's never even attempted. Without this,
     * stripping non-digits from an alphanumeric DLT sender ID (e.g. "AD-4MMILE-S") can leave a
     * single stray digit, which then falsely suffix-matches against real contacts' numbers via
     * the endsWith comparison below (two different sender IDs both collapsing to "4" and both
     * matching whichever contact's number happens to end in 4). */
    private fun normalizedPhoneOrNull(address: String): String? {
        if (address.any { it.isLetter() }) return null
        val normalized = address.replace(Regex("[^0-9+]"), "")
        return if (normalized.replace("+", "").length >= 7) normalized else null
    }

    private fun loadKnownContacts(): Map<String, ContactInfo> {
        val contacts = mutableMapOf<String, ContactInfo>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                null
            )
            cursor?.use {
                val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                while (it.moveToNext()) {
                    val rawNum = it.getString(numIndex)
                    val name = it.getString(nameIndex) ?: ""
                    val photo = if (photoIndex != -1) it.getString(photoIndex) else null
                    
                    val info = ContactInfo(name, photo)
                    val normalized = rawNum.replace(Regex("[^0-9+]"), "")
                    if (normalized.isNotBlank()) {
                        contacts[normalized] = info
                        // Also add without country code if it has one (+91)
                        if (normalized.startsWith("+")) {
                            contacts[normalized.substring(3)] = info
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contacts
    }

    private fun determineCategory(address: String, body: String, isKnown: Boolean): Int {
        val lowerBody = body.lowercase()
        
        // 1. Check for OTPs
        if (lowerBody.contains(Regex("\\b(otp|one time password|verification code)\\b"))) {
            return 4 // OTPs
        }
        
        // 2. Check for Transactions
        if (lowerBody.contains(Regex("\\b(debited|credited|txn|rs\\.?\\s*\\d|inr|account|acct|balance)\\b"))) {
            return 3 // Transactions
        }
        
        // 3. Check for Offers
        if (lowerBody.contains(Regex("\\b(offer|offers|discount|sale|cashback|promo|free|coupon|% off)\\b"))) {
            return 5 // Offers
        }
        
        // 4. Known vs Unknown
        val normalizedAddress = normalizedPhoneOrNull(address)
        if (normalizedAddress != null) {
            if (isKnown) {
                return 1 // Known
            } else {
                return 2 // Unknown
            }
        }
        
        // If the address contains letters (e.g., AD-HDFCBK), it's likely a business/unknown
        if (address.contains(Regex("[A-Za-z]"))) {
            return 2 // Unknown / Business
        }

        return 0 // Default Inbox
    }

    suspend fun getContactPhotoUri(address: String): String? = withContext(Dispatchers.IO) {
        val knownContacts = loadKnownContacts()
        val normalizedAddress = normalizedPhoneOrNull(address)
        if (normalizedAddress != null) {
            for ((number, info) in knownContacts) {
                if (number.endsWith(normalizedAddress) || normalizedAddress.endsWith(number)) {
                    return@withContext info.photoUri
                }
            }
        }
        null
    }

    fun getMessagesByThreadId(threadId: Long): Flow<List<SmsMessage>> {
        return smsDao.getMessagesByThreadIdFlow(threadId)
    }

    suspend fun insertMessage(message: SmsMessage) {
        smsDao.insertMessage(message)
    }

    suspend fun syncThreadMessages(threadId: Long) {
        withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                return@withContext
            }
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE
            )
            
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                Telephony.Sms.DEFAULT_SORT_ORDER
            )

            val knownContacts = loadKnownContacts()
            val localStates = smsDao.getLocalStates().associateBy { it.id }
            val deletedIds = smsDao.getDeletedMessageIds().toSet()
            val newMessages = mutableListOf<SmsMessage>()

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
                val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val msgId = it.getLong(idIndex)
                    if (deletedIds.contains(msgId)) continue
                    
                    val tId = it.getLong(threadIdIndex)
                    val localState = localStates[msgId]
                    val isRead = localState?.read ?: (it.getInt(readIndex) == 1)
                    val address = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    
                    val normalizedAddress = normalizedPhoneOrNull(address)
                    var contactName: String? = null
                    var photoUri: String? = null
                    val isKnown = if (normalizedAddress != null) {
                        var found = false
                        for ((number, info) in knownContacts) {
                            if (number.endsWith(normalizedAddress) || normalizedAddress.endsWith(number)) {
                                contactName = info.name
                                photoUri = info.photoUri
                                found = true
                                break
                            }
                        }
                        found
                    } else false

                    val category = determineCategory(address, body, isKnown)
                    val type = it.getInt(typeIndex)

                    val smsMsg = SmsMessage(
                        id = msgId,
                        threadId = tId,
                        address = address,
                        body = body,
                        date = it.getLong(dateIndex),
                        read = isRead,
                        type = type,
                        isPinned = localState?.isPinned ?: false,
                        unreadCount = 0,
                        category = category,
                        contactName = contactName,
                        photoUri = photoUri,
                        isArchived = localState?.isArchived ?: false,
                        isStarred = localState?.isStarred ?: false,
                        deliveredAtMillis = localState?.deliveredAtMillis
                    )
                    newMessages.add(smsMsg)
                }
            }
            if (newMessages.isNotEmpty()) {
                smsDao.insertMessages(newMessages)
            }
        }
    }

    suspend fun toggleStarStatus(ids: List<Long>, isStarred: Boolean) = withContext(Dispatchers.IO) {
        smsDao.updateStarStatus(ids, isStarred)
        AnalyticsManager.logEventWithAction("message_starred", "Chat", if (isStarred) "star" else "unstar", mapOf("count" to ids.size))
    }

    suspend fun getStarredMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }
        
        val starredIds = smsDao.getStarredMessageIds()
        if (starredIds.isEmpty()) return@withContext emptyList()
        
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )
        
        val idList = starredIds.joinToString(",")
        val selection = "${Telephony.Sms._ID} IN ($idList)"
        
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            null,
            "${Telephony.Sms.DATE} DESC"
        )
        
        val knownContacts = loadKnownContacts()
        val localStates = smsDao.getLocalStates().associateBy { it.id }
        val newMessages = mutableListOf<SmsMessage>()
        
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val msgId = it.getLong(idIndex)
                val tId = it.getLong(threadIdIndex)
                val localState = localStates[msgId]
                val isRead = localState?.read ?: (it.getInt(readIndex) == 1)
                val address = it.getString(addressIndex) ?: "Unknown"
                val body = it.getString(bodyIndex) ?: ""
                
                val normalizedAddress = normalizedPhoneOrNull(address)
                var contactName: String? = null
                var photoUri: String? = null
                val isKnown = if (normalizedAddress != null) {
                    var found = false
                    for ((number, info) in knownContacts) {
                        if (number.endsWith(normalizedAddress) || normalizedAddress.endsWith(number)) {
                            contactName = info.name
                            photoUri = info.photoUri
                            found = true
                            break
                        }
                    }
                    found
                } else false

                val category = determineCategory(address, body, isKnown)
                val type = it.getInt(typeIndex)

                val smsMsg = SmsMessage(
                    id = msgId,
                    threadId = tId,
                    address = address,
                    body = body,
                    date = it.getLong(dateIndex),
                    read = isRead,
                    type = type,
                    isPinned = localState?.isPinned ?: false,
                    unreadCount = 0,
                    category = category,
                    contactName = contactName,
                    photoUri = photoUri,
                    isArchived = localState?.isArchived ?: false,
                    isStarred = true
                )
                newMessages.add(smsMsg)
            }
        }
        
        newMessages
    }

    suspend fun getUnreadCountsByCategory(): List<CategoryUnreadCount> {
        // Fallback for one-time fetch
        return emptyList()
    }

    // Scheduled Messages
    suspend fun insertScheduledMessage(message: com.messages.sms.texting.app.model.ScheduledMessage): Long {
        return scheduledMessageDao.insert(message)
    }

    fun getAllScheduledMessages(): Flow<List<com.messages.sms.texting.app.model.ScheduledMessage>> {
        return scheduledMessageDao.getAllScheduledMessages()
    }

    suspend fun updateScheduledMessage(message: com.messages.sms.texting.app.model.ScheduledMessage) {
        scheduledMessageDao.update(message)
    }

    suspend fun getScheduledMessageById(id: Long): com.messages.sms.texting.app.model.ScheduledMessage? {
        return scheduledMessageDao.getMessageById(id)
    }

    suspend fun deleteScheduledMessage(id: Long) {
        scheduledMessageDao.deleteById(id)
    }

    // Reminders
    suspend fun insertReminder(reminder: com.messages.sms.texting.app.model.Reminder): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun getRemindersByAddress(address: String): List<com.messages.sms.texting.app.model.Reminder> {
        return reminderDao.getAllByAddress(address)
    }

    fun getRemindersByAddressFlow(address: String): Flow<List<com.messages.sms.texting.app.model.Reminder>> {
        return reminderDao.getAllByAddressFlow(address)
    }

    suspend fun getReminderById(id: Long): com.messages.sms.texting.app.model.Reminder? {
        return reminderDao.getById(id)
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteById(id)
    }

    // Drafts
    suspend fun getDraft(address: String): com.messages.sms.texting.app.model.Draft? = withContext(Dispatchers.IO) {
        draftDao.getDraft(address)
    }

    suspend fun saveDraft(address: String, threadId: Long, body: String) = withContext(Dispatchers.IO) {
        if (body.isBlank()) {
            draftDao.deleteDraft(address)
        } else {
            draftDao.upsert(com.messages.sms.texting.app.model.Draft(address, threadId, body, System.currentTimeMillis()))
        }
    }

    suspend fun clearDraft(address: String) = withContext(Dispatchers.IO) {
        draftDao.deleteDraft(address)
    }

    fun getAllDraftsFlow(): Flow<List<com.messages.sms.texting.app.model.Draft>> {
        return draftDao.getAllDrafts()
    }

    // Groups (broadcast lists — sends land as individual SMS in each member's own thread)
    suspend fun createGroup(
        name: String,
        avatarUri: String?,
        members: List<com.messages.sms.texting.app.model.GroupMember>
    ): Long = withContext(Dispatchers.IO) {
        groupDao.insert(
            com.messages.sms.texting.app.model.Group(
                name = name,
                avatarUri = avatarUri,
                members = members,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getGroupById(id: Long): com.messages.sms.texting.app.model.Group? = withContext(Dispatchers.IO) {
        groupDao.getGroupById(id)
    }

    fun getGroupByIdFlow(id: Long): Flow<com.messages.sms.texting.app.model.Group?> {
        return groupDao.getGroupByIdFlow(id)
    }

    fun getAllGroupsFlow(): Flow<List<com.messages.sms.texting.app.model.Group>> {
        return groupDao.getAllGroups()
    }

    fun getAllGroupsWithLastMessageFlow(): Flow<List<com.messages.sms.texting.app.model.GroupWithLastMessage>> {
        return groupDao.getAllGroupsWithLastMessage()
    }

    fun searchGroupsByName(query: String): Flow<List<com.messages.sms.texting.app.model.Group>> {
        return groupDao.searchGroupsByName(query)
    }

    fun searchGroupMessageContent(query: String): Flow<List<com.messages.sms.texting.app.model.GroupSearchResult>> {
        return groupDao.searchGroupMessageContent(query)
    }

    suspend fun updateGroup(group: com.messages.sms.texting.app.model.Group) = withContext(Dispatchers.IO) {
        groupDao.update(group)
    }

    suspend fun deleteGroup(id: Long) = withContext(Dispatchers.IO) {
        groupDao.deleteById(id)
        groupMessageDao.deleteMessagesForGroup(id)
    }

    suspend fun setGroupArchived(id: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        groupDao.setArchived(id, isArchived)
    }

    suspend fun setGroupsArchived(ids: List<Long>, isArchived: Boolean) = withContext(Dispatchers.IO) {
        groupDao.setArchivedBulk(ids, isArchived)
    }

    suspend fun setGroupsPinned(ids: List<Long>, isPinned: Boolean) = withContext(Dispatchers.IO) {
        groupDao.setPinnedBulk(ids, isPinned)
    }

    suspend fun deleteGroups(ids: List<Long>) = withContext(Dispatchers.IO) {
        groupDao.deleteByIds(ids)
        for (id in ids) {
            groupMessageDao.deleteMessagesForGroup(id)
        }
    }

    fun getGroupMessagesFlow(groupId: Long): Flow<List<com.messages.sms.texting.app.model.GroupMessage>> {
        return groupMessageDao.getMessagesForGroup(groupId)
    }

    suspend fun sendToGroup(group: com.messages.sms.texting.app.model.Group, body: String, delayMillis: Long = 0) = withContext(Dispatchers.IO) {
        if (delayMillis > 0) {
            val pendingId = groupMessageDao.insert(
                com.messages.sms.texting.app.model.GroupMessage(
                    groupId = group.id,
                    body = body,
                    date = System.currentTimeMillis(),
                    sendStatus = SmsMessage.STATUS_DELAYED,
                    delayedUntilMillis = System.currentTimeMillis() + delayMillis
                )
            )
            kotlinx.coroutines.delay(delayMillis)
            for (member in group.members) {
                try {
                    val threadId = Telephony.Threads.getOrCreateThreadId(context, member.address)
                    sendSms(member.address, body, threadId, member.name)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            groupMessageDao.updateSendStatus(pendingId, SmsMessage.STATUS_NONE)
        } else {
            for (member in group.members) {
                try {
                    val threadId = Telephony.Threads.getOrCreateThreadId(context, member.address)
                    sendSms(member.address, body, threadId, member.name)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            groupMessageDao.insert(
                com.messages.sms.texting.app.model.GroupMessage(
                    groupId = group.id,
                    body = body,
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    fun getUnreadCountsFlow(): Flow<Map<Int, Int>> {
        return combine(
            smsDao.getTotalUnreadCountFlow(),
            smsDao.getUnreadCountsByCategoryFlow()
        ) { total, categoryCounts ->
            val map = mutableMapOf<Int, Int>()
            map[0] = total // 0 is Inbox
            categoryCounts.forEach {
                map[it.category] = it.count
            }
            map
        }
    }
}
