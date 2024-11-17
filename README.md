# Camera Streamer App
A simple Android application built with Kotlin and Android Studio to stream the phone's camera feed over the local network as an MJPEG stream. The app uses CameraX for camera integration and NanoHTTPD for serving the stream.

## Features
- Live camera preview on the device.
- Streams the camera feed over HTTP (MJPEG format).
- Lightweight and runs efficiently on most Android devices.

## How It Works
1. The app captures live camera frames using CameraX.
2. Frames are converted to JPEG format and served over the local network using NanoHTTPD.
3. The app dynamically determines the local IP address to provide the stream URL.

## Prerequisites
- Android Studio: Version 4.0 or newer.
- A physical Android device (emulators typically do not support camera streaming).
- Both the Android device and the client device (browser or media player) must be connected to the same Wi-Fi network.

## How to Build and Run
1. Clone or download the project and open it in Android Studio.
2. Ensure you have a physical Android device connected.
3. Build and run the app on the device.
4. Once the app starts:
   - The camera preview will display on the device.
   - The stream URL will appear at the bottom of the screen.
5. Open the URL in a browser or a media player (e.g., VLC) on a device connected to the same network.

## How to Access the Stream
1. After starting the app, note the stream URL displayed at the bottom (e.g., `http://192.168.1.10:8080/stream`).
2. Open the URL in:
   - A web browser for real-time viewing.
   - A media player like VLC for better playback experience.
3. To stop streaming, tap the Stop Streaming button.

## Dependencies
- AndroidX CameraX: For capturing the live camera feed.
- NanoHTTPD: A lightweight web server for streaming.

## Known Limitations
- Works only on devices connected to Wi-Fi or Ethernet. 
- Limited to MJPEG format, which may not be efficient for high-resolution or long-term streaming.

## Future Enhancements
- Add authentication for secure streaming.
- Support additional streaming formats like H.264.
- Include customizable resolution and bitrate options.
