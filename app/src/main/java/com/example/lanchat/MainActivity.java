package com.example.lanchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.lanchat.adapter.FileAdapter;
import com.example.lanchat.adapter.PeerAdapter;
import com.example.lanchat.db.DatabaseHelper;
import com.example.lanchat.model.Peer;
import com.example.lanchat.model.SharedFile;
import com.example.lanchat.network.LanManager;
import com.example.lanchat.utils.InsetsUtils;
import com.example.lanchat.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LanManager.Listener {

    private TextView tabChats, tabFiles;
    private View tabIndicator;
    private RecyclerView rvItems;
    private View emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutSearch;
    private EditText etSearchPeer;
    private TextView tvUserWifiSub;

    private PeerAdapter peerAdapter;
    private FileAdapter fileAdapter;
    private LanManager lanManager;
    private DatabaseHelper db;

    private boolean isChatsTabSelected = true;
    private final List<Peer> currentAllPeers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("lanchat_prefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", null);
        if (name == null || name.trim().isEmpty()) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        InsetsUtils.applyTopAndBottomInsets(
                findViewById(R.id.root_main),
                findViewById(R.id.top_header_container),
                findViewById(R.id.fab_add)
        );

        lanManager = LanManager.getInstance(this);
        lanManager.setDeviceName(name);
        lanManager.start();

        db = new DatabaseHelper(this);

        initViews();
        setupListeners();
        setupAdapters();

        updateTopBarInfo();
        loadChatsTab();
    }

    private void initViews() {
        tabChats = findViewById(R.id.tab_chats);
        tabFiles = findViewById(R.id.tab_files);
        tabIndicator = findViewById(R.id.tab_indicator);
        rvItems = findViewById(R.id.rv_items);
        emptyState = findViewById(R.id.empty_state);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        layoutSearch = findViewById(R.id.layout_search);
        etSearchPeer = findViewById(R.id.et_search_peer);
        tvUserWifiSub = findViewById(R.id.tv_user_wifi_sub);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void updateTopBarInfo() {
        SharedPreferences prefs = getSharedPreferences("lanchat_prefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "User");
        String ssid = NetworkUtils.getWifiSsid(this);
        if (tvUserWifiSub != null) {
            tvUserWifiSub.setText("👤 " + name + " • 📶 " + ssid);
        }
    }

    private void setupListeners() {
        tabChats.setOnClickListener(v -> selectTab(true));
        tabFiles.setOnClickListener(v -> selectTab(false));

        findViewById(R.id.btn_network_info).setOnClickListener(v ->
                startActivity(new Intent(this, NetworkActivity.class)));

        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.btn_search_toggle).setOnClickListener(v -> {
            if (layoutSearch.getVisibility() == View.VISIBLE) {
                layoutSearch.setVisibility(View.GONE);
                etSearchPeer.setText("");
                filterPeers("");
            } else {
                layoutSearch.setVisibility(View.VISIBLE);
                etSearchPeer.requestFocus();
            }
        });

        etSearchPeer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPeers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.fab_add).setOnClickListener(v -> showDiscoverableUsersDialog());

        swipeRefresh.setOnRefreshListener(() -> {
            lanManager.announce();
            if (isChatsTabSelected) loadChatsTab();
            else loadFilesTab();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void showDiscoverableUsersDialog() {
        lanManager.announce();
        List<Peer> peers = lanManager.getPeersList();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (peers.isEmpty()) {
            builder.setTitle("🔍 Scanning LAN Network");
            builder.setMessage("No other WiFili users found on this Wi-Fi network yet.\n\nHow to fix:\n1. Open WiFili Messenger on the other phone.\n2. Connect both phones to the exact same Wi-Fi or Hotspot.\n3. Tap 'Scan Again'.");
            builder.setPositiveButton("Scan Again", (dialog, which) -> showDiscoverableUsersDialog());
            builder.setNeutralButton("Fix Connection Guide", (dialog, which) ->
                    startActivity(new Intent(this, NetworkActivity.class)));
            builder.setNegativeButton("Close", null);
        } else {
            builder.setTitle("⚡ Found " + peers.size() + " Discoverable User(s)");

            String[] names = new String[peers.size()];
            for (int i = 0; i < peers.size(); i++) {
                Peer p = peers.get(i);
                names[i] = "👤 " + p.getName() + " (" + p.getIp() + ") • Online";
            }

            builder.setItems(names, (dialog, which) -> {
                Peer selected = peers.get(which);
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("peer_id", selected.getId());
                intent.putExtra("peer_name", selected.getName());
                intent.putExtra("peer_ip", selected.getIp());
                startActivity(intent);
            });

            builder.setPositiveButton("Scan Again", (dialog, which) -> showDiscoverableUsersDialog());
            builder.setNegativeButton("Close", null);
        }

        builder.show();
    }

    private void setupAdapters() {
        peerAdapter = new PeerAdapter(peer -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("peer_id", peer.getId());
            intent.putExtra("peer_name", peer.getName());
            intent.putExtra("peer_ip", peer.getIp());
            startActivity(intent);
        });

        fileAdapter = new FileAdapter(null);
    }

    private void filterPeers(String query) {
        if (query == null || query.trim().isEmpty()) {
            peerAdapter.setPeers(currentAllPeers);
            emptyState.setVisibility(currentAllPeers.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Peer> filtered = new ArrayList<>();
        for (Peer p : currentAllPeers) {
            if ((p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q)) ||
                    (p.getIp() != null && p.getIp().contains(q))) {
                filtered.add(p);
            }
        }
        peerAdapter.setPeers(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void selectTab(boolean isChats) {
        isChatsTabSelected = isChats;
        if (isChats) {
            tabChats.setTextColor(0xFF1E65F3);
            tabFiles.setTextColor(0xFF64748B);
            tabIndicator.animate().translationX(0).setDuration(200).start();
            loadChatsTab();
        } else {
            tabChats.setTextColor(0xFF64748B);
            tabFiles.setTextColor(0xFF1E65F3);
            tabIndicator.animate().translationX(rvItems.getWidth() / 2f).setDuration(200).start();
            loadFilesTab();
        }
    }

    private void loadChatsTab() {
        rvItems.setAdapter(peerAdapter);
        currentAllPeers.clear();
        currentAllPeers.addAll(lanManager.getPeersList());
        filterPeers(etSearchPeer.getText().toString());
    }

    private void loadFilesTab() {
        rvItems.setAdapter(fileAdapter);
        List<SharedFile> sharedFiles = db.getAllSharedFiles();
        fileAdapter.setFiles(sharedFiles);

        emptyState.setVisibility(sharedFiles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lanManager.registerListener(this);
        updateTopBarInfo();
        if (isChatsTabSelected) loadChatsTab();
        else loadFilesTab();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lanManager.unregisterListener(this);
    }

    @Override
    public void onPeersUpdated(List<Peer> peerList) {
        currentAllPeers.clear();
        currentAllPeers.addAll(peerList);
        if (isChatsTabSelected) {
            filterPeers(etSearchPeer.getText().toString());
        }
    }

    @Override
    public void onMessageReceived(String senderIp) {
        if (isChatsTabSelected) loadChatsTab();
        else loadFilesTab();
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
