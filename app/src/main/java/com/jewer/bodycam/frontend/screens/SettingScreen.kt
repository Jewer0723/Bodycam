package com.jewer.bodycam.frontend.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.functions.getBeepSoundStatus
import com.jewer.bodycam.backend.functions.getBodycamBrand
import com.jewer.bodycam.backend.functions.getPersonDetectStatus
import com.jewer.bodycam.backend.functions.getUserName
import com.jewer.bodycam.backend.functions.getVibrateAndBeepTimeInterval
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.updateBeepSoundStatus
import com.jewer.bodycam.backend.functions.updateBodycamBrand
import com.jewer.bodycam.backend.functions.updatePersonDetectStatus
import com.jewer.bodycam.backend.functions.updateUserName
import com.jewer.bodycam.backend.functions.updateVibrateAndBeepTimeInterval
import com.jewer.bodycam.backend.functions.updateVibrateStatus
import com.jewer.bodycam.frontend.nav.NAV
import com.jewer.bodycam.ui.theme.DarkYellow
import com.jewer.bodycam.ui.theme.Gray
import com.jewer.bodycam.ui.theme.White

@Composable
fun SettingScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val userName = remember { mutableStateOf(getUserName(context)) } // 讀取使用者名稱
    val reName = remember { mutableStateOf(false) } // 重新命名對話框控制
    var timeIntervalExpand by remember { mutableStateOf(false) } // 聲響/震動聲響間隔下拉選單控制
    val brandChoose = remember { mutableStateOf(false) } // 品牌選擇對話框控制
    val isPersonDetectChecked = remember { mutableStateOf(getPersonDetectStatus(context)) } // 讀取人體辨識滑塊狀態
    val isVibrateChecked = remember { mutableStateOf(getVibrateStatus(context)) } // 讀取震動滑塊狀態
    val isBeepSoundChecked = remember { mutableStateOf(getBeepSoundStatus(context)) } // 讀取嗶聲滑塊狀態
    val chosenBrand = remember { mutableStateOf(getBodycamBrand(context)) } // 選擇的密錄器品牌

    data class TimeInterval(
        val displayText: String, // 用於顯示的文字
        val intervalMillis: Long    // 實際的毫秒數
    )
    val timeIntervalOptions = listOf(
        TimeInterval("0.5 min", 30000L),
        TimeInterval("1 min", 60000L),
        TimeInterval("1.5 min", 90000L),
        TimeInterval("2 min", 120000L),
        TimeInterval("3 min", 180000L),
        TimeInterval("5 min", 300000L),
        TimeInterval("10 min", 600000L),
        TimeInterval("30 min", 1800000L),
        TimeInterval("60 min", 3600000L)
    )
    val initialInterval = remember {
        timeIntervalOptions.find { it.intervalMillis == getVibrateAndBeepTimeInterval(context) }
            ?: timeIntervalOptions.find { it.intervalMillis == 120000L }!! // 如果找不到，使用預設的 "2 min"
    }
    var chosenTimeInterval by remember { mutableStateOf(initialInterval) } // 儲存整個物件

    val bodycamBrand = listOf( // 密錄器品牌列表
        "AXON",
        "MOTOROLA",
        "TRANSCEND"
    )

    LaunchedEffect(userName, isPersonDetectChecked, isVibrateChecked) { // 儲存狀態
        updateUserName(context, userName.value)
        updatePersonDetectStatus(context, isPersonDetectChecked.value)
        updateVibrateStatus(context, isVibrateChecked.value)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(top = 50.dp, start = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 返回按鈕、使用者名稱和重新命名按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按鈕
                IconButton(
                    onClick = {
                        navController.navigate(NAV.CAMERA)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrowback_foreground),
                        contentDescription = "Back to cameraScreen",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = White
                    )
                }

                // 使用者名稱
                Text(
                    text = userName.value,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                    color = White
                )

                // 重新命名按鈕
                IconButton(
                    onClick = {
                        reName.value = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_modify_user_name_foreground),
                        contentDescription = "modify_user_name_foreground",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = DarkYellow
                    )
                }
            }

            // 水平分割線
            HorizontalDivider(thickness = 2.dp)

            // 人體辨識開關
            TextButton(
                onClick = {
                    isPersonDetectChecked.value = !isPersonDetectChecked.value
                    updatePersonDetectStatus(context, isPersonDetectChecked.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 選項名稱
                    Text(
                        text = "Person Detect",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        color = White
                    )

                    // 選項設定滑塊
                    Switch(
                        checked = isPersonDetectChecked.value,
                        onCheckedChange = {
                            isPersonDetectChecked.value = it
                            updatePersonDetectStatus(context, isPersonDetectChecked.value)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            uncheckedThumbColor = White,
                            checkedTrackColor = DarkYellow,
                            uncheckedTrackColor = Gray
                        )
                    )
                }
            }

            // 震動開關
            TextButton(
                onClick = {
                    isVibrateChecked.value = !isVibrateChecked.value
                    updateVibrateStatus(context, isVibrateChecked.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 選項名稱
                    Text(
                        text = "Vibrate Effect",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        color = White
                    )

                    // 選項設定滑塊
                    Switch(
                        checked = isVibrateChecked.value,
                        onCheckedChange = {
                            isVibrateChecked.value = it
                            updateVibrateStatus(context, isVibrateChecked.value)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            uncheckedThumbColor = White,
                            checkedTrackColor = DarkYellow,
                            uncheckedTrackColor = Gray
                        )
                    )
                }
            }

            // 嗶聲開關
            TextButton(
                onClick = {
                    isBeepSoundChecked.value = !isBeepSoundChecked.value
                    updateBeepSoundStatus(context, isBeepSoundChecked.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 選項名稱
                    Text(
                        text = "Beep Sound",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        color = White
                    )

                    // 選項設定滑塊
                    Switch(
                        checked = isBeepSoundChecked.value,
                        onCheckedChange = {
                            isBeepSoundChecked.value = it
                            updateBeepSoundStatus(context, isBeepSoundChecked.value)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            uncheckedThumbColor = White,
                            checkedTrackColor = DarkYellow,
                            uncheckedTrackColor = Gray
                        )
                    )
                }
            }

            // 嗶聲/震動間隔選擇
            TextButton(
                onClick = {
                    timeIntervalExpand = !timeIntervalExpand
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 選項名稱
                    Text(
                        text = "Vibrate/Beep Sound Time Interval",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        color = White
                    )

                    Text(
                        text = chosenTimeInterval.displayText, // 直接從狀態物件中讀取 displayText
                        color = DarkYellow
                    )

                    // 時間間隔下拉選單
                    DropdownMenu(
                        expanded = timeIntervalExpand,
                        onDismissRequest = {
                            timeIntervalExpand = false
                        },
                        modifier = Modifier.border(1.dp, White)
                    ) {
                        timeIntervalOptions.forEach { timeOption ->
                            DropdownMenuItem(
                                text = { Text(text = timeOption.displayText, color = White) },
                                onClick = {
                                    chosenTimeInterval = timeOption
                                    updateVibrateAndBeepTimeInterval(context, timeOption.intervalMillis)
                                    timeIntervalExpand = false
                                })
                        }
                    }
                }
            }

            // 密錄器品牌
            TextButton(
                onClick = {
                    brandChoose.value = true // 開啟對話框
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 選項名稱
                    Text(
                        text = "Bodycam Brand",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                        color = White
                    )

                    chosenBrand.value?.let {
                        Text(
                            text = it,
                            color = DarkYellow
                        )
                    }
                }
            }
        }
    }

    if (reName.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "ReName User Name") },
            text = {
                OutlinedTextField(
                    value = userName.value,
                    onValueChange = { userName.value = it },
                    label = { Text("Input new user name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateUserName(context, userName.value)
                    reName.value = false
                }) {
                    Text(text = "confirm")
                }
            }
        )
    }

    if (brandChoose.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Choose Bodycam Brand") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    bodycamBrand.forEach { brand ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = chosenBrand.value == brand,
                                onClick = {
                                    chosenBrand.value = brand
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = RadioButtonDefaults.colors(DarkYellow)
                            )
                            Text(
                                modifier = Modifier.padding(top = 10.dp),
                                text = brand
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    chosenBrand.value?.let { updateBodycamBrand(context, it) }
                    brandChoose.value = false // 關閉對話框
                }) {
                    Text(
                        text = "confirm",
                        color = DarkYellow
                    )
                }
            }
        )
    }
}