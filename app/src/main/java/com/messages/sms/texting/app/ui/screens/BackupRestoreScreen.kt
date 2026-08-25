package com.messages.sms.texting.app.ui.screens

import com.messages.sms.texting.app.navigation.popBackStackWithAd

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.repository.BackupFileInfo
import com.messages.sms.texting.app.ui.components.SecondaryTopBar
import com.messages.sms.texting.app.ui.components.dialogs.ConfirmationDialog
import com.messages.sms.texting.app.ui.theme.Inter
import com.messages.sms.texting.app.viewmodel.BackupRestoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class BackupRestoreViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupRestoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupRestoreViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private val backupDateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())

private fun formatBackupSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format(Locale.getDefault(), "%.2f MB", mb)
    else String.format(Locale.getDefault(), "%.0f KB", kb)
}

@Composable
fun BackupRestoreScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: BackupRestoreViewModel = viewModel(factory = BackupRestoreViewModelFactory(application))
    val coroutineScope = rememberCoroutineScope()

    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val isBackupOrRestoreActive = isBackingUp || isRestoring

    val backupComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cloud_data_backup))
    val restoreComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cloud_data_restore))
    val activeComposition = if (isRestoring) restoreComposition else backupComposition
    val lottieProgress by animateLottieCompositionAsState(
        composition = activeComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isBackupOrRestoreActive
    )

    var showBackupListDialog by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var isLoadingBackupList by remember { mutableStateOf(false) }

    var backupPendingRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var backupPendingDelete by remember { mutableStateOf<BackupFileInfo?>(null) }

    val strBackupNever = stringResource(R.string.backup_never)
    val strStoragePermissionRequired = stringResource(R.string.toast_storage_permission_required)
    val strBackupRestoreTitle = stringResource(R.string.home_menu_backup_restore)
    val strBackupLabel = stringResource(R.string.backup_label)
    val strRestoreLabel = stringResource(R.string.restore_label)
    val strSelectABackup = stringResource(R.string.select_a_backup)
    val strBackupCreated = stringResource(R.string.toast_backup_created)
    val strBackupFailed = stringResource(R.string.toast_backup_failed)
    val strBackupNowButton = stringResource(R.string.backup_now_button)
    val strDeleteBackupTitle = stringResource(R.string.delete_backup_title)
    val strDeleteBackupText = stringResource(R.string.delete_backup_text)
    val strActionDelete = stringResource(R.string.action_delete)
    val strRestoreFromBackupTitle = stringResource(R.string.restore_from_backup_title)
    val strRestoreFromBackupText = stringResource(R.string.restore_from_backup_text)
    val strActionRestore = stringResource(R.string.action_restore)
    val strRestoreSummaryMessages = stringResource(R.string.restore_summary_messages)
    val strRestoreSummaryGroups = stringResource(R.string.restore_summary_groups)
    val strRestoreSummarySuffix = stringResource(R.string.restore_summary_suffix)
    val strRestoreFailed = stringResource(R.string.toast_restore_failed)

    val backupSubtitle = lastBackupTime?.let { backupDateFormat.format(it) } ?: strBackupNever

    fun refreshBackupList() {
        coroutineScope.launch {
            isLoadingBackupList = true
            backupList = viewModel.loadBackups()
            isLoadingBackupList = false
        }
    }

    // Only needed on API < 29, where public Downloads access requires the legacy permission.
    var pendingStorageAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                pendingStorageAction?.invoke()
            } else {
                Toast.makeText(context, strStoragePermissionRequired, Toast.LENGTH_SHORT).show()
            }
            pendingStorageAction = null
        }
    )

    fun withStoragePermission(action: () -> Unit) {
        val hasPermission = !viewModel.needsLegacyStoragePermission() ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            action()
        } else {
            pendingStorageAction = action
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.bg_primary))
    ) {
        SecondaryTopBar(
            title = strBackupRestoreTitle,
            onBackClick = { navController.popBackStackWithAd() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            LottieAnimation(
                composition = activeComposition,
                progress = { if (isBackupOrRestoreActive) lottieProgress else 0f },
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            BackupRestoreRow(
                iconRes = R.drawable.backup_and_restore_ic_backup,
                title = strBackupLabel,
                subtitle = backupSubtitle,
                onClick = { }
            )

            Spacer(modifier = Modifier.height(12.dp))

            BackupRestoreRow(
                iconRes = R.drawable.backup_and_restore_ic_restore,
                title = strRestoreLabel,
                subtitle = strSelectABackup,
                enabled = !isBackupOrRestoreActive,
                isLoading = isRestoring,
                onClick = {
                    withStoragePermission {
                        showBackupListDialog = true
                        refreshBackupList()
                    }
                }
            )

            Spacer(modifier = Modifier.height(38.dp))

            Button(
                onClick = {
                    withStoragePermission {
                        coroutineScope.launch {
                            val minDurationMs = (backupComposition?.duration ?: 0f).toLong()
                            val result = viewModel.performBackup(minDurationMs)
                            result.onSuccess {
                                Toast.makeText(context, strBackupCreated, Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, strBackupFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isBackupOrRestoreActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary),
                    disabledContainerColor = colorResource(R.color.primary).copy(alpha = 0.6f)
                )
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = strBackupNowButton,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Inter,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showBackupListDialog) {
        BackupListDialog(
            backups = backupList,
            folderPath = viewModel.backupFolderPath,
            isLoading = isLoadingBackupList,
            onSelectBackup = { info ->
                showBackupListDialog = false
                backupPendingRestore = info
            },
            onDeleteBackup = { info -> backupPendingDelete = info },
            onDismiss = { showBackupListDialog = false }
        )
    }

    if (backupPendingDelete != null) {
        val target = backupPendingDelete!!
        ConfirmationDialog(
            title = strDeleteBackupTitle,
            text = strDeleteBackupText,
            confirmText = strActionDelete,
            onConfirm = {
                backupPendingDelete = null
                coroutineScope.launch {
                    viewModel.deleteBackup(target)
                    refreshBackupList()
                }
            },
            onDismiss = { backupPendingDelete = null }
        )
    }

    if (backupPendingRestore != null) {
        val target = backupPendingRestore!!
        ConfirmationDialog(
            title = strRestoreFromBackupTitle,
            text = strRestoreFromBackupText,
            confirmText = strActionRestore,
            onConfirm = {
                backupPendingRestore = null
                coroutineScope.launch {
                    val minDurationMs = (restoreComposition?.duration ?: 0f).toLong()
                    val result = viewModel.performRestore(target, minDurationMs)
                    result.onSuccess { restoreResult ->
                        val summary = buildString {
                            append(String.format(strRestoreSummaryMessages, restoreResult.messagesRestored))
                            if (restoreResult.groupsRestored > 0) {
                                append(String.format(strRestoreSummaryGroups, restoreResult.groupsRestored))
                            }
                            append(strRestoreSummarySuffix)
                        }
                        Toast.makeText(context, summary, Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, strRestoreFailed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { backupPendingRestore = null }
        )
    }
}

@Composable
private fun BackupRestoreRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(colorResource(R.color.light_gray))
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = colorResource(R.color.primary)
            )
        } else {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = colorResource(R.color.text_title)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = Inter,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = colorResource(R.color.text_des)
            )
        }
    }
}

@Composable
private fun BackupListDialog(
    backups: List<BackupFileInfo>,
    folderPath: String,
    isLoading: Boolean,
    onSelectBackup: (BackupFileInfo) -> Unit,
    onDeleteBackup: (BackupFileInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val strRestoreLabel = stringResource(R.string.restore_label)
    val strNoBackupsFound = stringResource(R.string.no_backups_found)
    val strBackupMessagesCountTemplate = stringResource(R.string.backup_messages_count_template)
    val strDeleteBackup = stringResource(R.string.content_desc_delete_backup)
    val strActionCancelCaps = stringResource(R.string.action_cancel_caps)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorResource(R.color.light_gray),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    strRestoreLabel,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = colorResource(R.color.text_title)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    folderPath,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_des)
                )
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp,
                                color = colorResource(R.color.primary)
                            )
                        }
                    }
                    backups.isEmpty() -> {
                        Text(
                            strNoBackupsFound,
                            fontFamily = Inter,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_des),
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(backups, key = { it.displayName }) { info ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectBackup(info) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backupDateFormat.format(info.createdAt),
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 18.sp,
                                            color = colorResource(R.color.text_title)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(strBackupMessagesCountTemplate, info.messageCount, formatBackupSize(info.sizeBytes)),
                                            fontFamily = Inter,
                                            fontSize = 15.sp,
                                            color = colorResource(R.color.text_des)
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.longpress_ic_delete),
                                        contentDescription = strDeleteBackup,
                                        tint = colorResource(R.color.text_title),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { onDeleteBackup(info) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            strActionCancelCaps,
                            color = colorResource(R.color.light_color_gray),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
