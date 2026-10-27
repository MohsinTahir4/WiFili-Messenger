package com.example.lanchat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lanchat.adapter.FileAdapter;
import com.example.lanchat.db.DatabaseHelper;
import com.example.lanchat.model.SharedFile;
import com.example.lanchat.network.LanManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class SendMediaActivity extends AppCompatActivity {

    private static final int REQ_PICK_FILE = 101;
    private String peerIp;
    private String peerName;
    private LanManager lanManager;
    private DatabaseHelper db;
    private FileAdapter fileAdapter;
    private String selectedMediaType = "file";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_media);

        peerIp = getIntent().getStringExtra("peer_ip");
        peerName = getIntent().getStringExtra("peer_name");
        lanManager = LanManager.getInstance(this);
        db = new DatabaseHelper(this);

        if (peerIp == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("⚠️ Not Connected");
            builder.setMessage("You are not connected to any user.\n\nHow to Fix:\n1. Select an online user from your Chats list.\n2. Tap the '+' Scan button on the home screen to discover nearby users.");
            builder.setPositiveButton("OK", (dialog, which) -> finish());
            builder.show();
            return;
        }

        findViewById(R.id.btn_back_media).setOnClickListener(v -> finish());

        findViewById(R.id.option_photo).setOnClickListener(v -> {
            selectedMediaType = "photo";
            pickFile("image/*");
        });
        findViewById(R.id.option_video).setOnClickListener(v -> {
            selectedMediaType = "video";
            pickFile("video/*");
        });
        findViewById(R.id.option_file).setOnClickListener(v -> {
            selectedMediaType = "file";
            pickFile("*/*");
        });
        findViewById(R.id.option_voice).setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceRecordActivity.class);
            intent.putExtra("peer_ip", peerIp);
            intent.putExtra("peer_name", peerName);
            startActivity(intent);
            finish();
        });

        RecyclerView rvRecentFiles = findViewById(R.id.rv_recent_files);
        rvRecentFiles.setLayoutManager(new LinearLayoutManager(this));

        fileAdapter = new FileAdapter(this::sendExistingFile);
        rvRecentFiles.setAdapter(fileAdapter);

        loadRecentFiles();
    }

    private void loadRecentFiles() {
        List<SharedFile> list = db.getAllSharedFiles();
        fileAdapter.setFiles(list);
    }

    private void pickFile(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Media"), REQ_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null && requestCode == REQ_PICK_FILE) {
            Uri uri = data.getData();
            processAndSendUri(uri, selectedMediaType);
        }
    }

    private void processAndSendUri(Uri uri, String mediaType) {
        try {
            String fileName = getFileNameFromUri(uri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "media_" + System.currentTimeMillis();
            }
            File tempFile = new File(getCacheDir(), fileName);
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[8192];
            int len;
            while (in != null && (len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            if (in != null) in.close();
            out.close();

            sendFileToPeer(tempFile, mediaType);
        } catch (Exception e) {
            showFileSendHelpDialog("Failed to read media: " + e.getMessage());
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void sendExistingFile(SharedFile sharedFile) {
        if (sharedFile.getFile() != null && sharedFile.getFile().exists()) {
            sendFileToPeer(sharedFile.getFile(), sharedFile.getType());
        } else {
            Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFileToPeer(File file, String type) {
        if (peerIp == null) {
            showFileSendHelpDialog("Recipient invalid.");
            return;
        }
        lanManager.sendFile(peerIp, file, type, percent -> {}, () -> {},
                error -> showFileSendHelpDialog(error));
        finish();
    }

    private void showFileSendHelpDialog(String errorMsg) {
        String displayName = peerName != null ? peerName : (peerIp != null ? peerIp : "user");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ File Transfer Failed");
        builder.setMessage("You are not connected to " + displayName + ".\n\n" +
                errorMsg + "\n\n" +
                "How to Fix Easily:\n" +
                "1. Open WiFili: Ensure " + displayName + " has WiFili Messenger open on screen.\n" +
                "2. Same Wi-Fi / Hotspot: Connect both phones to same Wi-Fi or Hotspot.\n" +
                "3. Re-Scan: Tap '+' Scan button on main screen to update IP address.\n" +
                "4. Retry sending file.");
        builder.setPositiveButton("Re-Scan LAN", (dialog, which) -> {
            LanManager.getInstance(this).announce();
            Toast.makeText(this, "Scanning LAN devices...", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("OK", null);
        builder.show();
    }
}
