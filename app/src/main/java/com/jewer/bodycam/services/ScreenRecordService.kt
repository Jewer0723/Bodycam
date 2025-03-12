package com.jewer.bodycam.services

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
import androidx.core.content.getSystemService
import com.jewer.bodycam.R
import com.jewer.bodycam.notifications.NotificationHelper
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
    private val mediaRecorder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(applicationContext)
        } else {
            MediaRecorder()
        }
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mediaProjectionManager by lazy {
        getSystemService<MediaProjectionManager>()
    }

    // 建立 MediaProjection Callback
    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            releaseResources()
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            // 收到開始錄影指令
            START_RECORDING -> {
                val notification = NotificationHelper.createNotification(applicationContext) // 更新前台服務通知
                NotificationHelper.createNotificationChannel(applicationContext) // 建立前台服務通知頻道
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        1,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(
                        1,
                        notification
                    )
                }
                _isServiceRunning.value = true

                startRecording(intent) // 開始錄影
            }
            // 收到停止錄影指令
            STOP_RECORDING -> {
                stopRecording() // 停止錄影
            }
        }
        return START_STICKY
    }

    private fun startRecording(intent: Intent) {
        val config = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                KEY_RECORDING_CONFIG,
                ScreenRecordConfig::class.java
            )
        } else {
            intent.getParcelableExtra(KEY_RECORDING_CONFIG)
        }
        if(config == null) {
            return
        }

        mediaProjection = mediaProjectionManager?.getMediaProjection(
            config.resultCode,
            config.data
        )
        mediaProjection?.registerCallback(mediaProjectionCallback, null)

        initializeRecorder()
        mediaRecorder.start()

        virtualDisplay = createVirtualDisplay()
    }

    private fun stopRecording() {
        mediaRecorder.stop()
        mediaProjection?.stop()
        mediaRecorder.reset()
    }

    private fun stopService() {
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun initializeRecorder() {
        val filenameFormat = "yyyy-MM-dd-HH-mm-ss" // 檔名格式
        val videoName = SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".mp4" // 建立檔名

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoName) // 檔名
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4") // 存黨格式
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/${getString(R.string.app_name)}") // 儲存相對路徑
        }

        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        // 建立儲存路徑 Uri
        val videoUri = contentResolver.insert(videoCollection, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        val pfd = contentResolver.openFileDescriptor(videoUri, "rw")
            ?: throw FileNotFoundException("Failed to open file descriptor for URI: $videoUri")

        with(mediaRecorder) {
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(pfd.fileDescriptor)
            setVideoSize(886, 1920)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
            setVideoEncodingBitRate(3840 * 2160)
            setVideoFrameRate(30)
            prepare()
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
            "Screen",
            886,
            1920,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface,
            null,
            null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        serviceScope.coroutineContext.cancelChildren()
    }

    private fun releaseResources() {
        mediaRecorder.release()
        virtualDisplay?.release()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        const val START_RECORDING = "START_RECORDING"
        const val STOP_RECORDING = "STOP_RECORDING"
        const val KEY_RECORDING_CONFIG = "KEY_RECORDING_CONFIG"
    }
}