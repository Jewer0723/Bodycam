package com.jewer.bodycam

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jewer.bodycam.screens.CameraScreen
import com.jewer.bodycam.screens.SettingScreen
import com.jewer.bodycam.ui.theme.BodycamTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 保持螢幕永久開啟，直到銷毀活動
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            // 控制權限請求
            val permissionState = rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )

            // 授權對話框
            LaunchedEffect(key1 = Unit) {
                permissionState.launchMultiplePermissionRequest()
            }

            // 如果授權項被拒絕，執行授權
            if (!permissionState.allPermissionsGranted) {

                // 用戶手動授權畫面
                permissionState.permissions.forEach {
                    if (!it.status.isGranted) {
                        BodycamTheme(darkTheme = true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = MaterialTheme.colorScheme.onPrimary)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No authorized!!",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp)) // 添加間距
                                Button(
                                    onClick = { permissionState.launchMultiplePermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Request Permission")
                                }
                            }
                            return@BodycamTheme
                        }
                    }
                }
            } else {

                // 進入activity
                Bodycam()
            }
        }
    }
}


// 導航頁面方法
@Composable
fun Bodycam() {
    BodycamTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = NAV.CAMERA
            ) {
                composable(route = NAV.CAMERA) {
                    CameraScreen(navController = navController)
                }
                composable(route = NAV.SETTING) {
                    SettingScreen(navController = navController)
                }
            }
        }
    }
}

// 導航頁面物件
object NAV {
    const val CAMERA = "camera"
    const val SETTING = "setting"
}