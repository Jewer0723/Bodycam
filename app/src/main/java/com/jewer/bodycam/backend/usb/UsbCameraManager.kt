package com.jewer.bodycam.backend.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.view.Surface
import androidx.core.content.getSystemService
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.usb.UVCCamera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object UsbCameraManager {

    // ── 對外狀態旗標 ────────────────────────────────────────────
    private val _isUsbCameraConnected = MutableStateFlow(false)
    val isUsbCameraConnected = _isUsbCameraConnected.asStateFlow()

    /** null = 使用手機內建 CAMCORDER；非 null = 使用 USB 麥克風 MIC */
    private val _usbAudioSource = MutableStateFlow<Int?>(null)
    val usbAudioSource = _usbAudioSource.asStateFlow()

    // ── 內部物件 ───────────────────────────────────────────────
    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null
    private var previewSurface: Surface? = null

    // ── 初始化（在 CameraScreen DisposableEffect 中呼叫）────────
    fun register(
        context: Context,
        onAttached: () -> Unit,   // USB 插入回呼
        onDetached: () -> Unit    // USB 拔出回呼
    ) {
        usbMonitor = USBMonitor(context, object : OnDeviceConnectListener {

            // 偵測到 USB 裝置
            override fun onAttach(device: UsbDevice?) {
                _isUsbCameraConnected.value = true
                detectUsbMic(context)
                onAttached()
            }

            // USB 裝置拔出
            override fun onDettach(device: UsbDevice?) {
                _isUsbCameraConnected.value = false
                _usbAudioSource.value = null
                releaseCamera()
                onDetached()
            }

            // 使用者授權 USB 權限後觸發
            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: UsbControlBlock?,
                createNew: Boolean
            ) {
                releaseCamera()
                uvcCamera = UVCCamera()
                uvcCamera?.open(ctrlBlock)
                try {
                    uvcCamera?.setPreviewSize(
                        UVCCamera.DEFAULT_PREVIEW_WIDTH,
                        UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                        UVCCamera.FRAME_FORMAT_MJPEG
                    )
                } catch (_: Exception) {
                    // 部分攝像頭不支援 MJPEG，改用 YUV
                    uvcCamera?.setPreviewSize(
                        UVCCamera.DEFAULT_PREVIEW_WIDTH,
                        UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                        UVCCamera.FRAME_FORMAT_YUYV
                    )
                }
                previewSurface?.let { uvcCamera?.setPreviewDisplay(it) }
                uvcCamera?.startPreview()
            }

            // USB 連線中斷
            override fun onDisconnect(device: UsbDevice?, ctrlBlock: UsbControlBlock?) {
                releaseCamera()
            }

            override fun onCancel(device: UsbDevice?) {}
        })
        usbMonitor?.register()

        // App 啟動時若 USB 攝像頭已插入，主動掃描一次
        checkAlreadyConnected(context)
    }

    fun unregister() {
        releaseCamera()
        usbMonitor?.unregister()
        usbMonitor?.destroy()
        usbMonitor = null
    }

    /** 設定 UVC 預覽輸出的 Surface（來自 CameraScreen 的 SurfaceView） */
    fun setPreviewSurface(surface: Surface?) {
        previewSurface = surface
        uvcCamera?.setPreviewDisplay(surface)
    }

    /** 請求 USB 裝置連線權限（使用者會看到授權對話框） */
    fun requestPermission(device: UsbDevice?) {
        device ?: return
        usbMonitor?.requestPermission(device)
    }

    /** 取得目前偵測到的第一個 USB 裝置 */
    fun getUsbDevice(): UsbDevice? {
        return usbMonitor?.deviceList?.firstOrNull()
    }

    // ── 私有工具方法 ──────────────────────────────────────────

    private fun releaseCamera() {
        uvcCamera?.stopPreview()
        uvcCamera?.close()
        uvcCamera?.destroy()
        uvcCamera = null
    }

    /** 偵測已連接的 USB 麥克風 */
    private fun detectUsbMic(context: Context) {
        val audioManager = context.getSystemService<AudioManager>() ?: return
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val hasUsbMic = inputs.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
        _usbAudioSource.value = if (hasUsbMic) MediaRecorder.AudioSource.MIC else null
    }

    /** App 啟動時若攝像頭已插入則主動觸發 */
    private fun checkAlreadyConnected(context: Context) {
        val deviceList = usbMonitor?.deviceList
        if (!deviceList.isNullOrEmpty()) {
            _isUsbCameraConnected.value = true
            detectUsbMic(context)
        }
    }
}