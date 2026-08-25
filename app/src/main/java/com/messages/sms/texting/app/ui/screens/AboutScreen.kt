package com.messages.sms.texting.app.ui.screens

import com.messages.sms.texting.app.navigation.popBackStackWithAd

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.messages.sms.texting.app.R
import com.messages.sms.texting.app.navigation.Routes
import com.messages.sms.texting.app.ui.components.SecondaryTopBar
import com.messages.sms.texting.app.ui.theme.Inter

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

    val strAboutMessagesLabel = stringResource(R.string.about_messages_label)
    val strAppName = stringResource(R.string.app_name)
    val strCurrentVersionTemplate = stringResource(R.string.current_version_template)
    val strAboutAppDescription = stringResource(R.string.about_app_description)
    val strTermsConditionsLabel = stringResource(R.string.terms_conditions_label)

    val versionName = remember { getAppVersionName(context) }
    // The launcher icon is an adaptive icon (background+foreground layers), which painterResource()
    // can't decode directly — fetch it via PackageManager instead, which composites it properly.
    val appIconBitmap = remember {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.bg_primary),
        topBar = {
            SecondaryTopBar(
                title = strAboutMessagesLabel,
                onBackClick = { navController.popBackStackWithAd() },
                actions = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                bitmap = appIconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strAppName,
                fontSize = 22.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.text_title),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = String.format(strCurrentVersionTemplate, versionName),
                fontSize = 14.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strAboutAppDescription,
                fontSize = 14.sp,
                fontFamily = Inter,
                color = colorResource(R.color.text_des),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = strTermsConditionsLabel,
                fontSize = 16.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.primary),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { navController.navigate(Routes.LegalWebView.createRoute("terms")) }
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Reads the real installed version name (not a hardcoded literal, so it can't drift from the actual build). */
fun getAppVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
}
