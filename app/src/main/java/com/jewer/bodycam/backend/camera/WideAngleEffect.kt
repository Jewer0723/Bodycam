package com.jewer.bodycam.backend.camera

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executor

class CustomCameraEffect(
    targets: Int,
    executor: Executor,
    processor: SurfaceProcessor,
    errorListener: Consumer<Throwable>
) : CameraEffect(targets, executor, processor, errorListener)

class WideAngleSurfaceProcessor(private val isPortrait: Boolean, private val isFrontCamera: Boolean) : SurfaceProcessor {
    private val glThread = HandlerThread("GLThread").apply { start() }
    private val handler = Handler(glThread.looper)
    
    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    
    private var program = 0
    private var texName = -1
    private var surfaceTexture: SurfaceTexture? = null
    private var outputResolution = Size(0, 0)
    
    // 根據方向與鏡頭動態計算頂點數據
    private val vertexData: FloatBuffer by lazy {
        val data = if (isPortrait) {
            if (isFrontCamera) {
                // 前鏡頭垂直：鏡像標準映射
                floatArrayOf(
                    -1.0f, -1.0f, 1.0f, 0.0f,
                     1.0f, -1.0f, 0.0f, 0.0f,
                    -1.0f,  1.0f, 1.0f, 1.0f,
                     1.0f,  1.0f, 0.0f, 1.0f
                )
            } else {
                // 後鏡頭垂直：標準映射 (不旋轉)
                floatArrayOf(
                    -1.0f, -1.0f, 0.0f, 0.0f,
                     1.0f, -1.0f, 1.0f, 0.0f,
                    -1.0f,  1.0f, 0.0f, 1.0f,
                     1.0f,  1.0f, 1.0f, 1.0f
                )
            }
        } else {
            // 水平模式：應用順時針 90 度補償 (已驗證正確)
            floatArrayOf(
                -1.0f, -1.0f, 0.0f, 1.0f,
                 1.0f, -1.0f, 0.0f, 0.0f,
                -1.0f,  1.0f, 1.0f, 1.0f,
                 1.0f,  1.0f, 1.0f, 0.0f
            )
        }
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(data).apply { position(0) }
    }

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        uniform mat4 uTexMatrix;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES sTexture;

        void main() {
            vec2 uv = vTexCoord;
            vec2 pos = (uv - 0.5) * 2.0;
            float k = 0.45; 
            float r2 = pos.x * pos.x + pos.y * pos.y;
            vec2 distortedPos = pos * (1.0 + k * r2);
            distortedPos *= 0.6;
            vec2 sampleUv = (distortedPos / 2.0) + 0.5;
            
            if (sampleUv.x < 0.0 || sampleUv.x > 1.0 || sampleUv.y < 0.0 || sampleUv.y > 1.0) {
                gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
            } else {
                gl_FragColor = texture2D(sTexture, sampleUv);
            }
        }
    """.trimIndent()

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        handler.post {
            initEGL()
            initGL()
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            texName = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texName)
            surfaceTexture = SurfaceTexture(texName)
            surfaceTexture?.setDefaultBufferSize(surfaceRequest.resolution.width, surfaceRequest.resolution.height)
            val surface = Surface(surfaceTexture)
            surfaceRequest.provideSurface(surface, { command -> handler.post(command) }) {
                surface.release()
                surfaceTexture?.release()
                releaseGL()
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        handler.post {
            outputResolution = surfaceOutput.size
            val windowSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceOutput.getSurface({ command -> handler.post(command) }) {
                // Handle release
            }, intArrayOf(EGL14.EGL_NONE), 0)

            surfaceTexture?.setOnFrameAvailableListener {
                handler.post {
                    if (windowSurface == EGL14.EGL_NO_SURFACE) return@post
                    EGL14.eglMakeCurrent(eglDisplay, windowSurface, windowSurface, eglContext)
                    surfaceTexture?.updateTexImage()
                    GLES20.glViewport(0, 0, outputResolution.width, outputResolution.height)
                    val transform = FloatArray(16)
                    surfaceTexture?.getTransformMatrix(transform)
                    render(transform)
                    EGL14.eglSwapBuffers(eglDisplay, windowSurface)
                }
            }
        }
    }

    private fun initEGL() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) return
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        eglConfig = configs[0]
        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        val pbufferAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        val pbufferSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, pbufferSurface, pbufferSurface, eglContext)
    }

    private fun initGL() {
        if (program != 0) return
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun render(texMatrix: FloatArray) {
        GLES20.glUseProgram(program)
        val matrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, texMatrix, 0)

        vertexData.position(0)
        val posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 16, vertexData)

        vertexData.position(2)
        val texHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 16, vertexData)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texName)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun releaseGL() {
        handler.post {
            if (program != 0) { GLES20.glDeleteProgram(program); program = 0 }
            if (texName != -1) { GLES20.glDeleteTextures(1, intArrayOf(texName), 0); texName = -1 }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
            glThread.quitSafely()
        }
    }
}
