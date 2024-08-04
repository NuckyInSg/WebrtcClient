package com.example.webrtcclient;

import android.content.Context;
import android.util.Log;

import com.example.webrtcclient.signaling.SignalingClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import java.util.ArrayList;
import java.util.List;

public class Client implements PeerConnection.Observer, SdpObserver, SignalingClient.SignalingClientListener {

    private static final String TAG = "Client";
    private SignalingClient signalingClient;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private MediaStream localStream;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private List<PeerConnection.IceServer> iceServers;
    private EglBase.Context eglBaseContext;
    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    private VideoCapturer videoCapturer;
    private Context context;

    public Client(Context context, String serverUrl, EglBase.Context eglBaseContext, SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        this.context = context;
        this.eglBaseContext = eglBaseContext;
        this.localRenderer = localRenderer;
        this.remoteRenderer = remoteRenderer;

        signalingClient = new SignalingClient(serverUrl, this);
        signalingClient.connect();

        initializePeerConnectionFactory();
        createPeerConnection();
        startLocalStream();
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableNetworkMonitor = true;

        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                eglBaseContext, true /* enableIntelVp8Encoder */, true /* enableH264HighProfile */);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBaseContext);

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    private void createPeerConnection() {
        iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = factory.createPeerConnection(rtcConfig, this);
    }

    private void startLocalStream() {
        MediaConstraints constraints = new MediaConstraints();

        videoCapturer = createCameraCapturer(new Camera2Enumerator(context));

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());

        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        localVideoTrack.addSink(localRenderer);

        AudioSource audioSource = factory.createAudioSource(constraints);
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);

        localStream = factory.createLocalMediaStream("ARDAMS");
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);

        peerConnection.addStream(localStream);
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public void sendReady() {
        signalingClient.sendReady();
    }

    public void hangUp() {
        if (peerConnection != null) {
            peerConnection.close();
        }
        if (localStream != null) {
            localStream.dispose();
        }
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
        }
        if (localRenderer != null) {
            localRenderer.release();
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
        }
        signalingClient.disconnect();
    }

    // PeerConnection.Observer Implementation

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        signalingClient.sendIceCandidate(iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
        remoteVideoTrack.addSink(remoteRenderer);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

    // Other PeerConnection.Observer methods...

    // SdpObserver Implementation

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
        if (sessionDescription.type == SessionDescription.Type.OFFER) {
            signalingClient.sendOffer(sessionDescription);
        } else if (sessionDescription.type == SessionDescription.Type.ANSWER) {
            signalingClient.sendAnswer(sessionDescription);
        }
    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String s) {

    }

    @Override
    public void onSetFailure(String s) {

    }

    // Other SdpObserver methods...

    // SignalingClientListener Implementation

    @Override
    public void onSignalingConnected() {
        Log.d(TAG, "Signaling connected");
    }

    @Override
    public void onSignalingDisconnected() {
        Log.d(TAG, "Signaling disconnected");
    }

    @Override
    public void onSignalingError(Exception e) {
        Log.e(TAG, "Signaling error: " + e.getMessage());
    }

    @Override
    public void onReady() {
        peerConnection.createOffer(this, new MediaConstraints());
    }

    @Override
    public void onOffer(JSONObject offer) {
        try {
            Log.d(TAG, "Received offer");
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, offer.getString("sdp"));

            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "Remote description set successfully");
                    // Create answer after remote description is set
                    peerConnection.createAnswer(new SimpleSdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            Log.d(TAG, "Create answer success");
                            // Set local description
                            peerConnection.setLocalDescription(new SimpleSdpObserver() {
                                @Override
                                public void onSetSuccess() {
                                    Log.d(TAG, "Local description set successfully");
                                    // Send the answer to the signaling server
                                    signalingClient.sendAnswer(sessionDescription);
                                }
                            }, sessionDescription);
                        }
                    }, new MediaConstraints());
                }
            }, sdp);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing offer: " + e.getMessage());
        }
    }

    @Override
    public void onAnswer(JSONObject answer) {
        try {
            peerConnection.setRemoteDescription(new SimpleSdpObserver(),
                    new SessionDescription(SessionDescription.Type.ANSWER, answer.getString("sdp")));
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing answer: " + e.getMessage());
        }
    }

    @Override
    public void onIceCandidate(JSONObject candidate) {
        try {
            peerConnection.addIceCandidate(new IceCandidate(
                    candidate.getString("sdpMid"),
                    candidate.getInt("sdpMLineIndex"),
                    candidate.getString("candidate")
            ));
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ICE candidate: " + e.getMessage());
        }
    }

    private class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, "SDP created successfully");
        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "SDP set successfully");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "SDP creation failed: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "SDP setting failed: " + s);
        }
    }

    // Implement other necessary PeerConnection.Observer and SdpObserver methods
}