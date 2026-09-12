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
            // 存取媒體與通知權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }

            // Nearby Connections (Radio) 必備：位置權限
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)

            // Android 12 (API 31) 以上需要獨立的藍牙權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            // Android 13 (API 33) 以上需要附近 Wi-Fi 權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissionList)

    // 自動啟動一次授權請求
    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }
    
    val hasMediaPermission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            permissionState.permissions.any {
                (it.permission == Manifest.permission.READ_MEDIA_VIDEO ||
                 it.permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) && it.status.isGranted
            }
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            permissionState.permissions.any {
                it.permission == Manifest.permission.READ_MEDIA_VIDEO && it.status.isGranted
            }
        }
        else -> {
            permissionState.permissions.any {
                it.permission == Manifest.permission.READ_EXTERNAL_STORAGE && it.status.isGranted
            }
        }
    }

    val otherPermissionsGranted = permissionState.permissions
        .filter {
            it.permission != "android.permission.READ_MEDIA_VIDEO" &&
            it.permission != "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" &&
            it.permission != Manifest.permission.READ_EXTERNAL_STORAGE
        }
        .all { it.status.isGranted }

    val allGranted = hasMediaPermission && otherPermissionsGranted

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
                            val isMedia = permission.permission == "android.permission.READ_MEDIA_VIDEO" ||
                                          permission.permission == "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" ||
                                          permission.permission == Manifest.permission.READ_EXTERNAL_STORAGE
                            if (isMedia) {
                                !hasMediaPermission && !permission.status.shouldShowRationale
                            } else {
                                !permission.status.isGranted && !permission.status.shouldShowRationale
                            }
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