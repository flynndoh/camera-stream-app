package com.example.cam_streamer.network

import fi.iki.elonen.NanoHTTPD

class StreamServer : NanoHTTPD(8080) {
    private var mjpegStreamInputStream: MJPEGStreamInputStream? = null
    private val boundary = "mjpegstream"
    private val activeSessions: MutableList<String> = mutableListOf()

    override fun serve(session: IHTTPSession): Response {
        activeSessions.add(session.remoteIpAddress)
        return when (session.uri) {
            "/stream" -> serveMjpegStream()
            "/" -> serveTestPage()
            else -> serveNotFound()
        }
    }

    private fun serveMjpegStream(): Response {
        // Create new stream with specified boundary
        mjpegStreamInputStream = MJPEGStreamInputStream(boundary)

        // Create response with proper MIME type
        val mimeType = "multipart/x-mixed-replace;boundary=$boundary"
        val response = newChunkedResponse(Response.Status.OK, mimeType, mjpegStreamInputStream)

        // Add required headers
        response.addHeader("Connection", "keep-alive")
        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
        response.addHeader("Pragma", "no-cache")
        response.addHeader("Expires", "0")

        return response
    }

    private fun serveTestPage(): Response {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>MJPEG Stream Test</title>
                <style>
                    body { 
                        display: flex; 
                        justify-content: center; 
                        align-items: center; 
                        min-height: 100vh; 
                        margin: 0; 
                        background: #000;
                    }
                    img { 
                        max-width: 100%;
                        max-height: 100vh;
                    }
                </style>
            </head>
            <body>
                <img src="/stream" />
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(html)
    }

    private fun serveNotFound(): Response {
        val message = "Stream not found. Access /stream to view the camera feed or / for the test page."
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            MIME_PLAINTEXT,
            message
        )
    }

    fun sendFrame(frameData: ByteArray) {
        mjpegStreamInputStream?.setFrame(frameData)
    }
}