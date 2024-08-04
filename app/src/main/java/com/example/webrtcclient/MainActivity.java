package com.example.webrtcclient;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;
    private Button startCallButton;
    private Button hangUpButton;
    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localView = findViewById(R.id.local_view);
        remoteView = findViewById(R.id.remote_view);
        startCallButton = findViewById(R.id.start_call_button);
        hangUpButton = findViewById(R.id.hang_up_button);

        if (checkPermissions()) {
            initializeWebRTC();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initializeWebRTC();
            } else {
                Toast.makeText(this, "权限被拒绝，无法使用WebRTC功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeWebRTC() {
        EglBase.Context eglBaseContext = EglBase.create().getEglBaseContext();
        localView.init(eglBaseContext, null);
        remoteView.init(eglBaseContext, null);

        client = new Client(this, "http://192.168.68.58:3000", eglBaseContext, localView, remoteView);

        startCallButton.setOnClickListener(v -> client.sendReady());
        hangUpButton.setOnClickListener(v -> client.hangUp());
    }

    @Override
    protected void onDestroy() {
        if (localView != null) {
            localView.release();
        }
        if (remoteView != null) {
            remoteView.release();
        }
        super.onDestroy();
    }
}