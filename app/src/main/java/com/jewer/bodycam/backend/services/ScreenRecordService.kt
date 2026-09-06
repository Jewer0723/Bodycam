package com.jewer.bodycam.backend.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jewer.bodycam.MainActivity
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.functions.getBeepSoundStatus
import com.jewer.bodycam.backend.functions.getBodycamBrand
import com.jewer.bodycam.backend.functions.getVibrateAndBeepTimeInterval
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.playSoundAtMaxVolume
import com.jewer.bodycam.backend.functions.vibrateOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class ScreenRecordConfig(
    val resultCode: Int,
    val data: Intent
): Parcelable

class ScreenRecordService: Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recordingStartTime: Long = 0
    
    private var isResourcesReleased = false

    private val mediaRecorder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(applicationContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            releaseResources()
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必須先建立頻道，且確保在所有路徑下都調用 startForeground
        createNotificationChannel()
        val notification = createNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                startForeground(1, notification, type)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "Failed to start foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent == null) {
            stopService()
            return START_NOT_STICKY
        }

        when(intent.action) {
            START_RECORDING -> {
                try {
                    val config = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(KEY_RECORDING_CONFIG, ScreenRecordConfig::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(KEY_RECORDING_CONFIG)
                    }

                    if (config == null || mediaProjectionManager == null) {
                        Log.e("ScreenRecordService", "Config or Manager is null")
                        stopService()
                        return START_NOT_STICKY
                    }

                    // Android 14+ 嚴格要求 getMediaProjection 必須在 FGS 啟動後調用
                    // 增加 500ms 延遲以確保系統 FGS 狀態同步完成，防止 SecurityException
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            mediaProjection = mediaProjectionManager?.getMediaProjection(config.resultCode, config.data)

                            if (mediaProjection != null) {
                                mediaProjection?.registerCallback(mediaProjectionCallback, Handler(Looper.getMainLooper()))
                                isResourcesReleased = false
                                _isServiceRunning.value = true

                                serviceScope.launch {
                                    if (initializeRecorder()) {
                                        try {
                                            virtualDisplay = createVirtualDisplay()
                                            mediaRecorder.start()
                                            recordingStartTime = System.currentTimeMillis()
                                            Log.d("ScreenRecordService", "Recording started successfully")
                                            startPeriodicBeep()
                                        } catch (e: Exception) {
                                            Log.e("ScreenRecordService", "MediaRecorder start failed", e)
                                            stopService()
                                        }
                                    } else {
                                        Log.e("ScreenRecordService", "initializeRecorder failed")
                                        stopService()
                                    }
                                }
                            } else {
                                Log.e("ScreenRecordService", "MediaProjection is null")
                                stopService()
                            }
                        } catch (e: SecurityException) {
                            Log.e("ScreenRecordService", "SecurityException in getMediaProjection", e)
                            stopService()
                        }
                    }, 500)
                } catch (e: Exception) {
                    Log.e("ScreenRecordService", "Error processing START_RECORDING", e)
                    stopService()
                }
            }
            STOP_RECORDING -> {
                // 修正：將錄影停止移至背景執行緒
                serviceScope.launch {
                    stopRecordingLogic()
                }
            }
        }
        return START_NOT_STICKY // 防止無效 Intent 自動重啟
    }

    private fun stopRecordingLogic() {
        try {
            if (System.currentTimeMillis() - recordingStartTime > 1000) {
                mediaRecorder.stop()
            }
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "mediaRecorder.stop failed", e)
        } finally {
            // 防禦性檢查：若資源已釋放則不執行 reset，避免 IllegalStateException
            if (!isResourcesReleased) {
                try {
                    mediaRecorder.reset()
                } catch (e: Exception) {
                    Log.e("ScreenRecordService", "mediaRecorder.reset failed", e)
                }
            }
            mediaProjection?.stop()
            stopService()
        }
    }

    private fun stopService() {
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startPeriodicBeep() {
        serviceScope.launch {
            while (_isServiceRunning.value) {
                val beepApproved = getBeepSoundStatus(applicationContext)
                val vibrateApproved = getVibrateStatus(applicationContext)
                val brand = getBodycamBrand(applicationContext)
                val interval = getVibrateAndBeepTimeInterval(applicationContext)

                if (beepApproved) {
                    val soundRes = if (brand == "MOTOROLA") R.raw.motorolastartrecordsound else R.raw.axonstartrecordsound
                    playSoundAtMaxVolume(applicationContext, soundRes)
                }
                if (vibrateApproved) {
                    repeat(2) {
                        vibrateOnce(applicationContext, 300)
                        delay(400.milliseconds)
                    }
                }
                delay(interval.milliseconds)
            }
        }
    }

    private fun initializeRecorder(): Boolean {
        return try {
            val filenameFormat = "yyyy-MM-dd-HH-mm-ss"
            val videoName = SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/${getString(R.string.app_name)}")
                }
            }

            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val videoUri = contentResolver.insert(videoCollection, contentValues)
                ?: throw IOException("Failed to create new MediaStore record.")

            val pfd = contentResolver.openFileDescriptor(videoUri, "rw")
                ?: throw FileNotFoundException("Failed to open file descriptor")

            // 修正 5: 確保寬高是 2 的倍數，這對許多硬體編碼器是強制的
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.let { if (it % 2 != 0) it - 1 else it }
            val height = metrics.heightPixels.let { if (it % 2 != 0) it - 1 else it }

            with(mediaRecorder) {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(pfd.fileDescriptor)
                setVideoSize(width, height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setAudioChannels(1)
                setVideoEncodingBitRate(6 * 1024 * 1024)
                setVideoFrameRate(30)
                prepare()
            }
            true
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "initializeRecorder failed", e)
            false
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        val metrics = resources.displayMetrics
        // 寬高同樣確保為 2 的倍數
        val width = metrics.widthPixels.let { if (it % 2 != 0) it - 1 else it }
        val height = metrics.heightPixels.let { if (it % 2 != 0) it - 1 else it }

        return mediaProjection?.createVirtualDisplay(
            "ScreenRecord",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface,
            null,
            null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
        _isServiceRunning.value = false
        serviceScope.coroutineContext.cancelChildren()
    }

    private fun releaseResources() {
        if (isResourcesReleased) return
        try {
            virtualDisplay?.release()
            mediaProjection?.unregisterCallback(mediaProjectionCallback)
            mediaProjection?.stop()
            mediaRecorder.release()
            isResourcesReleased = true
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "releaseResources error", e)
        } finally {
            virtualDisplay = null
            mediaProjection = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notifyTitle = "Recording..."
        val notifyText = when(getBodycamBrand(this)) {
            "AXON" -> "Tap top right “AXON” icon to stop recording"
            "MOTOROLA" -> "Tap top left “MOTOROLA” icon to stop recording"
            "TRANSCEND" -> "Tap bottom left “TRANSCEND” icon to stop recording"
            "GETAC" -> "Tap top left “GETAC” icon to stop recording"
            "DOZOR" -> "Tap top right “DOZOR” icon to stop recording"
            "PANASONIC" -> "Tap top right “PANASONIC” icon to stop recording"
            else -> "Tap the icon to stop recording"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notifyTitle)
            .setContentText(notifyText)
            .setSmallIcon(R.mipmap.ic_water_mark_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Screen Recording Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    companion object {
        private const val CHANNEL_ID = "screen_recording_channel"
        private val _isServiceRunning = MutableStateFlow(false)
        val isRecordingRunning = _isServiceRunning.asStateFlow()

        // 按鍵觸發錄影信號
        private val _triggerStartRecording = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val triggerStartRecording = _triggerStartRecording.asSharedFlow()

        private val _triggerStopRecording = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val triggerStopRecording = _triggerStopRecording.asSharedFlow()

        fun startViaKey() = _triggerStartRecording.tryEmit(Unit)
        fun stopViaKey() = _triggerStopRecording.tryEmit(Unit)

        const val START_RECORDING = "START_RECORDING"
        const val STOP_RECORDING = "STOP_RECORDING"
        const val KEY_RECORDING_CONFIG = "KEY_RECORDING_CONFIG"
    }
}
