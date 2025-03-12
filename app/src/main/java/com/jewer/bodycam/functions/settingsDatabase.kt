package com.jewer.bodycam.functions

import android.content.Context

/**********************************************************************************************************/

// 重新命名使用者名稱
fun updateUserName(context: Context, newUserName: String) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("userName", newUserName)
    editor.apply()
}

// 讀取使用者名稱
fun getUserName(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("userName", "User") ?: ""
}

/**********************************************************************************************************/

// 更新人體辨識布林狀態
fun updatePersonDetectStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("personDetect", isEnabled)
    editor.apply()
}

// 讀取人體辨識布林狀態
fun getPersonDetectStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("personDetect", false)
}

/**********************************************************************************************************/

// 更新震動布林狀態
fun updateVibrateStatus(context: Context, isEnabled: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("vibrate", isEnabled)
    editor.apply()
}

// 讀取震動布林狀態
fun getVibrateStatus(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("vibrate", false)
}

/******************************************************************************************************************/