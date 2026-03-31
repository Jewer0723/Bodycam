package com.jewer.bodycam

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jewer.bodycam.backend.functions.setFullScreen
import com.jewer.bodycam.frontend.screens.PermissionScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 保持螢幕永久開啟
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 套用全螢幕模式至整個 App
        setFullScreen(this, true)

        setContent {
            PermissionScreen()
        }
    }
}
