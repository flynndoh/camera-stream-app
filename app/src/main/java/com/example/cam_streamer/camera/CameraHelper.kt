package com.example.cam_streamer.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import android.annotation.SuppressLint

class CameraHelper(
    private val context: Context,
    private val previewView: PreviewView,
    private val executor: ExecutorService,
    private val onFrameAvailable: (ByteArray) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentCameraSelector: CameraSelector? = null
    private var lastFrameTimeMs: Long = 0
    private val targetFpsInterval = 1000L / 30  // Target 30 FPS

    fun startCamera(cameraSelector: CameraSelector) {
        currentCameraSelector = cameraSelector
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Configure camera preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
            preview.surfaceProvider = previewView.surfaceProvider

            // Configure image analysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetRotation(previewView.display.rotation)
                .setOutputImageRotationEnabled(true)
                .build()

            imageAnalysis.setAnalyzer(executor, createAnalyzer())

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                // Set camera parameters
                val camera = cameraProvider?.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                // Configure camera for high performance
                camera?.cameraControl?.enableTorch(false)
                camera?.cameraInfo?.exposureState?.let { exposureState ->
                    if (exposureState.isExposureCompensationSupported) {
                        camera.cameraControl.setExposureCompensationIndex(0)
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraHelper", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun createAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { img ->

            // Add detailed logging
//            Log.d("CameraHelper", """
//                Image info:
//                Format: ${img.format}
//                Size: ${img.width}x${img.height}
//                Planes: ${img.planes.size}
//                Plane 0 - stride: ${img.planes[0].rowStride}, pixelStride: ${img.planes[0].pixelStride}
//                Plane 1 - stride: ${img.planes[1].rowStride}, pixelStride: ${img.planes[1].pixelStride}
//                Plane 2 - stride: ${img.planes[2].rowStride}, pixelStride: ${img.planes[2].pixelStride}
//            """.trimIndent())

            // Control frame rate
            val currentTimeMs = System.currentTimeMillis()
            if (currentTimeMs - lastFrameTimeMs >= targetFpsInterval) {
                lastFrameTimeMs = currentTimeMs

                try {
                    // Convert and send frame
                    val jpegBytes = ImageConverter.imageToJpegByteArray(img.image)
                    if (jpegBytes != null) {
                        onFrameAvailable(jpegBytes)
                    }
                } catch (e: Exception) {
                    Log.e("CameraHelper", "Error converting image", e)
                }
            }

            img.close()
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    fun flipCamera() {
        stopCamera()
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera(currentCameraSelector!!)
    }
}