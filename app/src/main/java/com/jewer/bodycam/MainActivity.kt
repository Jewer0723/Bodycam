package com.jewer.bodycam

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jewer.bodycam.backend.functions.getKeyRecordingStatus
import com.jewer.bodycam.backend.functions.setFullScreen
import com.jewer.bodycam.backend.services.ScreenRecordService
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 檢查設定是否開啟按鍵錄影
        if (getKeyRecordingStatus(this)) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    // 音量加：啟動錄影
                    ScreenRecordService.startViaKey()
                    return true // 攔截事件，不顯示音量條
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    // 音量減：停止錄影
                    ScreenRecordService.stopViaKey()
                    return true // 攔截事件，不顯示音量條
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
