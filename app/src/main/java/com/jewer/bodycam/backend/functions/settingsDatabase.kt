package com.jewer.bodycam.backend.functions

import android.content.Context
import androidx.core.content.edit

/**********************************************************************************************************/

// 重新命名使用者名稱
fun updateUserName(context: Context, newUserName: String) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putString("userName", newUserName)
    }
}

// 讀取使用者名稱
fun getUserName(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("userName", "User") ?: ""
}

/**********************************************************************************************************/

/**********************************************************************************************************/

// 更新人體辨識布林狀態
fun updatePersonDetectStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("personDetect", isEnabled)
    }
}

// 讀取人體辨識布林狀態
fun getPersonDetectStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("personDetect", false)
}

/**********************************************************************************************************/

/**********************************************************************************************************/

// 更新震動布林狀態
fun updateVibrateStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("vibrate", isEnabled)
    }
}

// 讀取震動布林狀態
fun getVibrateStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("vibrate", false)
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新嗶聲布林狀態
fun updateBeepSoundStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("beep", isEnabled)
    }
}

// 讀取嗶聲布林狀態
fun getBeepSoundStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("beep", false)
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新嗶聲/震動時間間隔
fun updateVibrateAndBeepTimeInterval(context: Context, timeInterval: Long) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putLong("timeInterval", timeInterval)
    }
}

// 讀取嗶聲/震動時間間隔
fun getVibrateAndBeepTimeInterval(context: Context): Long {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getLong("timeInterval", 120000L)
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新音量百分比 (0-100)
fun updateBeepVolume(context: Context, volume: Int) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putInt("beepVolume", volume)
    }
}

// 讀取音量百分比 (預設 100)
fun getBeepVolume(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getInt("beepVolume", 100)
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新說明書對話框顯示布林狀態
fun updateInstructionAlertDialogStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("instruction", isEnabled)
    }
}

// 讀取說明書對話框布林狀態
fun getInstructionAlertDialogStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("instruction", true)
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新密錄器品牌
fun updateBodycamBrand(context: Context, brand: String) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putString("brand", brand)
    }
}

// 讀取密錄器品牌
fun getBodycamBrand(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("brand", "AXON")
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新最低亮度螢幕布林狀態
fun updateLowBrightnessStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("lowBrightness", isEnabled)
    }
}

// 讀取最低亮度螢幕布林狀態
fun getLowBrightnessStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("lowBrightness", false)
}

/******************************************************************************************************************/

/**********************************************************************************************************/

// 更新手電筒布林狀態
fun updateFlashlightStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putBoolean("flashlight", isEnabled)
    }
}

// 讀取手電筒布林狀態
fun getFlashlightStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("flashlight", false)
}

/******************************************************************************************************************/
