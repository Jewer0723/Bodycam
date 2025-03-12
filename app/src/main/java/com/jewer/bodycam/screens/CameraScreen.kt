package com.jewer.bodycam.screens

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.jewer.bodycam.NAV
import com.jewer.bodycam.R
import com.jewer.bodycam.functions.getCurrentBatteryLevel
import com.jewer.bodycam.functions.getCurrentTime
import com.jewer.bodycam.functions.getPersonDetectStatus
import com.jewer.bodycam.functions.getPhoneName
import com.jewer.bodycam.functions.getUserName
import com.jewer.bodycam.objectdetector.ObjectDetectorHelper
import com.jewer.bodycam.objectdetector.ObjectDetectorListener
import com.jewer.bodycam.objectdetector.ResultsOverlay
import com.jewer.bodycam.services.ScreenRecordConfig
import com.jewer.bodycam.services.ScreenRecordService
import com.jewer.bodycam.services.ScreenRecordService.Companion.KEY_RECORDING_CONFIG
import com.jewer.bodycam.services.ScreenRecordService.Companion.START_RECORDING
import com.jewer.bodycam.services.ScreenRecordService.Companion.STOP_RECORDING
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavHostController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // 獲取現在時間
    val currentTime = remember {
        mutableStateOf(getCurrentTime())
    }

    // 獲取現在手機電量
    val currentBatteryLevel = remember {
        mutableIntStateOf(getCurrentBatteryLevel(context))
    }

    // 讀取使用者名稱
    val userName = getUserName(context)

    // 人體辨識授權
    val personDetectApproved = getPersonDetectStatus(context)

    // 拿取相機供應
    var cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    // 錄影 icon 可見控制
    var recordIconIsVisible by remember {
        mutableStateOf(false)
    }

    // 待機模式字串可見控制
    var standByStringIsVisible by remember {
        mutableStateOf(false)
    }

    // 工具列可見控制
    var toolBoxIsVisible by remember {
        mutableStateOf(false)
    }

    // 相機選擇控制
    var cameraSelector by remember {
        mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
    }
    /*
    val audioEnable : MutableState<Boolean> = remember {
        mutableStateOf(false)
    }
     */
    // 浮水印row寬度計算
    var waterMarkRowWidth by remember {
        mutableFloatStateOf(0f)
    }

    // 工具列 row 寬度計算
    var toolBarRowWidth by remember {
        mutableFloatStateOf(0f)
    }

    // 相機預覽畫面
    var previewView : PreviewView = remember {
        PreviewView(context)
    }

    // 影像辨識結果保持
    var results by remember {
        mutableStateOf<ObjectDetectorResult?>(null)
    }

    // 畫面高度
    var frameHeight by remember {
        mutableIntStateOf(4)
    }

    // 畫面寬度
    var frameWidth by remember {
        mutableIntStateOf(3)
    }

    // 影像辨識啟動旗標
    var active by remember {
        mutableStateOf(true)
    }

    /**********************************螢幕錄影變數專區*******************************************/

    // 建立螢幕錄影管理者
    val mediaProjectionManager by lazy {
        context.getSystemService<MediaProjectionManager>()!!
    }

    // 錄影狀態旗標 (連動 service 裡面的旗標)
    val isServiceRunning by ScreenRecordService
        .isServiceRunning
        .collectAsStateWithLifecycle()

    // 開始錄影之流程變數
    val screenRecordLauncher = rememberLauncherForActivityResult(
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
            action = START_RECORDING
            putExtra(KEY_RECORDING_CONFIG, config)
        }
        context.startForegroundService(serviceIntent)
    }

    /**************************************************************************************************************/

    // 相機預覽 function
    fun camPreview(): PreviewView {
        val executor = ContextCompat.getMainExecutor(context)
        val backgroundExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // We set a surface for the camera input feed to be displayed in, which is
                // in the camera preview view we just instantiated
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // We specify what phone camera to use. In our case it's the back camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraSelector)
                    .build()

                // 如果許可人體辨識，則執行辨識
                if (personDetectApproved) {
                    // We instantiate an image analyser to apply some transformations on the
                    // input frame before feeding it to the object detector
                    val imageAnalyzer =
                        ImageAnalysis.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                    // To apply object detection, we use our ObjectDetectorHelper class,
                    // which abstracts away the specifics of using MediaPipe  for object
                    // detection from the UI elements of the app
                    val objectDetectorHelper =
                        ObjectDetectorHelper(
                            context = context,
                            // Since we're detecting objects in a live camera feed, we need
                            // to have a way to listen for the results
                            objectDetectorListener = ObjectDetectorListener(
                                onErrorCallback = { _, _ -> },
                                onResultsCallback = {
                                    // On receiving results, we now have the exact camera
                                    // frame dimensions, so we set them here
                                    frameHeight = it.inputImageHeight
                                    frameWidth = it.inputImageWidth

                                    // Then we check if the camera view is still active,
                                    // if so, we set the state of the results and
                                    // inference time.
                                    if (active) {
                                        results = it.results.first()
                                    }
                                }
                            )
                        )

                    // Now that we have our ObjectDetectorHelper instance, we set is as an
                    // analyzer and start detecting objects from the camera live stream
                    imageAnalyzer.setAnalyzer(
                        backgroundExecutor,
                        objectDetectorHelper::detectLivestreamFrame
                    )

                    // We close any currently open camera just in case, then open up
                    // our own to be display the live camera feed
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalyzer,
                        preview
                    )
                } else {
                    // We close any currently open camera just in case, then open up
                    // our own to be display the live camera feed
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error initializing camera", e)
            }
        }, executor)
        // We return our preview view from the AndroidView factory to display it
        return previewView
    }

    // 在執行緒中啟動相機預覽
    LaunchedEffect(cameraSelector) {
        camPreview()
    }

    // 退出 compose 時解綁相機，生命週期結束
    DisposableEffect(Unit) {
        onDispose {
            active = false
            cameraProviderFuture.get().unbindAll()
        }
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
        if (!isServiceRunning) {
            recordIconIsVisible = false // 錄影中 Icon 旗標關閉
            standByStringIsVisible = true // 待機模式旗標開啟
        }
    }

    // 工具列點擊後出現(維持3秒)
    LaunchedEffect(toolBoxIsVisible) {
        if (toolBoxIsVisible) {
            delay(3000) // 3秒後自動隱藏
            toolBoxIsVisible = false
        }
    }

    // 相機預覽畫面box
    Box(
        modifier = Modifier
            .fillMaxSize()

            // 點擊範圍為全螢幕
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        toolBoxIsVisible = !toolBoxIsVisible

                        if (!isServiceRunning) {
                            Toast.makeText(context, "Tap top right “Bodycam” icon to start/stop recording", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
    ) {
        //相機預覽畫面
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(
                modifier = Modifier
                    .onSizeChanged { size ->
                        waterMarkRowWidth = size.width.toFloat()
                    }
                    .graphicsLayer(
                        translationX = waterMarkRowWidth * 0.4f,
                        translationY = -waterMarkRowWidth * 0.3f,
                        rotationZ = 90f
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 時間浮水印、手機型號資訊
                Text(
                    text = userName + "   " +
                            currentTime.value + "\n" +
                            getPhoneName(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(
                            color = MaterialTheme.colorScheme.onPrimary,
                            offset = Offset(3f, 3f),
                            blurRadius = 5f
                        )
                    )
                )

                IconButton(
                    modifier = Modifier.size(96.dp),
                    onClick = {
                        // 如果正在錄影，則停止錄影
                        if (isServiceRunning) {
                            Intent(
                                context.applicationContext,
                                ScreenRecordService::class.java
                            ).also {
                                it.action = STOP_RECORDING
                                context.startForegroundService(it)
                            }
                        } else {
                            screenRecordLauncher.launch(
                                mediaProjectionManager.createScreenCaptureIntent()
                            )
                        }
                    }
                ) {
                    // Icon浮水印錄影按鈕
                    Icon(
                        painter = painterResource(R.mipmap.ic_water_mark_foreground),
                        tint = MaterialTheme.colorScheme.secondary,
                        contentDescription = "WaterMark",
                        modifier = Modifier
                            .size(96.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 點擊後出現工具列 (漸入漸出)
            AnimatedVisibility(
                visible = toolBoxIsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // 工具列 box
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // 包括影片庫、設定和鏡頭切換按鈕
                    Row(
                        modifier = Modifier
                            .onSizeChanged { size ->
                                toolBarRowWidth = size.width.toFloat()
                            }
                            .graphicsLayer(
                                translationX = -toolBarRowWidth * 0.3f,
                                rotationZ = 90f
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // 設定按鈕
                        IconButton(
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                navController.navigate(NAV.SETTING)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_foreground),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "Settings",
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        // 相機切換按鈕
                        IconButton(
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                cameraSelector =
                                    if (cameraSelector == CameraSelector.LENS_FACING_BACK)
                                        CameraSelector.LENS_FACING_FRONT
                                    else CameraSelector.LENS_FACING_BACK
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_camera_switch_foreground),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }
            }
        }

        // 錄影中 icon 、待機模式 icon box
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            // 顯示錄影中 icon (每秒閃一次)
            if (recordIconIsVisible) {
                Icon(
                    painter = painterResource(R.mipmap.ic_recording_foreground),
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = "REC Icon",
                    modifier = Modifier
                        .graphicsLayer(rotationZ = 90f)
                        .size(48.dp)
                )
            } else if (standByStringIsVisible) {
                Icon(
                    painter = painterResource(R.drawable.ic_start_record_foreground),
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = "Stand By Icon",
                    modifier = Modifier
                        .graphicsLayer(rotationZ = 90f)
                        .size(48.dp)
                )
            }
        }

        // 顯示電量
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = currentBatteryLevel.intValue.toString() + "%",
                color = if (currentBatteryLevel.intValue <= 50) MaterialTheme.colorScheme.secondary
                else if (currentBatteryLevel.intValue <= 20) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.onPrimary,
                        offset = Offset(3f, 3f),
                        blurRadius = 5f
                    )
                ),
                modifier = Modifier
                    .graphicsLayer(rotationZ = 90f)
                    .size(48.dp)
            )
        }
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