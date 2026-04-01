package com.jewer.bodycam.backend.objectdetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ObjectDetectorHelper(
    val context: Context,
    var objectDetectorListener: DetectorListener? = null
) {
    private val runningMode = RunningMode.LIVE_STREAM
    private var objectDetector: ObjectDetector? = null
    
    @Volatile
    private var isClosed = false

    init {
        setupObjectDetector()
    }

    /**
     * 關鍵優化：使用同步鎖確保在關閉引擎時，沒有任何影像幀正在處理中。
     * 解決 nativeCreateRgbaImage 崩潰問題。
     */
    fun clearObjectDetector() {
        synchronized(this) {
            isClosed = true
            try {
                objectDetector?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing detector: ${e.message}")
            }
            objectDetector = null
        }
    }

    fun setupObjectDetector() {
        synchronized(this) {
            isClosed = false
            val baseOptionsBuilder = BaseOptions.builder()
            baseOptionsBuilder.setDelegate(Delegate.CPU)
            val modelName = LIFESTUFF_MOBILENET_V1
            baseOptionsBuilder.setModelAssetPath(modelName)

            if (objectDetectorListener == null) {
                throw IllegalStateException(
                    "objectDetectorListener must be set when runningMode is LIVE_STREAM."
                )
            }

            try {
                val optionsBuilder =
                    ObjectDetector.ObjectDetectorOptions.builder()
                        .setBaseOptions(baseOptionsBuilder.build())
                        .setScoreThreshold(THRESHOLD_DEFAULT)
                        .setRunningMode(runningMode)
                        .setMaxResults(MAX_RESULTS_DEFAULT)
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError)

                val options = optionsBuilder.build()
                objectDetector = ObjectDetector.createFromOptions(context, options)
            } catch (e: Exception) {
                objectDetectorListener?.onError(
                    "Object detector failed to initialize: ${e.message}"
                )
                Log.e(TAG, "MediaPipe failed to load model: ${e.message}")
            }
        }
    }

    fun detectLivestreamFrame(imageProxy: ImageProxy) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            imageProxy.close()
            return
        }
        
        // 1. 立即檢查引擎狀態
        if (isClosed) {
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        try {
            // 2. 獲取同步鎖，確保處理期間引擎不會被 close()
            synchronized(this) {
                val detector = objectDetector
                if (detector == null || isClosed) {
                    imageProxy.close()
                    return
                }

                // 3. 執行影像轉換
                val bitmapBuffer = createBitmap(imageProxy.width, imageProxy.height)
                imageProxy.use { 
                    bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) 
                }

                val matrix = Matrix().apply { 
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) 
                }

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
                )

                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                
                // 4. 送入非同步辨識
                detector.detectAsync(mpImage, frameTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Detection error: ${e.message}")
            imageProxy.close()
        }
    }

    private fun returnLivestreamResult(result: ObjectDetectorResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        objectDetectorListener?.onResults(
            ResultBundle(listOf(result), inferenceTime, input.height, input.width)
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        objectDetectorListener?.onError(error.message ?: "An unknown error has occurred")
    }

    data class ResultBundle(
        val results: List<ObjectDetectorResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    companion object {
        const val LIFESTUFF_MOBILENET_V1 = "lifestuff_mobilenet_v1.tflite"
        const val MAX_RESULTS_DEFAULT = 3
        const val THRESHOLD_DEFAULT = 0.5F
        const val TAG = "ObjectDetectorHelper"
    }

    interface DetectorListener {
        fun onError(error: String, errorCode: Int = 0)
        fun onResults(resultBundle: ResultBundle)
    }
}
