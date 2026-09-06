package com.jewer.bodycam.frontend.screens

import android.app.Activity
import android.content.Intent
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.camera.CustomCameraEffect
import com.jewer.bodycam.backend.camera.WideAngleSurfaceProcessor
import com.jewer.bodycam.backend.functions.getBeepSoundStatus
import com.jewer.bodycam.backend.functions.getBodyDetectionStatus
import com.jewer.bodycam.backend.functions.getBodycamBrand
import com.jewer.bodycam.backend.functions.getCurrentBatteryLevel
import com.jewer.bodycam.backend.functions.getCurrentTime
import com.jewer.bodycam.backend.functions.getFlashlightStatus
import com.jewer.bodycam.backend.functions.getInstructionAlertDialogStatus
import com.jewer.bodycam.backend.functions.getLowBrightnessStatus
import com.jewer.bodycam.backend.functions.getPhoneName
import com.jewer.bodycam.backend.functions.getSimulatedWideAngleStatus
import com.jewer.bodycam.backend.functions.getUserName
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.orientationFlow
import com.jewer.bodycam.backend.functions.playSoundAtMaxVolume
import com.jewer.bodycam.backend.functions.setScreenBrightness
import com.jewer.bodycam.backend.functions.updateInstructionAlertDialogStatus
import com.jewer.bodycam.backend.functions.vibrateOnce
import com.jewer.bodycam.backend.services.RadioService
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
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class, ExperimentalGetImage::class)
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavHostController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // 監聽方向模式
    val orientationMode by orientationFlow.collectAsStateWithLifecycle()
    val isPortrait = orientationMode == 1
    
    val currentTime = remember { mutableStateOf(getCurrentTime()) }
    val currentBatteryLevel = remember { mutableIntStateOf(getCurrentBatteryLevel(context)) }
    val userName = getUserName(context)
    
    // 設定值快取
    val vibrateApproved = remember { getVibrateStatus(context) }
    val beepSoundApproved = remember { getBeepSoundStatus(context) }
    val instructionAlertDialogApproved = remember { getInstructionAlertDialogStatus(context) }
    val isLowBrightnessApproved = remember { getLowBrightnessStatus(context) }
    val isFlashlightApproved = remember { getFlashlightStatus(context) }
    val isBodyDetectionApproved = remember { getBodyDetectionStatus(context) }
    val isSimulatedWideAngleApproved = remember { getSimulatedWideAngleStatus(context) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val textShadow = remember { Shadow(color = Black, offset = Offset(3f, 3f), blurRadius = 2f) }
    val consolasBold = remember { FontFamily(Font(R.font.consolas, FontWeight.Bold)) }
    
    var recordIconIsVisible by remember { mutableStateOf(false) }
    var standByStringIsVisible by remember { mutableStateOf(false) }
    var toolBoxIsVisible by remember { mutableStateOf(false) }
    var instructionAlertDialogIsVisible by remember { mutableStateOf(true) }
    
    val previewView: PreviewView = remember { 
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE // OpenGL 濾鏡必須使用 COMPATIBLE 模式
        }
    }
    
    val mediaProjectionManager by lazy { context.getSystemService<MediaProjectionManager>()!! }
    val isRecordingRunning by ScreenRecordService.isRecordingRunning.collectAsStateWithLifecycle()
    val isRadioRunning by RadioService.isRadioRunning.collectAsStateWithLifecycle()
    val radioEndpoints by RadioService.connectedEndpoints.collectAsStateWithLifecycle()

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var useUltraWide by remember { mutableStateOf(true) }
    val chosenBrand = remember { mutableStateOf(getBodycamBrand(context)) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // ── OpenGL 廣角濾鏡生命週期管理 ──
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val wideAngleEffect = remember(isSimulatedWideAngleApproved, lensFacing, isPortrait) {
        if (isSimulatedWideAngleApproved) {
            val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
            CustomCameraEffect(
                CameraEffect.PREVIEW,
                cameraExecutor,
                WideAngleSurfaceProcessor(isPortrait, isFront)
            ) { Log.e("WideAngle", "Effect error", it) }
        } else null
    }

    // ── 人體辨識相關 ──
    var detectedPoseBoundingBox by remember { mutableStateOf<RectF?>(null) }
    var frameWidth by remember { mutableIntStateOf(0) }
    var frameHeight by remember { mutableIntStateOf(0) }
    val poseDetector = remember {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    fun toggleRadio() {
        if (isRadioRunning) {
            val intent = Intent(context, RadioService::class.java).apply { action = RadioService.ACTION_STOP }
            context.startService(intent)
        } else {
            val intent = Intent(context, RadioService::class.java).apply { action = RadioService.ACTION_START }
            context.startForegroundService(intent)
        }
    }

    LaunchedEffect(Unit) {
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to get CameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

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
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data ?: return@rememberLauncherForActivityResult
            val config = ScreenRecordConfig(resultCode = result.resultCode, data = intent)
            val serviceIntent = Intent(context.applicationContext, ScreenRecordService::class.java).apply {
                this.action = START_RECORDING
                putExtra(KEY_RECORDING_CONFIG, config)
            }
            context.startForegroundService(serviceIntent)
        }
    }

    DisposableEffect(Unit) {
        if (isLowBrightnessApproved) setScreenBrightness(context, true)
        onDispose {
            setScreenBrightness(context, false)
            poseDetector.close()
            analysisExecutor.shutdown()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            chosenBrand.value = getBodycamBrand(context)
        }
    }

    LaunchedEffect(cameraProvider, cameraSelector, isBodyDetectionApproved, isSimulatedWideAngleApproved, lifecycleOwner) {
        val provider = cameraProvider ?: return@LaunchedEffect
        try {
            delay(200.milliseconds)
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider
            provider.unbindAll()

            val useCaseGroupBuilder = UseCaseGroup.Builder().addUseCase(preview)

            // ── 正式套用 OpenGL 廣角濾鏡 ──
            if (isSimulatedWideAngleApproved && wideAngleEffect != null) {
                useCaseGroupBuilder.addEffect(wideAngleEffect)
            }

            if (isBodyDetectionApproved) {
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (lifecycleOwner.lifecycle.currentState < Lifecycle.State.STARTED) {
                        imageProxy.close(); return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        try {
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val image = InputImage.fromMediaImage(mediaImage, rotation)
                            if (rotation == 90 || rotation == 270) { frameWidth = imageProxy.height; frameHeight = imageProxy.width } 
                            else { frameWidth = imageProxy.width; frameHeight = imageProxy.height }

                            poseDetector.process(image)
                                .addOnSuccessListener { pose ->
                                    if (lifecycleOwner.lifecycle.currentState < Lifecycle.State.STARTED) return@addOnSuccessListener
                                    val landmarks = pose.allPoseLandmarks
                                    val highConfidenceLandmarks = landmarks.filter { it.inFrameLikelihood > 0.99f }
                                    if (highConfidenceLandmarks.size >= 5) {
                                        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
                                        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
                                        for (landmark in highConfidenceLandmarks) {
                                            val position = landmark.position
                                            if (position.x < minX) minX = position.x
                                            if (position.x > maxX) maxX = position.x
                                            if (position.y < minY) minY = position.y
                                            if (position.y > maxY) maxY = position.y
                                        }
                                        detectedPoseBoundingBox = RectF(minX - 30f, minY - 30f, maxX + 30f, maxY + 30f)
                                    } else { detectedPoseBoundingBox = null }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } catch (_: Exception) { imageProxy.close() }
                    } else { imageProxy.close() }
                }
                useCaseGroupBuilder.addUseCase(imageAnalysis)
            }

            if (lifecycleOwner.lifecycle.currentState >= Lifecycle.State.STARTED) {
                val camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroupBuilder.build())
                if (isFlashlightApproved) camera.cameraControl.enableTorch(true)
                
                val zoomState = camera.cameraInfo.zoomState.value
                if (zoomState != null && zoomState.zoomRatio != zoomState.minZoomRatio) {
                    camera.cameraControl.setZoomRatio(zoomState.minZoomRatio)
                }
            }
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error initializing camera", e)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = getCurrentTime()
            currentBatteryLevel.intValue = getCurrentBatteryLevel(context)
            delay(1000.milliseconds)
        }
    }

    LaunchedEffect(isRecordingRunning) {
        while (isRecordingRunning) {
            standByStringIsVisible = false
            recordIconIsVisible = !recordIconIsVisible
            delay(1000.milliseconds)
        }
        recordIconIsVisible = false
        standByStringIsVisible = true
    }

    LaunchedEffect(toolBoxIsVisible) {
        if (toolBoxIsVisible) { delay(3000.milliseconds); toolBoxIsVisible = false }
    }

    LaunchedEffect(Unit) {
        ScreenRecordService.triggerStartRecording.collect {
            if (!isRecordingRunning) screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }

    LaunchedEffect(Unit) {
        ScreenRecordService.triggerStopRecording.collect {
            if (isRecordingRunning) {
                Intent(context.applicationContext, ScreenRecordService::class.java).also {
                    it.action = STOP_RECORDING
                    context.startService(it)
                }
                if (beepSoundApproved) {
                    if (chosenBrand.value == "MOTOROLA") playSoundAtMaxVolume(context, R.raw.motorolastoprecordsound)
                    else playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                }
                if (vibrateApproved) vibrateOnce(context, 1000)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (isBodyDetectionApproved) {
            val currentPose = detectedPoseBoundingBox
            if (currentPose != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / frameWidth.toFloat()
                    val scaleY = size.height / frameHeight.toFloat()
                    val left = currentPose.left * scaleX
                    val top = currentPose.top * scaleY
                    val right = currentPose.right * scaleX
                    val bottom = currentPose.bottom * scaleY
                    drawRect(
                        color = DarkYellow,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        when (chosenBrand.value) {
            "AXON" -> AxonUI(currentTime, currentBatteryLevel, userName, isRecordingRunning, recordIconIsVisible, standByStringIsVisible, toolBoxIsVisible, textShadow, consolasBold, navController, beepSoundApproved, vibrateApproved, lensFacing,
                onToolBoxToggle = { toolBoxIsVisible = !toolBoxIsVisible }, onCameraSwitch = { l, u -> lensFacing = l; useUltraWide = u }, screenRecordLauncher, mediaProjectionManager, isPortrait, isRadioRunning, radioEndpoints, ::toggleRadio)
            "MOTOROLA" -> MotorolaUI(currentTime, currentBatteryLevel, userName, isRecordingRunning, recordIconIsVisible, standByStringIsVisible, toolBoxIsVisible, textShadow, consolasBold, navController, beepSoundApproved, vibrateApproved, lensFacing,
                onToolBoxToggle = { toolBoxIsVisible = !toolBoxIsVisible }, onCameraSwitch = { l, u -> lensFacing = l; useUltraWide = u }, screenRecordLauncher, mediaProjectionManager, isPortrait, isRadioRunning, radioEndpoints, ::toggleRadio)
            "TRANSCEND" -> TranscendUI(currentTime, currentBatteryLevel, userName, isRecordingRunning, recordIconIsVisible, standByStringIsVisible, toolBoxIsVisible, textShadow, consolasBold, navController, beepSoundApproved, vibrateApproved, lensFacing,
                onToolBoxToggle = { toolBoxIsVisible = !toolBoxIsVisible }, onCameraSwitch = { l, u -> lensFacing = l; useUltraWide = u }, screenRecordLauncher, mediaProjectionManager, isPortrait, isRadioRunning, radioEndpoints, ::toggleRadio)
            "GETAC" -> GetacUI(currentTime, currentBatteryLevel, userName, isRecordingRunning, recordIconIsVisible, standByStringIsVisible, toolBoxIsVisible, textShadow, consolasBold, navController, beepSoundApproved, vibrateApproved, lensFacing,
                onToolBoxToggle = { toolBoxIsVisible = !toolBoxIsVisible }, onCameraSwitch = { l, u -> lensFacing = l; useUltraWide = u }, screenRecordLauncher, mediaProjectionManager, isPortrait, isRadioRunning, radioEndpoints, ::toggleRadio)
            "DOZOR" -> DozorUI(currentTime, currentBatteryLevel, userName, isRecordingRunning, recordIconIsVisible, standByStringIsVisible, toolBoxIsVisible, textShadow, consolasBold, navController, beepSoundApproved, vibrateApproved, lensFacing,
                onToolBoxToggle = { toolBoxIsVisible = !toolBoxIsVisible }, onCameraSwitch = { l, u -> lensFacing = l; useUltraWide = u }, screenRecordLauncher, mediaProjectionManager, isPortrait, isRadioRunning, radioEndpoints, ::toggleRadio)
            "PANASONIC" -> PanasonicUI(currentTime, currentBatteryLevel, userName, isRecordingRunning, recordIconIsVisible, standByStringIsVisible, toolBoxIsVisible, textShadow, consolasBold, navController, beepSoundApproved, vibrateApproved, lensFacing,
                onToolBoxToggle = { toolBoxIsVisible = !toolBoxIsVisible }, onCameraSwitch = { l, u -> lensFacing = l; useUltraWide = u }, screenRecordLauncher, mediaProjectionManager, isPortrait, isRadioRunning, radioEndpoints, ::toggleRadio)
        }
    }

    if (instructionAlertDialogApproved && instructionAlertDialogIsVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "Instruction", color = White) },
            text = { Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    SelectionContainer {
                        Text(
                            text =  "●  AXON : Tap top right \u201CAXON\u201D icon to start/stop recording.\n\n" +
                                    "●  MOTOROLA : Tap top left \u201CMOTOROLA\u201D icon to start/stop recording.\n\n" +
                                    "●  TRANSCEND : Tap bottom left \u201CTRANSCEND\u201D icon to start/stop recording.\n\n" +
                                    "●  GETAC : Tap top left \u201CGETAC\u201D icon to start/stop recording.\n\n" +
                                    "●  DOZOR : Tap top right \u201CDOZOR\u201D icon to start/stop recording.\n\n" +
                                    "●  PANASONIC : Tap top right \u201CPANASONIC\u201D icon to start/stop recording.\n\n" +
                                    "●  Record result will be stored in \u201CBodycam\u201D folder in device media store space.\n\n" +
                                    "●  For android 14+ device, you can chose to record \u201CA single app\u201D or \u201CEntire screen\u201D.\n\n" +
                                    "●  Tap the screen then \u201Csettings\u201D 、 \u201Cradio system\u201D 、\u201Ccamera change\u201D and \u201Cmedia storage\u201D buttom will show on the screen.\n\n" +
                                    "●  If you want to use radio system, push the radio buttom on all of your devices then wait for connection, there will be online devices number on the top of the buttom when connected.\n\n" +
                                    "●  You can change the orientation of your device in settings.\n\n" +
                                    "●  Not every device have wide lens, \u201CBodycam\u201D will search wide lens on your device automatically.\n\n" +
                                    "●  User name can be changed.",
                            color = White
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { instructionAlertDialogIsVisible = false }) { Text(color = DarkYellow, text = "close") } },
            dismissButton = { TextButton(onClick = { instructionAlertDialogIsVisible = false; updateInstructionAlertDialogStatus(context, false) }) { Text(color = DarkYellow, text = "close permanently") } }
        )
    }
}

@Composable
fun AxonUI(currentTime: MutableState<String>, currentBatteryLevel: MutableIntState, userName: String, isRecordingRunning: Boolean, recordIconIsVisible: Boolean, standByStringIsVisible: Boolean, toolBoxIsVisible: Boolean, textShadow: Shadow, consolasBold: FontFamily, navController: NavHostController, beepSoundApproved: Boolean, vibrateApproved: Boolean, lensFacing: Int,
           onToolBoxToggle: () -> Unit, onCameraSwitch: (Int, Boolean) -> Unit, screenRecordLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, mediaProjectionManager: MediaProjectionManager, isPortrait: Boolean, isRadioRunning: Boolean, radioEndpoints: Set<String>, toggleRadio: () -> Unit) {
    val context = LocalContext.current
    val brandIconSize = if (isPortrait) 64.dp else 96.dp
    val batteryTextSize = if (isPortrait) 14.sp else 17.5.sp
    val watermarkTextSize = if (isPortrait) 14.sp else 17.5.sp
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToolBoxToggle() }) }) {
        Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
            Text(text = "$userName ${currentTime.value}\n${getPhoneName()}", lineHeight = watermarkTextSize.value.sp, color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = watermarkTextSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
            IconButton(modifier = Modifier.size(brandIconSize), onClick = {
                    if (isRecordingRunning) {
                        Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startService(it) }
                        if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                        if (vibrateApproved) vibrateOnce(context, 1000)
                    } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
                }) { Icon(painter = painterResource(R.mipmap.ic_water_mark_foreground), tint = DarkYellow, contentDescription = "WaterMark", modifier = Modifier.size(brandIconSize)) }
        }
        Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp)) {
            Box(modifier = Modifier.size(48.dp)) {
                Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC Icon", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (recordIconIsVisible) 1f else 0f })
                Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Stand By Icon", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (!recordIconIsVisible && standByStringIsVisible) 1f else 0f })
            }
        }
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.SETTING); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { val nextLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK;onCameraSwitch(nextLens, nextLens == CameraSelector.LENS_FACING_BACK); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound);if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.VIDEO); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_gallery_foreground), tint = White, contentDescription = "Video Library", modifier = Modifier.size(72.dp)) }
            }
            if (isPortrait) AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
                IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } }
            }
        }
        if (!isPortrait) Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) { AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
            IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
            Text(text = "${currentBatteryLevel.intValue}%", color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = batteryTextSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
    }
}

@Composable
fun MotorolaUI(currentTime: MutableState<String>, currentBatteryLevel: MutableIntState, userName: String, isRecordingRunning: Boolean, recordIconIsVisible: Boolean, standByStringIsVisible: Boolean, toolBoxIsVisible: Boolean, textShadow: Shadow, consolasBold: FontFamily, navController: NavHostController, beepSoundApproved: Boolean, vibrateApproved: Boolean, lensFacing: Int,
               onToolBoxToggle: () -> Unit, onCameraSwitch: (Int, Boolean) -> Unit, screenRecordLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, mediaProjectionManager: MediaProjectionManager, isPortrait: Boolean, isRadioRunning: Boolean, radioEndpoints: Set<String>, toggleRadio: () -> Unit) {
    val context = LocalContext.current
    val brandIconSize = if (isPortrait) 64.dp else 96.dp
    val fontSize = if (isPortrait) 14.sp else 17.5.sp
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToolBoxToggle() }) }) {
        Box(modifier = Modifier.fillMaxWidth().height(if (isPortrait) 30.dp else 40.dp).background(Color.Black.copy(alpha = 0.5f)).align(Alignment.TopCenter)) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                IconButton(modifier = Modifier.size(brandIconSize), onClick = {
                    if (isRecordingRunning) {
                        Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startService(it) }
                        if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.motorolastoprecordsound)
                        if (vibrateApproved) vibrateOnce(context, 1000)
                    } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
                }) { Icon(painter = painterResource(R.mipmap.ic_motorola_icon_foreground), tint = White, contentDescription = "WaterMark", modifier = Modifier.size(brandIconSize)) }
            }
            Row(modifier = Modifier.padding(start = if (isPortrait) 50.dp else 70.dp, top = if (isPortrait) 5.dp else 10.dp).align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = "MOTOROLA", color = White, fontWeight = FontWeight(1000), fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), modifier = Modifier.graphicsLayer { scaleY = 0.8f }, fontSize = fontSize)
                Text(text = "SOLUTIONS", color = White, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), modifier = Modifier.graphicsLayer { scaleY = 0.8f }, fontSize = fontSize)
            }
            if (!isPortrait) Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 10.dp)) { Text(text = "${currentTime.value} $userName ${getPhoneName()}", color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f }) }
        }
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = if (isPortrait) 35.dp else 45.dp)) {
            Box(modifier = Modifier.size(if (isPortrait) 32.dp else 48.dp)) {
                Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (recordIconIsVisible) 1f else 0f })
                Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Wait", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (!recordIconIsVisible && standByStringIsVisible) 1f else 0f })
            }
        }
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.SETTING); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { onCameraSwitch(if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK, lensFacing != CameraSelector.LENS_FACING_BACK); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.VIDEO); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_gallery_foreground), tint = White, contentDescription = "Video Library", modifier = Modifier.size(72.dp)) }
            }
            if (isPortrait) AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
                IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } }
            }
        }
        if (!isPortrait) Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) { AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
            IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        if (isPortrait) Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) { Text(text = "${currentTime.value} $userName ${getPhoneName()}", color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f }) }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
            Text(text = "${currentBatteryLevel.intValue}%", color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
    }
}

@Composable
fun TranscendUI(currentTime: MutableState<String>, currentBatteryLevel: MutableIntState, userName: String, isRecordingRunning: Boolean, recordIconIsVisible: Boolean, standByStringIsVisible: Boolean, toolBoxIsVisible: Boolean, textShadow: Shadow, consolasBold: FontFamily, navController: NavHostController, beepSoundApproved: Boolean, vibrateApproved: Boolean, lensFacing: Int,
                onToolBoxToggle: () -> Unit, onCameraSwitch: (Int, Boolean) -> Unit, screenRecordLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, mediaProjectionManager: MediaProjectionManager, isPortrait: Boolean, isRadioRunning: Boolean, radioEndpoints: Set<String>, toggleRadio: () -> Unit) {
    val context = LocalContext.current
    val brandIconSize = if (isPortrait) 64.dp else 96.dp
    val fontSize = if (isPortrait) 14.sp else 17.5.sp
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToolBoxToggle() }) }) {
        Row(modifier = Modifier.align(Alignment.BottomStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
            IconButton(modifier = Modifier.size(brandIconSize), onClick = {
                if (isRecordingRunning) {
                    Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startService(it) }
                    if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                    if (vibrateApproved) vibrateOnce(context, 1000)
                } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
            }) { Icon(painter = painterResource(R.mipmap.ic_transcend_icon_foreground), tint = DarkRed, contentDescription = "WaterMark", modifier = Modifier.size(brandIconSize)) }
            Text(text = "${userName}\n${currentTime.value} ${getPhoneName()}", color = DarkOrange, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f }.padding(bottom = if (isPortrait) 10.dp else 0.dp))
        }
        Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp)) {
            if (recordIconIsVisible) Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC", modifier = Modifier.size(if (isPortrait) 32.dp else 48.dp))
            else if (standByStringIsVisible) Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Wait", modifier = Modifier.size(if (isPortrait) 32.dp else 48.dp))
        }
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.SETTING); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { onCameraSwitch(if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK, lensFacing != CameraSelector.LENS_FACING_BACK); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.VIDEO); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_gallery_foreground), tint = White, contentDescription = "Video Library", modifier = Modifier.size(72.dp)) }
            }
            if (isPortrait) AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
                IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        if (!isPortrait) Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 10.dp)) { AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
            IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
            Text(text = "${currentBatteryLevel.intValue}%", color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
    }
}

@Composable
fun GetacUI(currentTime: MutableState<String>, currentBatteryLevel: MutableIntState, userName: String, isRecordingRunning: Boolean, recordIconIsVisible: Boolean, standByStringIsVisible: Boolean, toolBoxIsVisible: Boolean, textShadow: Shadow, consolasBold: FontFamily, navController: NavHostController, beepSoundApproved: Boolean, vibrateApproved: Boolean, lensFacing: Int,
            onToolBoxToggle: () -> Unit, onCameraSwitch: (Int, Boolean) -> Unit, screenRecordLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, mediaProjectionManager: MediaProjectionManager, isPortrait: Boolean, isRadioRunning: Boolean, radioEndpoints: Set<String>, toggleRadio: () -> Unit) {
    val context = LocalContext.current
    val brandIconSize = if (isPortrait) 64.dp else 96.dp
    val fontSize = if (isPortrait) 14.sp else 17.5.sp
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToolBoxToggle() }) }) {
        Box(modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(if (isPortrait) 30.dp else 40.dp).background(Color.Black.copy(alpha = 0.5f)).align(Alignment.TopCenter)) {
            Column(modifier = Modifier.align(Alignment.TopStart).padding(start = if (isPortrait) 15.dp else 30.dp)) {
                IconButton(modifier = Modifier.size(brandIconSize).scale(if (isPortrait) 1.5f else 2.0f), onClick = {
                    if (isRecordingRunning) {
                        Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startService(it) }
                        if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                        if (vibrateApproved) vibrateOnce(context, 1000)
                    } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
                }) { Icon(painter = painterResource(R.mipmap.ic_getac_icon_foreground), tint = DarkOrange, contentDescription = "WaterMark", modifier = Modifier.size(brandIconSize)) }
            }
            if (!isPortrait) Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 10.dp)) { Text(text = "${currentTime.value} $userName ${getPhoneName()}", color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f }) }
        }
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = if (isPortrait) 55.dp else 70.dp)) {
            Box(modifier = Modifier.size(if (isPortrait) 32.dp else 48.dp)) {
                Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (recordIconIsVisible) 1f else 0f })
                Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Wait", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (!recordIconIsVisible && standByStringIsVisible) 1f else 0f })
            }
        }
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.SETTING); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { onCameraSwitch(if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK, lensFacing != CameraSelector.LENS_FACING_BACK); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.VIDEO); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_gallery_foreground), tint = White, contentDescription = "Video Library", modifier = Modifier.size(72.dp)) }
            }
            if (isPortrait) AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
                IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } }
            }
        }
        if (!isPortrait) Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) { AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
            IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        if (isPortrait) Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) { Text(text = "${currentTime.value} $userName ${getPhoneName()}", color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f }) }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
            Text(text = "${currentBatteryLevel.intValue}%", color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f } )
        }
    }
}

@Composable
fun DozorUI(currentTime: MutableState<String>, currentBatteryLevel: MutableIntState, userName: String, isRecordingRunning: Boolean, recordIconIsVisible: Boolean, standByStringIsVisible: Boolean, toolBoxIsVisible: Boolean, textShadow: Shadow, consolasBold: FontFamily, navController: NavHostController, beepSoundApproved: Boolean, vibrateApproved: Boolean, lensFacing: Int,
            onToolBoxToggle: () -> Unit, onCameraSwitch: (Int, Boolean) -> Unit, screenRecordLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, mediaProjectionManager: MediaProjectionManager, isPortrait: Boolean, isRadioRunning: Boolean, radioEndpoints: Set<String>, toggleRadio: () -> Unit) {
    val context = LocalContext.current
    val brandIconSize = if (isPortrait) 64.dp else 96.dp
    val fontSize = if (isPortrait) 14.sp else 17.5.sp
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToolBoxToggle() }) }) {
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = if (isPortrait) 15.dp else 30.dp).scale(if (isPortrait) 1.2f else 1.5f)) {
            IconButton(modifier = Modifier.size(brandIconSize), onClick = {
                if (isRecordingRunning) {
                    Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startService(it) }
                    if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                    if (vibrateApproved) vibrateOnce(context, 1000)
                } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
            }) { Icon(painter = painterResource(R.mipmap.ic_dozor_icon_foreground), tint = White, contentDescription = "WaterMark", modifier = Modifier.size(brandIconSize)) }
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp)) {
            Text(text = "DZ $userName ${getPhoneName()} *${currentTime.value}", color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = if (isPortrait) 16.sp else 20.sp, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
        Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp)) {
            Box(modifier = Modifier.size(if (isPortrait) 32.dp else 48.dp)) {
                Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (recordIconIsVisible) 1f else 0f })
                Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Wait", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (!recordIconIsVisible && standByStringIsVisible) 1f else 0f })
            }
        }
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.SETTING); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { onCameraSwitch(if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK, lensFacing != CameraSelector.LENS_FACING_BACK); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.VIDEO); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_gallery_foreground), tint = White, contentDescription = "Video Library", modifier = Modifier.size(72.dp)) }
            }
            if (isPortrait) AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
                IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } }
            }
        }
        if (!isPortrait) Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) { AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
            IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = if (isPortrait) 25.dp else 35.dp)) {
            Text(text = "${currentBatteryLevel.intValue}%", color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
    }
}

@Composable
fun PanasonicUI(currentTime: MutableState<String>, currentBatteryLevel: MutableIntState, userName: String, isRecordingRunning: Boolean, recordIconIsVisible: Boolean, standByStringIsVisible: Boolean, toolBoxIsVisible: Boolean, textShadow: Shadow, consolasBold: FontFamily, navController: NavHostController, beepSoundApproved: Boolean, vibrateApproved: Boolean, lensFacing: Int,
                onToolBoxToggle: () -> Unit, onCameraSwitch: (Int, Boolean) -> Unit, screenRecordLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, mediaProjectionManager: MediaProjectionManager, isPortrait: Boolean, isRadioRunning: Boolean, radioEndpoints: Set<String>, toggleRadio: () -> Unit) {
    val context = LocalContext.current
    val brandIconSize = if (isPortrait) 64.dp else 96.dp
    val fontSize = if (isPortrait) 14.sp else 17.5.sp
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToolBoxToggle() }) }) {
        Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 20.dp)) {
            Text(text = "${currentTime.value}\n${userName} ${getPhoneName()}", color = White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), lineHeight = fontSize.value.sp, fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(end = if (isPortrait) 20.dp else 30.dp).scale(if (isPortrait) 1.5f else 2.0f)) {
            IconButton(modifier = Modifier.size(brandIconSize), onClick = {
                if (isRecordingRunning) {
                    Intent(context.applicationContext, ScreenRecordService::class.java).also { it.action = STOP_RECORDING; context.startService(it) }
                    if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.axonstoprecordsound)
                    if (vibrateApproved) vibrateOnce(context, 1000)
                } else { screenRecordLauncher.launch(mediaProjectionManager.createScreenCaptureIntent()) }
            }) { Icon(painter = painterResource(R.mipmap.ic_panasonic1_icon_foreground), tint = LightGreen, contentDescription = "WaterMark", modifier = Modifier.size(brandIconSize)) }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp)) {
            Box(modifier = Modifier.size(if (isPortrait) 32.dp else 48.dp)) {
                Icon(painter = painterResource(R.mipmap.ic_recording_foreground), tint = Red, contentDescription = "REC", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (recordIconIsVisible) 1f else 0f })
                Icon(painter = painterResource(R.drawable.ic_start_record_foreground), tint = DarkYellow, contentDescription = "Wait", modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (!recordIconIsVisible && standByStringIsVisible) 1f else 0f })
            }
        }
        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.SETTING); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_settings_foreground), tint = White, contentDescription = "Settings", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { onCameraSwitch(if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK, lensFacing != CameraSelector.LENS_FACING_BACK); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_camera_switch_foreground), tint = White, contentDescription = "Switch Camera", modifier = Modifier.size(72.dp)) }
            }
            AnimatedVisibility(visible = toolBoxIsVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(modifier = Modifier.size(72.dp), onClick = { navController.navigate(NAV.VIDEO); if (beepSoundApproved) playSoundAtMaxVolume(context, R.raw.buttontouchedsound); if (vibrateApproved) vibrateOnce(context, 1000) }) { Icon(painter = painterResource(R.drawable.ic_gallery_foreground), tint = White, contentDescription = "Video Library", modifier = Modifier.size(72.dp)) }
            }
            if (isPortrait) AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
                IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } }
            }
        }
        if (!isPortrait) Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) { AnimatedVisibility(visible = toolBoxIsVisible || isRadioRunning, enter = fadeIn(), exit = fadeOut()) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (isRadioRunning && radioEndpoints.isNotEmpty()) Text(text = "Online: ${radioEndpoints.size + 1}", color = DarkYellow, fontSize = 12.sp, fontFamily = consolasBold, style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow))
            IconButton(modifier = Modifier.size(72.dp), onClick = { toggleRadio() }) { Icon(painter = painterResource(R.drawable.ic_radio_foreground), tint = if (isRadioRunning) DarkYellow else White, contentDescription = "Radio", modifier = Modifier.size(72.dp)) } } }
        }
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)) {
            Text(text = "${currentBatteryLevel.intValue}%", color = if (currentBatteryLevel.intValue <= 20) Red else if (currentBatteryLevel.intValue <= 50) DarkYellow else White, style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow), fontFamily = consolasBold, fontSize = fontSize, modifier = Modifier.graphicsLayer { scaleX = 0.95f })
        }
    }
}
