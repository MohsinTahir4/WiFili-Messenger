package com.example.lanchat;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.lanchat.adapter.FileAdapter;
import com.example.lanchat.utils.InsetsUtils;

import java.io.File;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar pbLoading;
    private TextView tvTitle;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        String filePath = getIntent().getStringExtra("file_path");
        String fileName = getIntent().getStringExtra("file_name");

        if (filePath == null) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        videoFile = new File(filePath);
        if (!videoFile.exists()) {
            Toast.makeText(this, "Video file does not exist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        InsetsUtils.applyTopAndBottomInsets(
                findViewById(R.id.root_video_player),
                findViewById(R.id.header_video_player),
                null
        );

        videoView = findViewById(R.id.video_view);
        pbLoading = findViewById(R.id.pb_video_loading);
        tvTitle = findViewById(R.id.tv_video_title);

        if (fileName != null) {
            tvTitle.setText(fileName);
        } else {
            tvTitle.setText(videoFile.getName());
        }

        findViewById(R.id.btn_back_video).setOnClickListener(v -> finish());
        findViewById(R.id.btn_open_external).setOnClickListener(v -> FileAdapter.openFile(this, videoFile, "video"));

        setupPlayer();
    }

    private void setupPlayer() {
        try {
            Uri videoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", videoFile);
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);

            videoView.setMediaController(mediaController);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                pbLoading.setVisibility(View.GONE);
                mp.setLooping(true);
                videoView.start();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(this, "In-app playback failed. Opening in external player...", Toast.LENGTH_SHORT).show();
                FileAdapter.openFile(this, videoFile, "video");
                finish();
                return true;
            });
        } catch (Exception e) {
            pbLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Cannot play video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            FileAdapter.openFile(this, videoFile, "video");
            finish();
        }
    }
}
