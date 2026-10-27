package com.example.lanchat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.lanchat.network.LanCallManager;
import com.example.lanchat.network.LanManager;

import java.util.List;
import java.util.Locale;

public class CallActivity extends AppCompatActivity implements LanManager.Listener, LanCallManager.VideoFrameListener, LanCallManager.CallStateListener {

    private static final int REQ_PERMISSIONS = 201;

    private String peerIp;
    private String peerName;
    private String callType; // "audio" or "video"
    private boolean isIncoming;

    private ImageView ivRemoteVideo;
    private View videoOverlay, cardLocalPreview;
    private TextureView tvLocalPreview;
    private TextView tvAvatar, tvPeerName, tvStatus;
    private View layoutIncomingControls, layoutActiveControls;
    private ImageButton btnToggleMic, btnSpeaker, btnSwitchCam;

    private LanManager lanManager;
    private LanCallManager callManager;

    private boolean isCallConnected = false;
    private int callSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private Ringtone incomingRingtone;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        peerIp = getIntent().getStringExtra("peer_ip");
        peerName = getIntent().getStringExtra("peer_name");
        callType = getIntent().getStringExtra("call_type");
        if (callType == null) callType = "audio";
        isIncoming = getIntent().getBooleanExtra("is_incoming", false);

        if (peerIp == null) {
            Toast.makeText(this, "Recipient invalid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        lanManager = LanManager.getInstance(this);
        callManager = LanCallManager.getInstance();

        initViews();
        checkAndRequestPermissions();
    }

    private void initViews() {
        ivRemoteVideo = findViewById(R.id.iv_remote_video);
        videoOverlay = findViewById(R.id.video_overlay);
        cardLocalPreview = findViewById(R.id.card_local_preview);
        tvLocalPreview = findViewById(R.id.tv_local_preview);

        tvAvatar = findViewById(R.id.tv_call_avatar);
        tvPeerName = findViewById(R.id.tv_call_peer_name);
        tvStatus = findViewById(R.id.tv_call_status);

        layoutIncomingControls = findViewById(R.id.layout_incoming_controls);
        layoutActiveControls = findViewById(R.id.layout_active_controls);

        btnToggleMic = findViewById(R.id.btn_toggle_mic);
        btnSpeaker = findViewById(R.id.btn_speaker);
        btnSwitchCam = findViewById(R.id.btn_switch_camera);

        if (peerName != null && !peerName.isEmpty()) {
            tvPeerName.setText(peerName);
            tvAvatar.setText(peerName.substring(0, 1).toUpperCase());
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xFF6366F1);
            tvAvatar.setBackground(bg);
        }

        if ("video".equalsIgnoreCase(callType)) {
            cardLocalPreview.setVisibility(View.VISIBLE);
            btnSwitchCam.setVisibility(View.VISIBLE);
        } else {
            cardLocalPreview.setVisibility(View.GONE);
            btnSwitchCam.setVisibility(View.GONE);
        }

        findViewById(R.id.fab_accept).setOnClickListener(v -> acceptCall());
        findViewById(R.id.fab_decline).setOnClickListener(v -> declineCall());
        findViewById(R.id.fab_hangup).setOnClickListener(v -> endCall());

        btnToggleMic.setOnClickListener(v -> {
            callManager.toggleMute();
            boolean muted = callManager.isMuted();
            btnToggleMic.setSelected(muted);
            btnToggleMic.setBackgroundTintList(ColorStateList.valueOf(muted ? 0xFFEF4444 : 0xFF1E293B));
        });

        btnSpeaker.setOnClickListener(v -> {
            callManager.toggleSpeaker(this);
            boolean speakerOn = callManager.isSpeakerOn();
            btnSpeaker.setSelected(speakerOn);
            btnSpeaker.setBackgroundTintList(ColorStateList.valueOf(speakerOn ? 0xFF22C55E : 0xFF1E293B));
        });

        btnSwitchCam.setOnClickListener(v -> callManager.switchCamera(tvLocalPreview));
    }

    private void checkAndRequestPermissions() {
        String[] permissions = ("video".equalsIgnoreCase(callType)) ?
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA} :
                new String[]{Manifest.permission.RECORD_AUDIO};

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            setupCallFlow();
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                setupCallFlow();
            } else {
                Toast.makeText(this, "Permissions required for call", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void setupCallFlow() {
        lanManager.registerListener(this);
        callManager.setVideoFrameListener(this);
        callManager.setCallStateListener(this);

        if (isIncoming) {
            tvStatus.setText("Incoming " + callType + " call...");
            layoutIncomingControls.setVisibility(View.VISIBLE);
            layoutActiveControls.setVisibility(View.GONE);
            startRinging();
        } else {
            tvStatus.setText("Calling " + (peerName != null ? peerName : "") + "...");
            layoutIncomingControls.setVisibility(View.GONE);
            layoutActiveControls.setVisibility(View.VISIBLE);
            lanManager.sendCallSignal(peerIp, LanManager.TYPE_CALL_OFFER, callType);
        }
    }

    private void startRinging() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            incomingRingtone = RingtoneManager.getRingtone(this, notification);
            if (incomingRingtone != null) {
                incomingRingtone.play();
            }
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) {}
    }

    private void stopRinging() {
        try {
            if (incomingRingtone != null && incomingRingtone.isPlaying()) {
                incomingRingtone.stop();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception ignored) {}
    }

    private void acceptCall() {
        stopRinging();
        layoutIncomingControls.setVisibility(View.GONE);
        layoutActiveControls.setVisibility(View.VISIBLE);

        lanManager.sendCallSignal(peerIp, LanManager.TYPE_CALL_ACCEPT, callType);
        connectCall();
    }

    private void declineCall() {
        stopRinging();
        lanManager.sendCallSignal(peerIp, LanManager.TYPE_CALL_DECLINE, callType);
        finish();
    }

    private void connectCall() {
        stopRinging();
        isCallConnected = true;
        tvStatus.setText("Connecting...");
        if ("video".equalsIgnoreCase(callType)) {
            ivRemoteVideo.setVisibility(View.VISIBLE);
            videoOverlay.setVisibility(View.VISIBLE);
        }

        boolean isCaller = !isIncoming;
        callManager.startCall(this, peerIp, "video".equalsIgnoreCase(callType), isCaller, tvLocalPreview);
        startTimer();
    }

    private void startTimer() {
        callSeconds = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCallConnected) {
                    callSeconds++;
                    int mins = callSeconds / 60;
                    int secs = callSeconds % 60;
                    String currentStatus = tvStatus.getText().toString();
                    if (currentStatus.startsWith("Connected") || currentStatus.startsWith("Connecting")) {
                        tvStatus.setText(String.format(Locale.getDefault(), "Connected • %02d:%02d", mins, secs));
                    }
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void endCall() {
        stopRinging();
        if (peerIp != null) {
            lanManager.sendCallSignal(peerIp, LanManager.TYPE_CALL_END, callType);
        }
        cleanupAndFinish();
    }

    private void cleanupAndFinish() {
        stopRinging();
        isCallConnected = false;
        timerHandler.removeCallbacks(timerRunnable);
        callManager.stopCall(this);
        lanManager.unregisterListener(this);
        finish();
    }

    @Override
    public void onRemoteFrameReceived(Bitmap bitmap) {
        if (bitmap != null && "video".equalsIgnoreCase(callType)) {
            ivRemoteVideo.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onConnectionStatusChanged(String status) {
        if (tvStatus != null) {
            tvStatus.setText(status);
        }
    }

    @Override
    public void onPeersUpdated(List<com.example.lanchat.model.Peer> peerList) {}

    @Override
    public void onMessageReceived(String senderIp) {}

    @Override
    public void onCallAcceptReceived(String senderIp) {
        if (peerIp != null && peerIp.equals(senderIp) && !isCallConnected) {
            connectCall();
        }
    }

    @Override
    public void onCallDeclineReceived(String senderIp) {
        if (peerIp != null && peerIp.equals(senderIp)) {
            Toast.makeText(this, "Call declined", Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
        }
    }

    @Override
    public void onCallEndReceived(String senderIp) {
        if (peerIp != null && peerIp.equals(senderIp)) {
            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupAndFinish();
    }
}
