package com.jewer.bodycam.frontend.screens

import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.functions.getBeepSoundStatus
import com.jewer.bodycam.backend.functions.getBodycamBrand
import com.jewer.bodycam.backend.functions.getCurrentBatteryLevel
import com.jewer.bodycam.backend.functions.getCurrentTime
import com.jewer.bodycam.backend.functions.getFlashlightStatus
import com.jewer.bodycam.backend.functions.getInstructionAlertDialogStatus
import com.jewer.bodycam.backend.functions.getLowBrightnessStatus
import com.jewer.bodycam.backend.functions.getPhoneName
import com.jewer.bodycam.backend.functions.getUserName
import com.jewer.bodycam.backend.functions.getVibrateAndBeepTimeInterval
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.playSoundAtMaxVolume
import com.jewer.bodycam.backend.functions.setFlashlight
import com.jewer.bodycam.backend.functions.setScreenBrightness
import com.jewer.bodycam.backend.functions.updateInstructionAlertDialogStatus
import com.jewer.bodycam.backend.functions.vibrateOnce
import com.jewer.bodycam.backend.services.ScreenRecordConfig
import com.jewer.bodycam.backend.services.ScreenRecordService
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.KEY_RECORDING_CONFIG
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.START_RECORDING
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.STOP_RECORDING
import com.jewer.bodycam.frontend.nav.NAV
import com.jewer.bodycam.ui.theme.Black
import com.jewer.bodycam.ui.theme.DarkOrange
import com.jewer.bodycam.ui.theme.DarkRed
import com.jewer.bodycam.ui.theme.DarkYellow
import com.jewer.bodycam.ui.theme.LightGreen
import com.jewer.bodycam.ui.theme.Red
import com.jewer.bodycam.ui.theme.White
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavHostController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentTime = remember { mutableStateOf(getCurrentTime()) }
    val currentBatteryLevel = remember { mutableIntStateOf(getCurrentBatteryLevel(context)) }
    val userName = getUserName(context)
    val vibrateApproved = getVibrateStatus(context)
    val beepSoundApproved = getBeepSoundStatus(context)
    val instructionAlertDialogApproved = getInstructionAlertDialogStatus(context)
    val chosenTimeInterval = getVibrateAndBeepTimeInterval(context)
    val isLowBrightnessApproved = getLowBrightnessStatus(context)
    val isFlashlightApproved = getFlashlightStatus(context)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var recordIconIsVisible by remember { mutableStateOf(false) }
    var standByStringIsVisible by remember { mutableStateOf(false) }
    var toolBoxIsVisible by remember { mutableStateOf(false) }
    var instructionAlertDialogIsVisible by remember { mutableStateOf(true) }
    val previewView: PreviewView = remember { PreviewView(context) }
    val mediaProjectionManager by lazy { context.getSystemService<MediaProjectionManager>()!! }
    val isServiceRunning by ScreenRecordService.isServiceRunning.collectAsStateWithLifecycle()
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var useUltraWide by remember { mutableStateOf(true) }
    val chosenBrand = remember { mutableStateOf(getBodycamBrand(context)) }

    val cameraSelector = remember(lensFacing, useUltraWide) {
        CameraSelector.Builder().addCameraFilter { cameraInfos ->
            val filtered = cameraInfos.filter { it.lensFacing == lensFacing }
            if (lensFacing == CameraSelector.LENS_FACING_BACK && useUltraWide) {
                val wideLens = filtered.minByOrNull { cameraInfo ->
                    val camera2Info = Camera2CameraInfo.from(cameraInfo)
                    val focalLengths = camera2Info.getCameraCharacteristic(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )
                    focalLengths?.minOrNull() ?: Float.MAX_VALUE
                }
                wideLens?.let { listOf(it) } ?: filtered
            } else {
                filtered
            }
        }.build()
    }

    val screenRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data ?: return@rememberLauncherForActivityResult
        val config = ScreenRecordConfig(resultCode = result.resultCode, data = intent)
        val serviceIntent = Intent(context.applicationContext, ScreenRecordService::class.java).apply {
            this.action = START_RECORDING
            putExtra(KEY_RECORDING_CONFIG, config)
        }
        context.startForegroundService(serviceIntent)
    }

    // ── 生命週期與硬體控制 ────────────────────────────────────────
    DisposableEffect(Unit) {
        if (isLowBrightnessApproved) setScreenBrightness(context, true)
        if (isFlashlightApproved) setFlashlight(context, true)
        
        onDispose {
            setScreenBrightness(context, false)
            setFlashlight(context, false)
            
            // 修正點：安全釋放相機
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("CameraScreen", "Unbind failed on dispose", e)
            }
        }
    }

    // ── 從 SettingScreen 返回時重新讀取設定 ───────────────────────
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            chosenBrand.value = getBodycamBrand(context)
        }
    }

    // ── CameraX 綁定 ──────────────────────────────────────────────
    LaunchedEffect(cameraSelector) {
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()

                // 確保在 UI thread 設定 provider
                previewView.post {
                    preview.surfaceProvider = previewView.surfaceProvider
                }

                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

                camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                    camera.cameraControl.setZoomRatio(zoomState.minZoomRatio)
                    camera.cameraInfo.zoomState.removeObservers(lifecycleOwner)
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error initializing camera", e)
            }
        }, executor)
    }

    // ── 時間與電量更新 ──
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = getCurrentTime()
            currentBatteryLevel.intValue = getCurrentBatteryLevel(context)
            delay(1000)
        }
    }

    // ── 錄影 Icon 閃爍 ──
    LaunchedEffect(isServiceRunning) {
        while (isServiceRunning) {
            standByStringIsVisible = false
            recordIconIsVisible = !recordIconIsVisible
            delay(1000)
        }
        recordIconIsVisible = false
        standByStringIsVisible = true
    }

    // ── 定時嗶聲/震動 ──
    LaunchedEffect(isServiceRunning) {
        while (isServiceRunning) {
            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonrecordingsound)
            if (vibrateApproved) repeat(2) { vibrateOnce(context, 300); delay(400) }
            delay(chosenTimeInterval)
        }
    }

    // ── 工具列 3 秒自動隱藏 ──
    LaunchedEffect(toolBoxIsVisible) {
        if (toolBoxIsVisible) { delay(3000); toolBoxIsVisible = false }
    }

    // ════════════════════════════════════════════════════════════
    // UI
    // ════════════════════════════════════════════════════════════

    when (chosenBrand.value) {
        "AXON" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(context, "Tap top right \u201CAXON\u201D icon to start/stop recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userName + "   " + currentTime.value + "\n" + getPhoneName(),
                        color = White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f))
                    )
                    IconButton(
                        modifier = Modifier.size(96.dp),
                        onClick = {
                            if (isServiceRunning) {
                                Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                                if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                                if (vibrateApproved) vibrateOnce(context, 1000)
                            } else {
                                screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        }
                    ) {
                        Icon(painter = painterResource(R.mipmap.ic_water_mark_foreground), tint = DarkYellow, contentDescription = "WaterMark", modifier = Modifier.size(96.dp))
                    }
                }

                Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp)) {
                    if (recordIconIsVisible) {
                        Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.size(48.dp))
                    } else if (standByStringIsVisible) {
                        Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.size(48.dp))
                    }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            navController.navigate(NAV.SETTING)
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
                    }
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) { lensFacing = CameraSelector.LENS_FACING_FRONT; useUltraWide = false }
                            else { lensFacing = CameraSelector.LENS_FACING_BACK; useUltraWide = true }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
                    }
                }
                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)) {
                    Text(
                        text = currentBatteryLevel.intValue.toString() + "%",
                        color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        "MOTOROLA" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(context, "Tap top left \u201CMOTOROLA\u201D icon to start/stop recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.Black.copy(alpha = 0.5f)).align(Alignment.TopCenter)) {
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        IconButton(modifier = Modifier.size(96.dp), onClick = {
                            if (isServiceRunning) {
                                Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                                if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                                if (vibrateApproved) vibrateOnce(context, 1000)
                            } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
                        }) { Icon(painter = painterResource(R.mipmap.ic_motorola_icon_foreground), tint = White, contentDescription = "WaterMark", modifier = Modifier.size(96.dp)) }
                    }
                    Row(modifier = Modifier.padding(start = 70.dp, top = 10.dp).align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(text = "MOTOROLA", color = White, fontWeight = FontWeight(1000), fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)), modifier = Modifier.graphicsLayer { scaleY = 0.8f })
                        Text(text = "SOLUTIONS", color = White, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)), modifier = Modifier.graphicsLayer { scaleY = 0.8f })
                    }
                    Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 30.dp, top = 10.dp)) {
                        Text(text = currentTime.value + " " + userName + " " + getPhoneName(), color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)))
                    }
                }

                Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 40.dp)) {
                    if (recordIconIsVisible) { Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.size(48.dp)) }
                    else if (standByStringIsVisible) { Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.size(48.dp)) }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            navController.navigate(NAV.SETTING)
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
                    }
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) { lensFacing = CameraSelector.LENS_FACING_FRONT; useUltraWide = false }
                            else { lensFacing = CameraSelector.LENS_FACING_BACK; useUltraWide = true }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
                    }
                }
                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)) {
                    Text(
                        text = currentBatteryLevel.intValue.toString() + "%",
                        color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        "TRANSCEND" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(context, "Tap bottom left \u201CTRANSCEND\u201D icon to start/stop recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Row(modifier = Modifier.align(Alignment.BottomStart), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(modifier = Modifier.size(96.dp), onClick = {
                        if (isServiceRunning) {
                            Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
                    }) { Icon(painter = painterResource(R.mipmap.ic_transcend_icon_foreground), tint = DarkRed, contentDescription = "WaterMark", modifier = Modifier.size(96.dp)) }
                    Text(text = userName + "\n" + currentTime.value + " " + getPhoneName(), color = DarkOrange, style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)))
                }

                Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp)) {
                    if (recordIconIsVisible) { Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.size(48.dp)) }
                    else if (standByStringIsVisible) { Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.size(48.dp)) }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            navController.navigate(NAV.SETTING)
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
                    }
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) { lensFacing = CameraSelector.LENS_FACING_FRONT; useUltraWide = false }
                            else { lensFacing = CameraSelector.LENS_FACING_BACK; useUltraWide = true }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)) {
                    Text(
                        text = currentBatteryLevel.intValue.toString() + "%",
                        color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        "GETAC" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(context, "Tap top left \u201CGETAC\u201D icon to start/stop recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Box(modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(40.dp).background(Color.Black.copy(alpha = 0.5f)).align(Alignment.TopCenter)) {
                    Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 30.dp)) {
                        IconButton(modifier = Modifier.size(96.dp).scale(2.0f), onClick = {
                            if (isServiceRunning) {
                                Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                                if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                                if (vibrateApproved) vibrateOnce(context, 1000)
                            } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
                        }) { Icon(painter = painterResource(R.mipmap.ic_getac_icon_foreground), tint = DarkOrange, contentDescription = "WaterMark", modifier = Modifier.size(96.dp)) }
                    }
                    Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 30.dp, top = 10.dp)) {
                        Text(text = currentTime.value + " " + userName + " " + getPhoneName(), color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)))
                    }
                }

                Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 60.dp)) {
                    if (recordIconIsVisible) { Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.size(48.dp)) }
                    else if (standByStringIsVisible) { Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.size(48.dp)) }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            navController.navigate(NAV.SETTING)
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
                    }
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) { lensFacing = CameraSelector.LENS_FACING_FRONT; useUltraWide = false }
                            else { lensFacing = CameraSelector.LENS_FACING_BACK; useUltraWide = true }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)) {
                    Text(
                        text = currentBatteryLevel.intValue.toString() + "%",
                        color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        "DOZOR" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(context, "Tap top right \u201CDOZOR\u201D icon to start/stop recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            ) {
                // ── 預覽區域 ──
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 30.dp).scale(1.5f)) {
                    IconButton(
                        modifier = Modifier.size(96.dp),
                        onClick = {
                            if (isServiceRunning) {
                                Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                                if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                                if (vibrateApproved) vibrateOnce(context, 1000)
                            } else {
                                screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        }
                    ) {
                        Icon(painter = painterResource(R.mipmap.ic_dozor_icon_foreground), tint = White, contentDescription = "WaterMark", modifier = Modifier.size(96.dp))
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp)) {
                    Text(text = "DZ" + "   " + userName + "  " + getPhoneName() + "   " + "*" + currentTime.value, color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)), fontSize = 20.sp)
                }

                Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp)) {
                    if (recordIconIsVisible) {
                        Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.size(48.dp))
                    } else if (standByStringIsVisible) {
                        Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.size(48.dp))
                    }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            navController.navigate(NAV.SETTING)
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
                    }
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) { lensFacing = CameraSelector.LENS_FACING_FRONT; useUltraWide = false }
                            else { lensFacing = CameraSelector.LENS_FACING_BACK; useUltraWide = true }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)) {
                    Text(
                        text = currentBatteryLevel.intValue.toString() + "%",
                        color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        "PANASONIC" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(context, "Tap top right \u201CPANASONIC\u201D icon to start/stop recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            ) {
                // ── 預覽區域 ──
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 20.dp)) {
                    Text(
                        text = currentTime.value + "\n" + userName + "   " + getPhoneName(),
                        color = White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f))
                    )
                }

                Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 30.dp).scale(2.0f)) {
                    IconButton(
                        modifier = Modifier.size(96.dp),
                        onClick = {
                            if (isServiceRunning) {
                                Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                                if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                                if (vibrateApproved) vibrateOnce(context, 1000)
                            } else {
                                screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        }
                    ) {
                        Icon(painter = painterResource(R.mipmap.ic_panasonic1_icon_foreground), tint = LightGreen, contentDescription = "WaterMark", modifier = Modifier.size(96.dp))
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) {
                    if (recordIconIsVisible) {
                        Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.size(48.dp))
                    } else if (standByStringIsVisible) {
                        Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.size(48.dp))
                    }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            navController.navigate(NAV.SETTING)
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
                    }
                    AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(modifier = Modifier.size(72.dp), onClick = {
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) { lensFacing = CameraSelector.LENS_FACING_FRONT; useUltraWide = false }
                            else { lensFacing = CameraSelector.LENS_FACING_BACK; useUltraWide = true }
                            if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstartrecordsound)
                            if (vibrateApproved) vibrateOnce(context, 1000)
                        }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp)){
                    Text(
                        text = currentBatteryLevel.intValue.toString() + "%",
                        color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 5f)),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }

    // ── 使用手冊 ──────────────────────────────────────────────────
    if (instructionAlertDialogApproved && instructionAlertDialogIsVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "Instruction", color = White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    SelectionContainer {
                        Text(
                            text = "●  AXON : Tap top right \u201CAXON\u201D icon to start/stop recording.\n\n" +
                                    "●  MOTOROLA : Tap top left \u201CMOTOROLA\u201D icon to start/stop recording.\n\n" +
                                    "●  TRANSCEND : Tap bottom left \u201CTRANSCEND\u201D icon to start/stop recording.\n\n" +
                                    "●  GETAC : Tap top left \u201CGETAC\u201D icon to start/stop recording.\n\n" +
                                    "●  DOZOR : Tap top right \u201CDOZOR\u201D icon to start/stop recording.\n\n" +
                                    "●  PANASONIC : Tap top right \u201CPANASONIC\u201D icon to start/stop recording.\n\n" +
                                    "●  Record result will be stored in \u201CBodycam\u201D folder in device media store space.\n\n" +
                                    "●  For android 14+ device, you can chose to record \u201CA single app\u201D or \u201CEntire screen\u201D.\n\n" +
                                    "●  Tap the screen then \u201Csettings\u201D and \u201Ccamera change\u201D will show on the left side of screen.\n\n" +
                                    "●  User name can be changed.",
                            color = White
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { instructionAlertDialogIsVisible = false }) {
                    Text(color = DarkYellow, text = "close")
                }
            },
            dismissButton = {
                TextButton(onClick = { instructionAlertDialogIsVisible = false; updateInstructionAlertDialogStatus(context, false) }) {
                    Text(color = DarkYellow, text = "close and do not show again")
                }
            }
        )
    }
}
