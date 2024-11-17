package com.example.cam_streamer.network

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class MJPEGStreamInputStream(private val boundary: String) : InputStream() {
    private val frameQueue = ArrayBlockingQueue<ByteArray>(2) // Buffer size of 2 frames
    private var currentFrameStream: ByteArrayInputStream? = null
    private var isFirstFrame = true

    @Synchronized
    fun setFrame(frameData: ByteArray) {
        // Clear old frame if queue is full
        if (!frameQueue.offer(frameData)) {
            frameQueue.poll() // Remove oldest frame
            frameQueue.offer(frameData) // Add new frame
        }
    }

    private fun createFrameData(imageData: ByteArray): ByteArray {
        val headerBuilder = StringBuilder()

        if (isFirstFrame) {
            headerBuilder.append("--")
            isFirstFrame = false
        } else {
            headerBuilder.append("\r\n--")
        }

        headerBuilder.append("""
            $boundary
            Content-Type: image/jpeg
            Content-Length: ${imageData.size}

            """.trimIndent())
        headerBuilder.append("\r\n")

        val header = headerBuilder.toString().toByteArray(Charsets.US_ASCII)
        val frameData = ByteArray(header.size + imageData.size)
        System.arraycopy(header, 0, frameData, 0, header.size)
        System.arraycopy(imageData, 0, frameData, header.size, imageData.size)

        return frameData
    }

    override fun read(): Int {
        // If we're in the middle of streaming a frame, continue with it
        currentFrameStream?.let { stream ->
            val nextByte = stream.read()
            if (nextByte != -1) return nextByte
            currentFrameStream = null
        }

        // Get next frame with a timeout
        val nextFrame = frameQueue.poll(100, TimeUnit.MILLISECONDS)
        if (nextFrame != null) {
            val frameData = createFrameData(nextFrame)
            currentFrameStream = ByteArrayInputStream(frameData)
            return read()
        }

        return 0 // Keep stream alive
    }

    override fun available(): Int {
        return currentFrameStream?.available() ?: 0
    }
}