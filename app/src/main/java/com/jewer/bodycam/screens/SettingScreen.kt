package com.jewer.bodycam.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jewer.bodycam.NAV
import com.jewer.bodycam.R
import com.jewer.bodycam.functions.getPersonDetectStatus
import com.jewer.bodycam.functions.getUserName
import com.jewer.bodycam.functions.getVibrateStatus
import com.jewer.bodycam.functions.updatePersonDetectStatus
import com.jewer.bodycam.functions.updateUserName
import com.jewer.bodycam.functions.updateVibrateStatus

@Composable
fun SettingScreen(
    navController: NavController
) {
    val context = LocalContext.current

    // 讀取使用者名稱
    val userName = remember {
        mutableStateOf(getUserName(context))
    }

    // 重新命名對話框控制
    var reName = remember {
        mutableStateOf(false)
    }

    // 讀取人體辨識滑塊狀態
    var isPersonDetectChecked = remember {
        mutableStateOf(getPersonDetectStatus(context))
    }

    // 讀取震動滑塊狀態
    var isVibrateChecked = remember {
        mutableStateOf(getVibrateStatus(context))
    }

    // 儲存狀態
    LaunchedEffect(userName, isPersonDetectChecked, isVibrateChecked) {
        updateUserName(context, userName.value)
        updatePersonDetectStatus(context, isPersonDetectChecked.value)
        updateVibrateStatus(context, isVibrateChecked.value)
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(top = 50.dp, start = 16.dp)
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to cameraScreen",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // 使用者名稱
                Text(
                    text = userName.value,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
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
                        tint = MaterialTheme.colorScheme.secondary
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
                        modifier = Modifier.weight(1f)
                    )

                    // 選項設定滑塊
                    Switch(
                        checked = isPersonDetectChecked.value,
                        onCheckedChange = {
                            isPersonDetectChecked.value = it
                            updatePersonDetectStatus(context, isPersonDetectChecked.value)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = Color.Gray
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
                        text = "Vibrate When Person Detected",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )

                    // 選項設定滑塊
                    Switch(
                        checked = isVibrateChecked.value,
                        onCheckedChange = {
                            isVibrateChecked.value = it
                            updateVibrateStatus(context, isVibrateChecked.value)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = Color.Gray
                        )
                    )
                }
            }
        }
    }
}