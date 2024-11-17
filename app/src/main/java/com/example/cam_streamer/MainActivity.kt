package com.example.cam_streamer
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cam_streamer.camera.CameraHelper
import com.example.cam_streamer.network.NetworkHelper
import com.example.cam_streamer.network.StreamServer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), CameraXConfig.Provider {

    private lateinit var previewView: PreviewView
    private lateinit var startStopButton: Button
    private lateinit var flipCameraButton: Button
    private lateinit var streamUrlTextView: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var streamServer: StreamServer? = null
    private var cameraHelper: CameraHelper? = null
    private var isStreaming = false
    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility") // No need to propagate touch events for cam preview
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        previewView = findViewById(R.id.previewView)
        startStopButton = findViewById(R.id.startStopButton)
        flipCameraButton = findViewById(R.id.flipCameraButton)
        streamUrlTextView = findViewById(R.id.streamUrlTextView)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up CameraX
        setupCamera()

        // Set up start/stop streaming button
        startStopButton.setOnClickListener {
            toggleStreaming()
        }

        // Set up flip camera button
        flipCameraButton.setOnClickListener {
            cameraHelper?.flipCamera()
        }

        // Set up GestureDetector for double-tap detection
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                cameraHelper?.flipCamera()
                return true
            }
        })

        // Hook GestureDetector up to the camera preview
        previewView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) // Pass touch events to the gesture detector
            true // Return true to indicate the touch event is handled
        }
    }

    private fun setupCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        cameraHelper = CameraHelper(this, previewView, cameraExecutor) { frameData ->
            // Send the captured frame to the stream server
            streamServer?.sendFrame(frameData)
        }
        cameraHelper!!.startCamera(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    private fun toggleStreaming() {
        if (isStreaming) {
            stopStreaming()
        } else {
            startStreaming()
        }
    }

    private fun startStreaming() {
        val port = 8080
        val localIpAddress = NetworkHelper.getLocalIpAddress(this)
        var streamUrl: String? = null
        if (localIpAddress != null) {
            streamUrl = "http://$localIpAddress:$port/stream"
            "Stream URL: $streamUrl".also { streamUrlTextView.text = it }
        } else {
            "Unable to determine IP address. Ensure you are connected to Wi-Fi.".also { streamUrlTextView.text = it }
        }

        if (streamUrl == null) {
            return
        }

        // Start the stream server
        streamServer = StreamServer().apply {
            start()
        }

        // Update UI
        isStreaming = true
        "Stop Streaming".also { startStopButton.text = it }
        "Stream URL: $streamUrl".also { streamUrlTextView.text = it }
    }

    private fun stopStreaming() {
        streamServer?.stop()
        streamServer = null

        // Update UI
        isStreaming = false
        "Start Streaming".also { startStopButton.text = it }
        "Stream stopped.".also { streamUrlTextView.text = it }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopStreaming()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}
