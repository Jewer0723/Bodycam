package com.jewer.bodycam.frontend.screens

import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaRecorder
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.functions.getBeepSoundStatus
import com.jewer.bodycam.backend.functions.getBodycamBrand
import com.jewer.bodycam.backend.functions.getCurrentBatteryLevel
import com.jewer.bodycam.backend.functions.getCurrentTime
import com.jewer.bodycam.backend.functions.getInstructionAlertDialogStatus
import com.jewer.bodycam.backend.functions.getPersonDetectStatus
import com.jewer.bodycam.backend.functions.getPhoneName
import com.jewer.bodycam.backend.functions.getUserName
import com.jewer.bodycam.backend.functions.getVibrateAndBeepTimeInterval
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.playSoundAtMaxVolume
import com.jewer.bodycam.backend.functions.updateInstructionAlertDialogStatus
import com.jewer.bodycam.backend.functions.vibrateOnce
import com.jewer.bodycam.backend.objectdetector.ObjectDetectorHelper
import com.jewer.bodycam.backend.objectdetector.ObjectDetectorListener
import com.jewer.bodycam.backend.objectdetector.ResultsOverlay
import com.jewer.bodycam.backend.services.ScreenRecordConfig
import com.jewer.bodycam.backend.services.ScreenRecordService
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.KEY_AUDIO_SOURCE
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.KEY_RECORDING_CONFIG
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.START_RECORDING
import com.jewer.bodycam.backend.services.ScreenRecordService.Companion.STOP_RECORDING
import com.jewer.bodycam.backend.usb.UsbCameraManager
import com.jewer.bodycam.frontend.nav.NAV
import com.jewer.bodycam.ui.theme.Black
import com.jewer.bodycam.ui.theme.DarkOrange
import com.jewer.bodycam.ui.theme.DarkRed
import com.jewer.bodycam.ui.theme.DarkYellow
import com.jewer.bodycam.ui.theme.Red
import com.jewer.bodycam.ui.theme.White
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavHostController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentTime = remember { mutableStateOf(getCurrentTime()) } // 獲取現在時間
    val currentBatteryLevel = remember { mutableIntStateOf(getCurrentBatteryLevel(context)) } // 獲取現在手機電量
    val userName = getUserName(context) // 讀取使用者名稱
    val personDetectApproved = getPersonDetectStatus(context) // 人體辨識授權
    val vibrateApproved = getVibrateStatus(context) // 震動狀態
    val beepSoundApproved = getBeepSoundStatus(context) // 嗶聲授權
    val instructionAlertDialogApproved = getInstructionAlertDialogStatus(context) // 說明書顯示授權
    val chosenTimeInterval = getVibrateAndBeepTimeInterval(context) // 聲響/震動時間間隔
    val chosenBrand = remember { mutableStateOf(getBodycamBrand(context)) } // 密錄器品牌
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) } // 拿取相機供應
    var recordIconIsVisible by remember { mutableStateOf(false) } // 錄影 icon 可見控制
    var standByStringIsVisible by remember { mutableStateOf(false) } // 待機模式字串可見控制
    var toolBoxIsVisible by remember { mutableStateOf(false) } // 工具列可見控制
    var instructionAlertDialogIsVisible by remember { mutableStateOf(true) } // 使用手冊可見控制
    val previewView : PreviewView = remember { PreviewView(context) } // 相機預覽畫面
    var results by remember { mutableStateOf<ObjectDetectorResult?>(null) } // 影像辨識結果保持
    var frameHeight by remember { mutableIntStateOf(4) } // 畫面高度
    var frameWidth by remember { mutableIntStateOf(3) } // 畫面寬度
    var active by remember { mutableStateOf(true) } // 影像辨識啟動旗標
    val mediaProjectionManager by lazy { context.getSystemService<MediaProjectionManager>()!! } // 建立螢幕錄影管理者
    val isServiceRunning by ScreenRecordService.isServiceRunning.collectAsStateWithLifecycle() // 錄影狀態旗標 (連動 service 裡面的旗標)
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var useUltraWide by remember { mutableStateOf(true) }
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
    val isUsbCameraConnected by UsbCameraManager.isUsbCameraConnected
        .collectAsStateWithLifecycle()
    val usbAudioSource by UsbCameraManager.usbAudioSource
        .collectAsStateWithLifecycle()
    val uvcSurfaceView = remember { SurfaceView(context) }

    val screenRecordLauncher = rememberLauncherForActivityResult( // 開始錄影之流程變數
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data ?: return@rememberLauncherForActivityResult
        val config = ScreenRecordConfig(
            resultCode = result.resultCode,
            data = intent
        )

        val serviceIntent = Intent(
            context.applicationContext,
            ScreenRecordService::class.java
        ).apply {
            this.action = START_RECORDING
            putExtra(KEY_RECORDING_CONFIG, config)
            putExtra(
                KEY_AUDIO_SOURCE,
                usbAudioSource ?: MediaRecorder.AudioSource.CAMCORDER
            )
        }
        context.startForegroundService(serviceIntent)
    }

    // 在執行緒中啟動相機預覽
    LaunchedEffect(cameraSelector) {
        val executor = ContextCompat.getMainExecutor(context)
        val backgroundExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                cameraProvider.unbindAll()

                // 如果許可人體辨識，則執行辨識
                val camera = if (personDetectApproved) {
                    val imageAnalyzer =
                        ImageAnalysis.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                    val objectDetectorHelper =
                        ObjectDetectorHelper(
                            context = context,
                            objectDetectorListener = ObjectDetectorListener(
                                onErrorCallback = { _, _ -> },
                                onResultsCallback = {
                                    frameHeight = it.inputImageHeight
                                    frameWidth = it.inputImageWidth
                                    if (active) { results = it.results.first() }
                                }
                            )
                        )
                    imageAnalyzer.setAnalyzer(backgroundExecutor, objectDetectorHelper::detectLivestreamFrame)

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalyzer,
                        preview
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                }

                val minZoom = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
                camera.cameraControl.setZoomRatio(minZoom)// 設定廣角鏡頭

            } catch (e: Exception) {
                Log.e("CameraPreview", "Error initializing camera", e)
            }
        }, executor)
    }

    // 獲取實時時間和電量
    LaunchedEffect(getCurrentTime(), getCurrentBatteryLevel(context)) {
        while (true) {
            currentTime.value = getCurrentTime()
            currentBatteryLevel.intValue = getCurrentBatteryLevel(context)
            delay(1000) // 每1秒更新一次
        }
    }

    // 根據錄影狀態判斷是否顯示錄影中 Icon
    LaunchedEffect(isServiceRunning) {
        while (isServiceRunning) {
            standByStringIsVisible = false // 待機模式旗標關閉
            recordIconIsVisible = !recordIconIsVisible
            delay(1000) // 每1秒更新一次
        }

        // 停止錄影時錄影中 Icon 消失
        recordIconIsVisible = false // 錄影中 Icon 旗標關閉
        standByStringIsVisible = true // 待機模式旗標開啟
    }

    // 每 2 分鐘震動及嗶聲兩次提醒使用者錄影中
    LaunchedEffect(isServiceRunning) {
        while(isServiceRunning) {
            if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                playSoundAtMaxVolume(context, R.raw.axonrecordingsound)
            }
            if (vibrateApproved) { // 如果震動授權
                repeat(2) {
                    vibrateOnce(context, 300)
                    delay(400)
                }
            }
            delay(chosenTimeInterval) // 根據選擇的時間間隔發出聲響/震動一次
        }
    }

    // 工具列點擊後出現(維持3秒)
    LaunchedEffect(toolBoxIsVisible) {
        if (toolBoxIsVisible) {
            delay(3000) // 3秒後自動隱藏
            toolBoxIsVisible = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            active = false
            cameraProviderFuture.get().unbindAll()
        }
    }

    DisposableEffect(Unit) {
        UsbCameraManager.register(
            context = context,
            onAttached = {
                // 插入時若正在錄影先停止
                if (isServiceRunning) {
                    Intent(context.applicationContext, ScreenRecordService::class.java)
                        .also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                    if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                        playSoundAtMaxVolume(context, R.raw.axonstoprecordsound) // 結束錄影聲響
                    }
                    if (vibrateApproved) { // 如果震動授權
                        vibrateOnce(context, 1000) // 震動 1 秒
                    }
                }
                // 請求 USB 權限，授權後 onConnect 會自動啟動預覽
                UsbCameraManager.requestPermission(UsbCameraManager.getUsbDevice())
            },
            onDetached = {
                // 拔出時若正在錄影先停止
                if (isServiceRunning) {
                    Intent(context.applicationContext, ScreenRecordService::class.java)
                        .also { it.action = STOP_RECORDING; context.startForegroundService(it) }
                    if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                        playSoundAtMaxVolume(context, R.raw.axonstoprecordsound) // 結束錄影聲響
                    }
                    if (vibrateApproved) { // 如果震動授權
                        vibrateOnce(context, 1000) // 震動 1 秒
                    }
                }
            }
        )
        onDispose { UsbCameraManager.unregister() }
    }

    DisposableEffect(isUsbCameraConnected) {
        if (isUsbCameraConnected) {
            // 等 SurfaceView 的 Surface 建立完成後再傳入
            uvcSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    UsbCameraManager.setPreviewSurface(holder.surface)
                }
                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    UsbCameraManager.setPreviewSurface(null)
                }
            })
        }
        onDispose {}
    }

    when(chosenBrand.value) {
        "AXON" -> {
            Box( // 相機預覽畫面box
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { // 點擊範圍為全螢幕
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(
                                        context,
                                        "Tap top right “AXON” icon to start/stop recording",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
            ) {
                if (isUsbCameraConnected) {
                    // USB 外接攝像頭預覽
                    AndroidView(
                        factory = { uvcSurfaceView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 原本的手機鏡頭預覽（不動）
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text( // 時間浮水印、手機型號資訊
                        text = userName + "   " +
                                currentTime.value + "\n" +
                                getPhoneName(),
                        color = White,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            shadow = Shadow(
                                color = Black,
                                offset = Offset(3f, 3f),
                                blurRadius = 5f
                            )
                        )
                    )

                    IconButton(
                        modifier = Modifier.size(96.dp),
                        onClick = {
                            if (isServiceRunning) { // 如果正在錄影，則停止錄影
                                Intent(
                                    context.applicationContext,
                                    ScreenRecordService::class.java
                                ).also {
                                    it.action = STOP_RECORDING
                                    context.startForegroundService(it)
                                }
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstoprecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            } else {
                                screenRecordLauncher.launch(
                                    mediaProjectionManager.createScreenCaptureIntent()
                                )
                            }
                        }
                    ) {
                        Icon( // Icon浮水印錄影按鈕
                            painter = painterResource(R.mipmap.ic_water_mark_foreground),
                            tint = DarkYellow,
                            contentDescription = "WaterMark",
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp)
                ) {
                    if (recordIconIsVisible) { // 錄影中 icon 、待機模式 icon box，顯示錄影中 icon (每秒閃一次)
                        Icon(
                            painter = painterResource(R.mipmap.ic_recording_foreground),
                            tint = Red,
                            contentDescription = "REC Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (standByStringIsVisible) {
                        Icon(
                            painter = painterResource(R.drawable.ic_start_record_foreground),
                            tint = DarkYellow,
                            contentDescription = "Stand By Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                ) {
                    AnimatedVisibility( // 點擊後出現工具列 (漸入漸出)
                        visible = toolBoxIsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton( // 設定按鈕
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                navController.navigate(NAV.SETTING)
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstartrecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_foreground),
                                tint = White,
                                contentDescription = "Settings",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    AnimatedVisibility( // 點擊後出現工具列 (漸入漸出)
                        visible = toolBoxIsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton( // 相機切換按鈕
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    lensFacing = CameraSelector.LENS_FACING_FRONT
                                    useUltraWide = false
                                } else {
                                    lensFacing = CameraSelector.LENS_FACING_BACK
                                    useUltraWide = true
                                }
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstartrecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_camera_switch_foreground),
                                tint = White,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Text( // 顯示電量
                    text = currentBatteryLevel.intValue.toString() + "%",
                    color = if (currentBatteryLevel.intValue <= 50) DarkYellow
                    else if (currentBatteryLevel.intValue <= 20) Red
                    else White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(
                            color = Black,
                            offset = Offset(3f, 3f),
                            blurRadius = 5f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(48.dp)
                )
            }
        }

        "MOTOROLA" -> {
            Box( // 相機預覽畫面box
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { // 點擊範圍為全螢幕
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(
                                        context,
                                        "Tap top left “Motorola” icon to start/stop recording",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
            ) {
                if (isUsbCameraConnected) {
                    // USB 外接攝像頭預覽
                    AndroidView(
                        factory = { uvcSurfaceView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 原本的手機鏡頭預覽（不動）
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        IconButton(
                            modifier = Modifier.size(96.dp),
                            onClick = {
                                if (isServiceRunning) { // 如果正在錄影，則停止錄影
                                    Intent(
                                        context.applicationContext,
                                        ScreenRecordService::class.java
                                    ).also {
                                        it.action = STOP_RECORDING
                                        context.startForegroundService(it)
                                    }
                                    if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                        playSoundAtMaxVolume(context, R.raw.axonstoprecordsound) // 結束錄影聲響
                                    }
                                    if (vibrateApproved) { // 如果震動授權
                                        vibrateOnce(context, 1000) // 震動 1 秒
                                    }
                                } else {
                                    screenRecordLauncher.launch(
                                        mediaProjectionManager.createScreenCaptureIntent()
                                    )
                                }
                            }
                        ) {
                            Icon( // Icon浮水印錄影按鈕
                                painter = painterResource(R.mipmap.ic_motorola_icon_foreground),
                                tint = White,
                                contentDescription = "WaterMark",
                                modifier = Modifier.size(96.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(start = 70.dp, top = 10.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "MOTOROLA",
                            color = White,
                            fontWeight = FontWeight(1000),
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                shadow = Shadow(
                                    color = Black,
                                    offset = Offset(3f, 3f),
                                    blurRadius = 5f
                                )
                            ),
                            modifier = Modifier.graphicsLayer {
                                scaleY = 0.8f
                            }
                        )

                        Text(
                            text = "SOLUTIONS",
                            color = White,
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                shadow = Shadow(
                                    color = Black,
                                    offset = Offset(3f, 3f),
                                    blurRadius = 5f
                                )
                            ),
                            modifier = Modifier.graphicsLayer {
                                scaleY = 0.8f
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 30.dp, top = 10.dp)
                    ) {
                        Text( // 時間浮水印、手機型號資訊
                            text = currentTime.value + " " + userName + " " + getPhoneName(),
                            color = White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                shadow = Shadow(
                                    color = Black,
                                    offset = Offset(3f, 3f),
                                    blurRadius = 5f
                                )
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 10.dp, top = 40.dp)
                ) {
                    if (recordIconIsVisible) { // 錄影中 icon 、待機模式 icon box，顯示錄影中 icon (每秒閃一次)
                        Icon(
                            painter = painterResource(R.mipmap.ic_recording_foreground),
                            tint = Red,
                            contentDescription = "REC Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (standByStringIsVisible) {
                        Icon(
                            painter = painterResource(R.drawable.ic_start_record_foreground),
                            tint = DarkYellow,
                            contentDescription = "Stand By Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                ) {
                    AnimatedVisibility( // 點擊後出現工具列 (漸入漸出)
                        visible = toolBoxIsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton( // 設定按鈕
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                navController.navigate(NAV.SETTING)
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstartrecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_foreground),
                                tint = White,
                                contentDescription = "Settings",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    AnimatedVisibility( // 點擊後出現工具列 (漸入漸出)
                        visible = toolBoxIsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton( // 相機切換按鈕
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    lensFacing = CameraSelector.LENS_FACING_FRONT
                                    useUltraWide = false
                                } else {
                                    lensFacing = CameraSelector.LENS_FACING_BACK
                                    useUltraWide = true
                                }
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstartrecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_camera_switch_foreground),
                                tint = White,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Text( // 顯示電量
                    text = currentBatteryLevel.intValue.toString() + "%",
                    color = if (currentBatteryLevel.intValue <= 50) DarkYellow
                    else if (currentBatteryLevel.intValue <= 20) Red
                    else White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(
                            color = Black,
                            offset = Offset(3f, 3f),
                            blurRadius = 5f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(48.dp)
                )
            }
        }

        "TRANSCEND" -> {
            Box( // 相機預覽畫面box
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { // 點擊範圍為全螢幕
                        detectTapGestures(
                            onTap = {
                                toolBoxIsVisible = !toolBoxIsVisible
                                if (!isServiceRunning) {
                                    Toast.makeText(
                                        context,
                                        "Tap bottom left “TRANSCEND” icon to start/stop recording",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
            ) {
                if (isUsbCameraConnected) {
                    // USB 外接攝像頭預覽
                    AndroidView(
                        factory = { uvcSurfaceView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 原本的手機鏡頭預覽（不動）
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.size(96.dp),
                        onClick = {
                            if (isServiceRunning) { // 如果正在錄影，則停止錄影
                                Intent(
                                    context.applicationContext,
                                    ScreenRecordService::class.java
                                ).also {
                                    it.action = STOP_RECORDING
                                    context.startForegroundService(it)
                                }
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstoprecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            } else {
                                screenRecordLauncher.launch(
                                    mediaProjectionManager.createScreenCaptureIntent()
                                )
                            }
                        }
                    ) {
                        Icon( // Icon浮水印錄影按鈕
                            painter = painterResource(R.mipmap.ic_transcend_icon_foreground),
                            tint = DarkRed,
                            contentDescription = "WaterMark",
                            modifier = Modifier.size(96.dp)
                        )
                    }

                    Text( // 時間浮水印、手機型號資訊
                        text =  userName + "\n" +
                                currentTime.value + " " + getPhoneName(),
                        color = DarkOrange,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            shadow = Shadow(
                                color = Black,
                                offset = Offset(3f, 3f),
                                blurRadius = 5f
                            )
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp)
                ) {
                    if (recordIconIsVisible) { // 錄影中 icon 、待機模式 icon box，顯示錄影中 icon (每秒閃一次)
                        Icon(
                            painter = painterResource(R.mipmap.ic_recording_foreground),
                            tint = Red,
                            contentDescription = "REC Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (standByStringIsVisible) {
                        Icon(
                            painter = painterResource(R.drawable.ic_start_record_foreground),
                            tint = DarkYellow,
                            contentDescription = "Stand By Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                ) {
                    AnimatedVisibility( // 點擊後出現工具列 (漸入漸出)
                        visible = toolBoxIsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton( // 設定按鈕
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                navController.navigate(NAV.SETTING)
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstartrecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_foreground),
                                tint = White,
                                contentDescription = "Settings",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    AnimatedVisibility( // 點擊後出現工具列 (漸入漸出)
                        visible = toolBoxIsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton( // 相機切換按鈕
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    lensFacing = CameraSelector.LENS_FACING_FRONT
                                    useUltraWide = false
                                } else {
                                    lensFacing = CameraSelector.LENS_FACING_BACK
                                    useUltraWide = true
                                }
                                if (beepSoundApproved) { // 如果嗶聲授權再發出聲響
                                    playSoundAtMaxVolume(context, R.raw.axonstartrecordsound) // 結束錄影聲響
                                }
                                if (vibrateApproved) { // 如果震動授權
                                    vibrateOnce(context, 1000) // 震動 1 秒
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_camera_switch_foreground),
                                tint = White,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Text( // 顯示電量
                    text = currentBatteryLevel.intValue.toString() + "%",
                    color = if (currentBatteryLevel.intValue <= 50) DarkYellow
                    else if (currentBatteryLevel.intValue <= 20) Red
                    else White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(
                            color = Black,
                            offset = Offset(3f, 3f),
                            blurRadius = 5f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(48.dp)
                )
            }
        }
    }

    // 使用手冊對話框
    if (instructionAlertDialogApproved && instructionAlertDialogIsVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(text = "Instruction", color = White)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer{
                        Text(
                            text =  "●  AXON : Tap top right “AXON” icon to start/stop recording.\n" +
                                    "\n" +
                                    "●  MOTOROLA : Tap top left “MOTOROLA” icon to start/stop recording.\n" +
                                    "\n" +
                                    "●  TRANSCEND : Tap bottom left “TRANSCEND” icon to start/stop recording.\n" +
                                    "\n" +
                                    "●  Record result will be stored in “Bodycam” folder in device media store space.\n" +
                                    "\n" +
                                    "●  For android 14+ device, you can chose to record “A single app” or “Entire screen”.\n" +
                                    "\n" +
                                    "●  Tap the screen then “settings” and “camera change” will show on the left side of screen.\n" +
                                    "\n" +
                                    "●  Tap the screen then “record instruction” will show on the screen again.\n" +
                                    "\n" +
                                    "●  User name can be changed.",
                            color = White
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        instructionAlertDialogIsVisible = false // 關閉此對話框
                    }
                ) {
                    Text(
                        color = DarkYellow,
                        text = "close"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        instructionAlertDialogIsVisible = false // 關閉此對話框
                        updateInstructionAlertDialogStatus(context, false) // 永久關閉此對話框
                    }
                ) {
                    Text(
                        color = DarkYellow,
                        text = "close and do not show again"
                    )
                }
            }
        )
    }

    // 物件方框繪製
    results?.let {
        ResultsOverlay(
            context = context,
            results = it,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }
}