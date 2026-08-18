package com.messages.ui.screens

import com.messages.navigation.popBackStackWithAd

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.messages.R
import com.messages.model.GroupMember
import com.messages.navigation.Routes
import com.messages.ui.components.SecondaryTopBar
import com.messages.ui.theme.Inter
import com.messages.viewmodel.AddGroupNameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AddGroupNameViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddGroupNameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddGroupNameViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

fun copyPickedImageToAppStorage(context: android.content.Context, uri: Uri): String? {
    val bitmap = loadDownsampledBitmap(context, uri, maxDimension = 512) ?: return null
    return try {
        val dir = File(context.filesDir, "group_avatars")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AddGroupNameScreen(
    navController: NavController,
    members: List<GroupMember>
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AddGroupNameViewModel = viewModel(factory = AddGroupNameViewModelFactory(application))
    val coroutineScope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var avatarPath by remember { mutableStateOf<String?>(null) }
    var isLoadingAvatar by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    val strUnableLoadSelectedImage = stringResource(R.string.toast_unable_load_selected_image)
    val strNewGroupTitle = stringResource(R.string.new_group_title)
    val strCreateGroup = stringResource(R.string.content_desc_create_group)
    val strGroupAvatar = stringResource(R.string.content_desc_group_avatar)
    val strAddGroupPhoto = stringResource(R.string.content_desc_add_group_photo)
    val strAddImageLabel = stringResource(R.string.add_image_label)
    val strGroupNamePlaceholder = stringResource(R.string.group_name_placeholder)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                isLoadingAvatar = true
                coroutineScope.launch {
                    val path = withContext(Dispatchers.IO) { copyPickedImageToAppStorage(context, uri) }
                    isLoadingAvatar = false
                    if (path != null) {
                        avatarPath = path
                    } else {
                        Toast.makeText(context, strUnableLoadSelectedImage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = colorResource(R.color.bg_primary),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(if (isCreating) colorResource(R.color.primary).copy(alpha = 0.6f) else colorResource(R.color.primary))
                    .clickable(enabled = !isCreating) {
                        isCreating = true
                        coroutineScope.launch {
                            val finalName = groupName.trim().ifBlank {
                                members.joinToString(", ") { it.name?.takeIf { n -> n.isNotBlank() } ?: it.address }
                                    .ifBlank { strNewGroupTitle }
                            }
                            val id = viewModel.createGroup(finalName, avatarPath, members)
                            navController.navigate(
                                Routes.Chat.createRoute(
                                    threadId = 0L,
                                    address = "",
                                    contactName = null,
                                    groupId = id
                                )
                            ) {
                                popUpTo(Routes.NewChat.route) { inclusive = true }
                            }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = strCreateGroup,
                        color = Color.White,
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SecondaryTopBar(
                title = strNewGroupTitle,
                onBackClick = { navController.popBackStackWithAd() }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                val openImagePicker = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(colorResource(R.color.light_gray))
                            .clickable { openImagePicker() },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoadingAvatar -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                    color = colorResource(R.color.primary)
                                )
                            }
                            avatarPath != null -> {
                                AsyncImage(
                                    model = File(avatarPath!!),
                                    contentDescription = strGroupAvatar,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            else -> {
                                Icon(
                                    painter = painterResource(id = R.drawable.contact_ic_group),
                                    contentDescription = strAddGroupPhoto,
                                    tint = colorResource(R.color.primary),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colorResource(R.color.primary))
                            .clickable { openImagePicker() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chat_ic_camera),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = strAddImageLabel,
                    fontSize = 14.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.primary),
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .clickable { openImagePicker() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .background(colorResource(R.color.light_gray))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (groupName.isEmpty()) {
                            Text(
                                text = strGroupNamePlaceholder,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.1.sp,
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_des),
                                textAlign = TextAlign.Start
                            )
                        }
                        val customTextSelectionColors = TextSelectionColors(
                            handleColor = colorResource(R.color.primary),
                            backgroundColor = colorResource(R.color.primary).copy(alpha = 0.4f)
                        )
                        CompositionLocalProvider(
                            LocalTextSelectionColors provides customTextSelectionColors
                        ) {
                            BasicTextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = colorResource(R.color.text_title),
                                    textAlign = TextAlign.Start
                                ),
                                cursorBrush = SolidColor(colorResource(R.color.primary)),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
