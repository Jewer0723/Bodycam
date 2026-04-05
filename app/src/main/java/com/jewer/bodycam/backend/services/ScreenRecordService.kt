package com.jewer.bodycam.backend.services

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
import android.os.IBinder
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.getSystemService
import com.jewer.bodycam.R
import com.jewer.bodycam.backend.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.parcelize.Parcelize
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

@Parcelize
data class ScreenRecordConfig(
    val resultCode: Int,
    val data: Intent
): Parcelable

class ScreenRecordService: Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recordingStartTime: Long = 0
    
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
        getSystemService<MediaProjectionManager>()
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            releaseResources()
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 修正 1: 處理 Intent 為 null 的情況 (由系統重啟時發生)
        if (intent == null) {
            stopService()
            return START_NOT_STICKY
        }

        when(intent.action) {
            START_RECORDING -> {
                val notification = NotificationHelper.createNotification(applicationContext)
                NotificationHelper.createNotificationChannel(applicationContext)
                
                try {
                    // 修正 2: 確保前台服務啟動異常時不會崩潰
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            1,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(1, notification)
                    }
                    _isServiceRunning.value = true
                    startRecording(intent)
                } catch (e: Exception) {
                    Log.e("ScreenRecordService", "Failed to start foreground service", e)
                    stopService()
                }
            }
            STOP_RECORDING -> {
                stopRecordingLogic()
            }
        }
        return START_NOT_STICKY // 修正 3: 防止無效 Intent 自動重啟
    }

    private fun startRecording(intent: Intent) {
        val config = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(KEY_RECORDING_CONFIG, ScreenRecordConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(KEY_RECORDING_CONFIG)
        }
        
        if(config == null || mediaProjectionManager == null) {
            Log.e("ScreenRecordService", "Recording config is null")
            stopService()
            return
        }

        try {
            mediaProjection = mediaProjectionManager?.getMediaProjection(
                config.resultCode,
                config.data
            )
            mediaProjection?.registerCallback(mediaProjectionCallback, null)

            if (initializeRecorder()) {
                mediaRecorder.start()
                recordingStartTime = System.currentTimeMillis() // 記錄開始時間
                virtualDisplay = createVirtualDisplay()
            } else {
                stopService()
            }
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "Error in startRecording", e)
            stopService()
        }
    }

    private fun stopRecordingLogic() {
        try {
            // 修正 4: 確保錄製時間足夠長，避免 recorder.stop() 狀態異常崩潰
            if (System.currentTimeMillis() - recordingStartTime > 1000) {
                mediaRecorder.stop()
            }
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "mediaRecorder.stop failed", e)
        } finally {
            mediaRecorder.reset()
            mediaProjection?.stop()
            stopService()
        }
    }

    private fun stopService() {
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        try {
            virtualDisplay?.release()
            mediaProjection?.unregisterCallback(mediaProjectionCallback)
            mediaProjection?.stop()
            mediaRecorder.release()
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "releaseResources error", e)
        } finally {
            virtualDisplay = null
            mediaProjection = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        const val START_RECORDING = "START_RECORDING"
        const val STOP_RECORDING = "STOP_RECORDING"
        const val KEY_RECORDING_CONFIG = "KEY_RECORDING_CONFIG"
    }
}
