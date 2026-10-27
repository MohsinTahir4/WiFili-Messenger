package com.example.lanchat;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lanchat.adapter.ChatAdapter;
import com.example.lanchat.db.DatabaseHelper;
import com.example.lanchat.model.ChatMessage;
import com.example.lanchat.model.Peer;
import com.example.lanchat.network.LanManager;
import com.example.lanchat.utils.InsetsUtils;

import java.util.List;

public class ChatActivity extends AppCompatActivity implements LanManager.Listener {

    private String peerName;
    private String peerIp;

    private TextView tvAvatar, tvPeerName, tvPeerStatus;
    private RecyclerView rvMessages;
    private EditText etMessage;

    private ChatAdapter chatAdapter;
    private DatabaseHelper db;
    private LanManager lanManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        peerName = getIntent().getStringExtra("peer_name");
        peerIp = getIntent().getStringExtra("peer_ip");

        if (peerIp == null) {
            Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        InsetsUtils.applyTopAndBottomInsets(
                findViewById(R.id.root_chat),
                findViewById(R.id.chat_header),
                findViewById(R.id.input_bar_container)
        );

        db = new DatabaseHelper(this);
        lanManager = LanManager.getInstance(this);

        initViews();
        setupData();
    }

    private void initViews() {
        tvAvatar = findViewById(R.id.tv_avatar);
        tvPeerName = findViewById(R.id.tv_peer_name);
        tvPeerStatus = findViewById(R.id.tv_peer_status);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter();
        rvMessages.setAdapter(chatAdapter);

        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());

        findViewById(R.id.btn_attach).setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMediaActivity.class);
            intent.putExtra("peer_ip", peerIp);
            intent.putExtra("peer_name", peerName);
            startActivity(intent);
        });

        findViewById(R.id.btn_emoji).setOnClickListener(v -> showEmojiPicker());
        findViewById(R.id.btn_call_audio).setOnClickListener(v -> startCall("audio"));
        findViewById(R.id.btn_call_video).setOnClickListener(v -> startCall("video"));
    }

    private void showEmojiPicker() {
        String[] emojis = {
                "😀", "😂", "😍", "😎", "👍", "❤️",
                "🔥", "🎉", "🙏", "👋", "🥳", "💡",
                "💯", "📌", "🚀", "💬", "🙌", "😊",
                "🤝", "⭐", "🎯", "⚡", "✨", "🎵"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Emoji");

        GridView gridView = new GridView(this);
        gridView.setNumColumns(6);
        gridView.setPadding(32, 24, 32, 24);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, emojis) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextSize(24f);
                tv.setGravity(Gravity.CENTER);
                return tv;
            }
        };

        gridView.setAdapter(adapter);
        builder.setView(gridView);

        AlertDialog dialog = builder.create();
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String emoji = emojis[position];
            etMessage.append(emoji);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void startCall(String callType) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("peer_ip", peerIp);
        intent.putExtra("peer_name", peerName);
        intent.putExtra("call_type", callType);
        intent.putExtra("is_incoming", false);
        startActivity(intent);
    }

    private void setupData() {
        if (peerName != null && !peerName.isEmpty()) {
            tvPeerName.setText(peerName);
            String initial = peerName.substring(0, 1).toUpperCase();
            tvAvatar.setText(initial);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xFF6366F1);
            tvAvatar.setBackground(bg);
        }

        loadMessages();
    }

    private void loadMessages() {
        List<ChatMessage> list = db.getMessagesForPeer(peerIp);
        chatAdapter.setMessages(list);
        if (!list.isEmpty()) {
            rvMessages.scrollToPosition(list.size() - 1);
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        etMessage.setText("");

        lanManager.sendText(peerIp, text, this::loadMessages,
                error -> showSendErrorDialog(error));
    }

    private void showSendErrorDialog(String errorMsg) {
        String displayName = peerName != null ? peerName : (peerIp != null ? peerIp : "user");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Send Failed");
        builder.setMessage("You are not connected to " + displayName + ".\n\n" +
                errorMsg + "\n\n" +
                "How to Fix Easily:\n" +
                "1. Open WiFili: Ensure " + displayName + " has WiFili Messenger open on screen.\n" +
                "2. Same Wi-Fi / Hotspot: Connect both phones to same Wi-Fi or Hotspot.\n" +
                "3. Re-Scan: Tap '+' Scan button on main screen to update IP address.\n" +
                "4. Retry sending message.");
        builder.setPositiveButton("Re-Scan LAN", (dialog, which) -> {
            LanManager.getInstance(this).announce();
            Toast.makeText(this, "Scanning LAN devices...", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("OK", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lanManager.registerListener(this);
        loadMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lanManager.unregisterListener(this);
    }

    @Override
    public void onPeersUpdated(List<Peer> peerList) {}

    @Override
    public void onMessageReceived(String senderIp) {
        if (peerIp.equals(senderIp)) {
            loadMessages();
        }
    }

    @Override
    public void onProgressUpdated(long messageId, int progress, String peerIp) {
        if (this.peerIp != null && this.peerIp.equals(peerIp)) {
            chatAdapter.updateProgress(messageId, progress);
        }
    }

    @Override
    public void onCallOfferReceived(String senderIp, String senderName, String callType) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("peer_ip", senderIp);
        intent.putExtra("peer_name", senderName);
        intent.putExtra("call_type", callType);
        intent.putExtra("is_incoming", true);
        startActivity(intent);
    }
}
