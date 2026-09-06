package com.jewer.bodycam

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jewer.bodycam.backend.functions.getKeyRecordingStatus
import com.jewer.bodycam.backend.functions.initSettings
import com.jewer.bodycam.backend.functions.orientationFlow
import com.jewer.bodycam.backend.functions.setFullScreen
import com.jewer.bodycam.backend.services.ScreenRecordService
import com.jewer.bodycam.frontend.screens.PermissionScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化設定 Flow，讀取最後保存的方向
        initSettings(this)
        
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFullScreen(this, true)

        setContent {
            // 即時監聽 orientationFlow，一旦在設定中改變模式，這裡會立即觸發轉向
            val currentMode by orientationFlow.collectAsStateWithLifecycle()

            LaunchedEffect(currentMode) {
                val target = if (currentMode == 1) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT 
                             else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                
                if (requestedOrientation != target) {
                    requestedOrientation = target
                }
            }
            
            PermissionScreen()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (getKeyRecordingStatus(this)) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    ScreenRecordService.startViaKey()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    ScreenRecordService.stopViaKey()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
