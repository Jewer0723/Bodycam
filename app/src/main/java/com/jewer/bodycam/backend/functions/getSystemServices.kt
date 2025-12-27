package com.jewer.bodycam.backend.functions

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Vibrator
import java.text.SimpleDateFormat
import java.util.Locale

// 獲取現在時間
fun getCurrentTime(): String {
    val timeFormat = "yyyy-MM-dd   HH:mm:ss"
    return SimpleDateFormat(timeFormat, Locale.US).format(System.currentTimeMillis())
}

// 獲取現在電量
fun getCurrentBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

// 震動秒數控制
fun vibrateOnce(context: Context, duration: Long) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(duration) // 震動指定的毫秒數
}

// 撥放音檔及最大喇叭聲響控制
fun playSoundAtMaxVolume(context: Context, resourceId: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // 1. 記錄當前的媒體音量.
    val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    // 2. 獲取並設定媒體音量為最大值.
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC, // 指定調整媒體音量
        maxVolume,
        0 // 0 表示不在螢幕上顯示音量調整UI
    )

    // 3. 建立並播放音檔
    // 使用 MediaPlayer.create 可以簡化準備過程.
    val mediaPlayer = MediaPlayer.create(context, resourceId)

    mediaPlayer?.let { mp ->
        // 4. 設定監聽器，當音檔播放完畢時執行.
        mp.setOnCompletionListener {
            // 恢復原始音量.
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            // 釋放 MediaPlayer 資源.
            it.release()
        }

        // 處理播放時可能發生的錯誤
        mp.setOnErrorListener { _, _, _ ->
            // 如果發生錯誤，同樣要恢復音量並釋放資源
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            mp.release()
            true // 表示錯誤已處理
        }

        // 5. 開始播放
        mp.start()
    } ?: run {
        // 如果 MediaPlayer 建立失敗 (例如資源不存在)，也要恢復音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }
}