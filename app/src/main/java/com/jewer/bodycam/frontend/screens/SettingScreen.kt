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
import com.jewer.bodycam.backend.functions.getBeepVolume
import com.jewer.bodycam.backend.functions.getBodycamBrand
import com.jewer.bodycam.backend.functions.getFlashlightStatus
import com.jewer.bodycam.backend.functions.getLowBrightnessStatus
import com.jewer.bodycam.backend.functions.getPersonDetectStatus
import com.jewer.bodycam.backend.functions.getUserName
import com.jewer.bodycam.backend.functions.getVibrateAndBeepTimeInterval
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.updateBeepSoundStatus
import com.jewer.bodycam.backend.functions.updateBeepVolume
import com.jewer.bodycam.backend.functions.updateBodycamBrand
import com.jewer.bodycam.backend.functions.updateFlashlightStatus
import com.jewer.bodycam.backend.functions.updateLowBrightnessStatus
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
    val userName = remember { mutableStateOf(getUserName(context)) }
    val reName = remember { mutableStateOf(false) }
    var timeIntervalExpand by remember { mutableStateOf(false) }
    var volumeExpand by remember { mutableStateOf(false) }
    val brandChoose = remember { mutableStateOf(false) }
    val isPersonDetectChecked = remember { mutableStateOf(getPersonDetectStatus(context)) }
    val isVibrateChecked = remember { mutableStateOf(getVibrateStatus(context)) }
    val isBeepSoundChecked = remember { mutableStateOf(getBeepSoundStatus(context)) }
    val isLowBrightnessChecked = remember { mutableStateOf(getLowBrightnessStatus(context)) }
    val isFlashlightChecked = remember { mutableStateOf(getFlashlightStatus(context)) }
    val chosenBrand = remember { mutableStateOf(getBodycamBrand(context)) }

    data class TimeInterval(val displayText: String, val intervalMillis: Long)
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
            ?: timeIntervalOptions.find { it.intervalMillis == 120000L }!!
    }
    var chosenTimeInterval by remember { mutableStateOf(initialInterval) }

    data class VolumeOption(val displayText: String, val percent: Int)
    val volumeOptions = listOf(
        VolumeOption("High", 100),
        VolumeOption("Medium", 60),
        VolumeOption("Low", 30)
    )
    val initialVolume = remember {
        volumeOptions.find { it.percent == getBeepVolume(context) }
            ?: volumeOptions.find { it.percent == 100 }!!
    }
    var chosenVolume by remember { mutableStateOf(initialVolume) }

    val bodycamBrand = listOf("AXON", "MOTOROLA", "TRANSCEND")

    LaunchedEffect(userName, isPersonDetectChecked, isVibrateChecked, isLowBrightnessChecked, isFlashlightChecked) {
        updateUserName(context, userName.value)
        updatePersonDetectStatus(context, isPersonDetectChecked.value)
        updateVibrateStatus(context, isVibrateChecked.value)
        updateLowBrightnessStatus(context, isLowBrightnessChecked.value)
        updateFlashlightStatus(context, isFlashlightChecked.value)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(top = 50.dp, start = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 返回 + 使用者名稱列
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.navigate(NAV.CAMERA) }) {
                    Icon(painter = painterResource(R.drawable.ic_arrowback_foreground), contentDescription = "Back", modifier = Modifier.padding(end = 8.dp), tint = White)
                }
                Text(text = userName.value, textAlign = TextAlign.End, modifier = Modifier.weight(1f), color = White)
                IconButton(onClick = { reName.value = true }) {
                    Icon(painter = painterResource(id = R.drawable.ic_modify_user_name_foreground), contentDescription = "rename", modifier = Modifier.padding(end = 8.dp), tint = DarkYellow)
                }
            }

            HorizontalDivider(thickness = 2.dp)

            // 人體辨識
            TextButton(onClick = { isPersonDetectChecked.value = !isPersonDetectChecked.value; updatePersonDetectStatus(context, isPersonDetectChecked.value) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Person Detect", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Switch(checked = isPersonDetectChecked.value, onCheckedChange = { isPersonDetectChecked.value = it; updatePersonDetectStatus(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, uncheckedThumbColor = White, checkedTrackColor = DarkYellow, uncheckedTrackColor = Gray))
                }
            }

            // 震動
            TextButton(onClick = { isVibrateChecked.value = !isVibrateChecked.value; updateVibrateStatus(context, isVibrateChecked.value) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Vibrate Effect", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Switch(checked = isVibrateChecked.value, onCheckedChange = { isVibrateChecked.value = it; updateVibrateStatus(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, uncheckedThumbColor = White, checkedTrackColor = DarkYellow, uncheckedTrackColor = Gray))
                }
            }

            // 嗶聲
            TextButton(onClick = { isBeepSoundChecked.value = !isBeepSoundChecked.value; updateBeepSoundStatus(context, isBeepSoundChecked.value) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Beep Sound", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Switch(checked = isBeepSoundChecked.value, onCheckedChange = { isBeepSoundChecked.value = it; updateBeepSoundStatus(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, uncheckedThumbColor = White, checkedTrackColor = DarkYellow, uncheckedTrackColor = Gray))
                }
            }

            // 最低亮度
            TextButton(onClick = { isLowBrightnessChecked.value = !isLowBrightnessChecked.value; updateLowBrightnessStatus(context, isLowBrightnessChecked.value) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Low Brightness", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Switch(checked = isLowBrightnessChecked.value, onCheckedChange = { isLowBrightnessChecked.value = it; updateLowBrightnessStatus(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, uncheckedThumbColor = White, checkedTrackColor = DarkYellow, uncheckedTrackColor = Gray))
                }
            }

            // 手電筒
            TextButton(onClick = { isFlashlightChecked.value = !isFlashlightChecked.value; updateFlashlightStatus(context, isFlashlightChecked.value) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Flashlight", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Switch(checked = isFlashlightChecked.value, onCheckedChange = { isFlashlightChecked.value = it; updateFlashlightStatus(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, uncheckedThumbColor = White, checkedTrackColor = DarkYellow, uncheckedTrackColor = Gray))
                }
            }

            // 嗶聲/震動間隔
            TextButton(onClick = { timeIntervalExpand = !timeIntervalExpand }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Vibrate/Beep Sound Time Interval", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Text(text = chosenTimeInterval.displayText, color = DarkYellow)
                    DropdownMenu(expanded = timeIntervalExpand, onDismissRequest = { timeIntervalExpand = false }, modifier = Modifier.border(1.dp, White)) {
                        timeIntervalOptions.forEach { timeOption ->
                            DropdownMenuItem(text = { Text(text = timeOption.displayText, color = White) }, onClick = {
                                chosenTimeInterval = timeOption
                                updateVibrateAndBeepTimeInterval(context, timeOption.intervalMillis)
                                timeIntervalExpand = false
                            })
                        }
                    }
                }
            }

            // 嗶聲音量選擇
            TextButton(onClick = { volumeExpand = !volumeExpand }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Beep Volume", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    Text(text = chosenVolume.displayText, color = DarkYellow)
                    DropdownMenu(expanded = volumeExpand, onDismissRequest = { volumeExpand = false }, modifier = Modifier.border(1.dp, White)) {
                        volumeOptions.forEach { volumeOption ->
                            DropdownMenuItem(text = { Text(text = volumeOption.displayText, color = White) }, onClick = {
                                chosenVolume = volumeOption
                                updateBeepVolume(context, volumeOption.percent)
                                volumeExpand = false
                            })
                        }
                    }
                }
            }

            // 品牌
            TextButton(onClick = { brandChoose.value = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Bodycam Brand", textAlign = TextAlign.Start, modifier = Modifier.weight(1f), color = White)
                    chosenBrand.value?.let { Text(text = it, color = DarkYellow) }
                }
            }
        }
    }

    // ── 重新命名對話框 ────────────────────────────────────────────
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
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                TextButton(onClick = { updateUserName(context, userName.value); reName.value = false }) {
                    Text(text = "confirm")
                }
            }
        )
    }

    // ── 品牌對話框 ────────────────────────────────────────────────
    if (brandChoose.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Choose Bodycam Brand") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    bodycamBrand.forEach { brand ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            RadioButton(selected = chosenBrand.value == brand, onClick = { chosenBrand.value = brand }, modifier = Modifier.padding(end = 8.dp), colors = RadioButtonDefaults.colors(DarkYellow))
                            Text(modifier = Modifier.padding(top = 10.dp), text = brand)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { chosenBrand.value?.let { updateBodycamBrand(context, it) }; brandChoose.value = false }) {
                    Text(text = "confirm", color = DarkYellow)
                }
            }
        )
    }
}
