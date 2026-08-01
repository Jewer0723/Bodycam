package com.jewer.bodycam.frontend.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.jewer.bodycam.frontend.nav.Navigation
import com.jewer.bodycam.ui.theme.Black
import com.jewer.bodycam.ui.theme.BodycamTheme
import com.jewer.bodycam.ui.theme.DarkYellow
import com.jewer.bodycam.ui.theme.White

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen() {
    val context = LocalContext.current

    val permissionList = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            // 存取媒體權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissionList)

    // 自動啟動一次授權請求
    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }
    
    val allGranted = permissionState.permissions.all { it.status.isGranted }

    if (!allGranted) {
        BodycamTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Permissions Required!",
                    textAlign = TextAlign.Center,
                    color = White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val permanentlyDenied = permissionState.permissions.any { permission ->
                            !permission.status.isGranted && !permission.status.shouldShowRationale
                        }

                        if (permanentlyDenied) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(DarkYellow)
                ) {
                    Text(
                        text = "Request Permission",
                        color = Black
                    )
                }
            }
        }
    } else {
        BodycamTheme {
            val navController = rememberNavController()
            Navigation(navController)
        }
    }
}