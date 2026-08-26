package com.message.sms.texting.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.message.sms.texting.app.repository.BackupFileInfo
import com.message.sms.texting.app.repository.BackupManager
import com.message.sms.texting.app.repository.RestoreResult
import com.message.sms.texting.app.utils.AnalyticsManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)

    private val _lastBackupTime = MutableStateFlow(backupManager.getLastBackupTimestamp())
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    fun needsLegacyStoragePermission(): Boolean = backupManager.needsLegacyStoragePermission()

    val backupFolderPath: String = backupManager.getBackupFolderDisplayPath()

    /**
     * [minDurationMs] keeps [isBackingUp] true for at least this long, so the caller's
     * loading animation always gets to play at least one full loop even on a fast backup.
     */
    suspend fun performBackup(minDurationMs: Long = 0L): Result<BackupFileInfo> {
        _isBackingUp.value = true
        val result = coroutineScope {
            val deferred = async { backupManager.createBackup() }
            delay(minDurationMs)
            deferred.await()
        }
        result.onSuccess { _lastBackupTime.value = it.createdAt }
        AnalyticsManager.logEventWithAction("backup_created", "BackupRestoreScreen", if (result.isSuccess) "success" else "failed")
        _isBackingUp.value = false
        return result
    }

    suspend fun loadBackups(): List<BackupFileInfo> {
        return backupManager.listBackups()
    }

    suspend fun deleteBackup(info: BackupFileInfo): Boolean {
        return backupManager.deleteBackup(info)
    }

    /**
     * [minDurationMs] keeps [isRestoring] true for at least this long, so the caller's
     * loading animation always gets to play at least one full loop even on a fast restore.
     */
    suspend fun performRestore(info: BackupFileInfo, minDurationMs: Long = 0L): Result<RestoreResult> {
        _isRestoring.value = true
        val result = coroutineScope {
            val deferred = async { backupManager.restoreBackup(info) }
            delay(minDurationMs)
            deferred.await()
        }
        AnalyticsManager.logEventWithAction("backup_restored", "BackupRestoreScreen", if (result.isSuccess) "success" else "failed")
        _isRestoring.value = false
        return result
    }
}
