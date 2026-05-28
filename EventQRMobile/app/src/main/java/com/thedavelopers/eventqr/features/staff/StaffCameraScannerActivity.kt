package com.thedavelopers.eventqr.features.staff

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.thedavelopers.eventqr.R
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DEPRECATION")
open class StaffCameraScannerActivity : AppCompatActivity(), SurfaceHolder.Callback, android.hardware.Camera.PreviewCallback {
    private val tag = "StaffQrScanner"
    private lateinit var surfaceView: SurfaceView
    private lateinit var statusText: TextView
    private var camera: android.hardware.Camera? = null
    private val reader = MultiFormatReader()
    private val decoding = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val decodeHints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_camera_scanner)

        surfaceView = findViewById(R.id.surfaceCameraPreview)
        statusText = findViewById(R.id.txtCameraStatus)
        surfaceView.holder.addCallback(this)
        Log.d(tag, "analyzer initialized")

        findViewById<android.view.View>(R.id.btnCloseCamera).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (surfaceView.holder.surface.isValid) {
            startCameraPreview(surfaceView.holder)
        }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(tag, "surface created")
        startCameraPreview(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(tag, "surface changed width=$width height=$height")
        releaseCamera()
        startCameraPreview(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(tag, "surface destroyed")
        releaseCamera()
    }

    override fun onPreviewFrame(data: ByteArray?, camera: android.hardware.Camera?) {
        if (data == null || camera == null || !decoding.compareAndSet(false, true)) {
            return
        }
        val previewSize = camera.parameters.previewSize
        val width = previewSize.width
        val height = previewSize.height

        executor.execute {
            val decoded = decodeFrame(data, width, height)
            runOnUiThread {
                if (!decoded.isNullOrBlank()) {
                    Log.d(tag, "raw QR value detected: $decoded")
                    val payload = android.content.Intent().apply {
                        putExtra(StaffScreenExtras.EXTRA_QR_VALUE, decoded)
                    }
                    setResult(RESULT_OK, payload)
                    finish()
                }
                decoding.set(false)
            }
        }
    }

    private fun startCameraPreview(holder: SurfaceHolder) {
        if (camera != null) {
            return
        }
        statusText.text = "Point camera at attendee QR"
        runCatching {
            camera = android.hardware.Camera.open().apply {
                val parameters = parameters
                val focusModes = parameters.supportedFocusModes.orEmpty()
                when {
                    focusModes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ->
                        parameters.focusMode = android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    focusModes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO) ->
                        parameters.focusMode = android.hardware.Camera.Parameters.FOCUS_MODE_AUTO
                }
                parameters.previewFormat = android.graphics.ImageFormat.NV21
                this.parameters = parameters
                setPreviewDisplay(holder)
                setDisplayOrientation(90)
                setPreviewCallback(this@StaffCameraScannerActivity)
                startPreview()
                Log.d(
                    tag,
                    "camera bind/start success preview=${parameters.previewSize.width}x${parameters.previewSize.height} focusMode=${parameters.focusMode}"
                )
            }
        }.onFailure {
            statusText.text = "Unable to start camera"
            Log.w(tag, "camera bind/start failed: ${it.message}", it)
        }
    }

    private fun releaseCamera() {
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun decodeFrame(data: ByteArray, width: Int, height: Int): String? {
        val ySize = width * height
        if (data.size < ySize) {
            return null
        }
        val luma = data.copyOf(ySize)
        val rotated90 = rotateLuma90(luma, width, height)
        val rotated180 = rotateLuma90(rotated90, height, width)
        val rotated270 = rotateLuma90(rotated180, width, height)

        val candidates = listOf(
            Triple(luma, width, height),
            Triple(rotated90, height, width),
            Triple(rotated180, width, height),
            Triple(rotated270, height, width),
        )

        for ((buffer, w, h) in candidates) {
            val normal = decodeBinaryBitmap(buffer, w, h, invert = false)
            if (!normal.isNullOrBlank()) return normal
            val inverted = decodeBinaryBitmap(buffer, w, h, invert = true)
            if (!inverted.isNullOrBlank()) return inverted
        }
        return null
    }

    private fun decodeBinaryBitmap(data: ByteArray, width: Int, height: Int, invert: Boolean): String? {
        val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
        val bitmap = BinaryBitmap(HybridBinarizer(if (invert) source.invert() else source))
        return try {
            reader.setHints(decodeHints)
            reader.decodeWithState(bitmap).text
        } catch (_: NotFoundException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun rotateLuma90(input: ByteArray, width: Int, height: Int): ByteArray {
        val output = ByteArray(width * height)
        var index = 0
        for (x in 0 until width) {
            for (y in height - 1 downTo 0) {
                output[index++] = input[y * width + x]
            }
        }
        return output
    }
}
