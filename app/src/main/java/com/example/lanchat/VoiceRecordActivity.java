package com.example.lanchat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lanchat.network.LanManager;

import java.io.File;
import java.util.Locale;

public class VoiceRecordActivity extends AppCompatActivity {

    private static final int REQ_AUDIO_PERM = 201;

    private String peerIp;
    private TextView tvRecordTimer;
    private MediaRecorder recorder;
    private File audioFile;
    private boolean isRecording = false;
    private boolean isFileSent = false;

    private int seconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                seconds++;
                int mins = seconds / 60;
                int secs = seconds % 60;
                tvRecordTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_record);

        peerIp = getIntent().getStringExtra("peer_ip");
        tvRecordTimer = findViewById(R.id.tv_record_timer);

        findViewById(R.id.btn_cancel_voice).setOnClickListener(v -> cancelRecording());
        findViewById(R.id.btn_discard).setOnClickListener(v -> cancelRecording());
        findViewById(R.id.btn_send_voice).setOnClickListener(v -> stopAndSendVoice());

        checkAudioPermissionAndStart();
    }

    private void checkAudioPermissionAndStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERM);
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Permission denied for audio recording", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startRecording() {
        try {
            audioFile = new File(getFilesDir(), "voice_" + System.currentTimeMillis() + ".m4a");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            isRecording = true;
            seconds = 0;
            timerHandler.post(timerRunnable);
        } catch (Exception e) {
            recorder = null;
            Toast.makeText(this, "Cannot start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void stopAndSendVoice() {
        if (!isRecording) return;
        try {
            isRecording = false;
            timerHandler.removeCallbacks(timerRunnable);

            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }

            if (audioFile != null && audioFile.exists() && peerIp != null) {
                isFileSent = true;
                Toast.makeText(this, "Sending voice message...", Toast.LENGTH_SHORT).show();
                File sendFile = audioFile;
                audioFile = null;
                LanManager.getInstance(this).sendFile(peerIp, sendFile, "voice", percent -> {
                    // progress update
                }, () -> {
                    Toast.makeText(this, "Voice message sent!", Toast.LENGTH_SHORT).show();
                    finish();
                }, error -> {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to stop recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void cancelRecording() {
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
            if (!isFileSent && audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
        } catch (Exception ignored) {}
        finish();
    }

    @Override
    protected void onDestroy() {
        cancelRecording();
        super.onDestroy();
    }
}
