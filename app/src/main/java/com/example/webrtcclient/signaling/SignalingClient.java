package com.example.webrtcclient.signaling;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;

public class SignalingClient {
    private static final String TAG = "SignalingClient";
    private Socket socket;
    private SignalingClientListener listener;

    public SignalingClient(String serverUrl, SignalingClientListener listener) {
        this.listener = listener;
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            socket = IO.socket(serverUrl, opts);
            setupSocketEvents();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket connection error: " + e.getMessage());
            listener.onSignalingError(e);
        }
    }

    private void setupSocketEvents() {
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Socket connected");
            listener.onSignalingConnected();
        }).on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Socket disconnected");
            listener.onSignalingDisconnected();
        }).on(Socket.EVENT_CONNECT_ERROR, args -> {
            Exception e = (Exception) args[0];
            Log.e(TAG, "Socket connection error: " + e.getMessage());
            listener.onSignalingError(e);
        }).on("ready", args -> {
            Log.d(TAG, "Received ready event");
            listener.onReady();
        }).on("offer", args -> {
            Log.d(TAG, "Received offer: " + args[0]);
            try {
                JSONObject offer = (JSONObject) args[0];
                listener.onOffer(offer);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing offer: " + e.getMessage());
            }
        }).on("answer", args -> {
            Log.d(TAG, "Received answer: " + args[0]);
            try {
                JSONObject answer = (JSONObject) args[0];
                listener.onAnswer(answer);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing answer: " + e.getMessage());
            }
        }).on("ice-candidate", args -> {
            Log.d(TAG, "Received ICE candidate");
            try {
                JSONObject candidate = (JSONObject) args[0];
                listener.onIceCandidate(candidate);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing ICE candidate: " + e.getMessage());
            }
        });
    }

    public void connect() {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }

    public void sendReady() {
        socket.emit("ready");
    }

    public void sendOffer(SessionDescription offer) {
        try {
            JSONObject jsonOffer = new JSONObject();
            jsonOffer.put("type", offer.type.canonicalForm());
            jsonOffer.put("sdp", offer.description);
            socket.emit("offer", jsonOffer);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating offer JSON: " + e.getMessage());
        }
    }

    public void sendAnswer(SessionDescription answer) {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("type", answer.type.canonicalForm());
            jsonAnswer.put("sdp", answer.description);
            socket.emit("answer", jsonAnswer);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating answer JSON: " + e.getMessage());
        }
    }

    public void sendIceCandidate(IceCandidate candidate) {
        try {
            JSONObject jsonCandidate = new JSONObject();
            jsonCandidate.put("sdpMLineIndex", candidate.sdpMLineIndex);
            jsonCandidate.put("sdpMid", candidate.sdpMid);
            jsonCandidate.put("candidate", candidate.sdp);
            socket.emit("ice-candidate", jsonCandidate);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ICE candidate JSON: " + e.getMessage());
        }
    }

    public interface SignalingClientListener {
        void onSignalingConnected();
        void onSignalingDisconnected();
        void onSignalingError(Exception e);
        void onReady();
        void onOffer(JSONObject offer);
        void onAnswer(JSONObject answer);
        void onIceCandidate(JSONObject candidate);
    }
}