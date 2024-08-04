# Android WebRTC Native Client

This project is an Android implementation of a WebRTC (Web Real-Time Communication) native client. It allows for peer-to-peer video calling functionality using WebRTC technology.

## Features

- Real-time video calling
- Signaling server communication
- Camera capture and rendering
- Peer connection management
- ICE candidate handling

## Prerequisites

- Android Studio
- Android SDK (minimum SDK version to be determined based on your project settings)
- WebRTC library for Android

## Project Structure

The project consists of three main Java classes:

1. `MainActivity.java`: The main activity that handles UI interactions and permissions.
2. `SignalingClient.java`: Manages the WebSocket connection with the signaling server.
3. `Client.java`: Implements the WebRTC functionality, including peer connection, media stream handling, and signaling.

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/NuckyInSg/WebrtcClient
   ```

2. Open the project in Android Studio.

3. Ensure you have the latest Android SDK tools and platform tools installed.

4. Update the `serverUrl` in `MainActivity.java` to point to your signaling server:
   ```java
   client = new Client(this, "http://your-server-url:port", eglBaseContext, localView, remoteView);
   ```

5. Build and run the project on an Android device or emulator.

## Usage

1. Launch the app on two different devices.

2. Grant camera and microphone permissions when prompted.

3. On one device, tap the "Start Call" button to initiate a call.

4. The other device should receive the call automatically.

5. To end the call, tap the "Hang Up" button.

## Permissions

The app requires the following permissions:

- `CAMERA`: For capturing video
- `RECORD_AUDIO`: For capturing audio

These permissions are requested at runtime in `MainActivity.java`.

## Customization

You can customize various aspects of the WebRTC connection:

- Modify ICE servers in `Client.java`
- Adjust video resolution and frame rate in `Client.java`
- Customize UI layout in `activity_main.xml`

## Integration with WebRTC JS Project

This Android WebRTC client is designed to work seamlessly with the [WebRTC JS project](https://github.com/NuckyInSg/WebrtcJs). Here's how to integrate and use them together:

### Setting up the WebRTC JS Project

1. Clone the WebRTC JS project:
   ```
   git clone https://github.com/NuckyInSg/WebrtcJs.git
   cd WebrtcJs
   ```

2. Install the required dependencies:
   ```
   npm install
   ```

3. Start the signaling server:
   ```
   node server.js
   ```
   By default, the server runs on port 3000.

### Configuring the Android Client

1. In `MainActivity.java`, update the `serverUrl` to point to your signaling server:
   ```java
   client = new Client(this, "http://your-server-ip:3000", eglBaseContext, localView, remoteView);
   ```
   Replace `your-server-ip` with the IP address of the machine running the signaling server. If testing on the same network, you can use the local IP address.

### Using Android and Web Clients Together

1. Ensure the signaling server from the WebRTC JS project is running.

2. Open the web client in a browser:
   - If testing locally: `http://localhost:3000`
   - If testing from another device: `http://your-server-ip:3000`

3. Launch the Android app on your device.

4. Initiate a call from either the web client or the Android app:
   - On the web: Click the "Start Call" button.
   - On Android: Tap the "Start Call" button.

5. The call should connect between the web browser and the Android device.

### Notes on Compatibility

- The WebRTC JS project uses Socket.IO for signaling, which is compatible with the Socket.IO client used in the Android project.
- Both projects use a STUN server (`stun:stun.l.google.com:19302`) for NAT traversal. Ensure this is consistent in both clients.

## Troubleshooting

If you encounter issues:

1. Ensure both devices are connected to the internet.
2. Check that the signaling server URL is correct and the server is running.
3. Verify that camera and microphone permissions are granted.
4. Check the Android Studio logcat for detailed error messages.

For cross-platform calls between Android and web:

1. Check the console logs in the web browser and logcat in Android Studio for any error messages.
2. Ensure both clients are connected to the same signaling server by verifying the IP and port.
3. If testing over the internet, make sure port 3000 is forwarded to the machine running the signaling server.
4. Verify that your network allows WebRTC traffic (some corporate networks may block it).

## Contributing

Contributions to improve the project or enhance cross-platform compatibility are welcome. Please follow these steps:

1. Fork the repository
2. Create a new branch
3. Make your changes
4. Submit a pull request

