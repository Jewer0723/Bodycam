package com.jewer.bodycam.functions

import android.content.Context
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