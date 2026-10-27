package com.example.lanchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lanchat.network.LanManager;
import com.example.lanchat.utils.NetworkUtils;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvInitial, tvProfileName, tvSubName, tvSubNetwork;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("lanchat_prefs", MODE_PRIVATE);

        initViews();
        loadProfileData();
    }

    private void initViews() {
        tvInitial = findViewById(R.id.tv_profile_initial);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvSubName = findViewById(R.id.tv_sub_name);
        tvSubNetwork = findViewById(R.id.tv_sub_network);

        findViewById(R.id.btn_back_profile).setOnClickListener(v -> finish());

        findViewById(R.id.item_change_name).setOnClickListener(v -> showChangeNameDialog());
        findViewById(R.id.btn_edit_name).setOnClickListener(v -> showChangeNameDialog());

        findViewById(R.id.item_network_info).setOnClickListener(v ->
                startActivity(new Intent(this, NetworkActivity.class)));
    }

    private void loadProfileData() {
        String name = prefs.getString("user_name", "Mohsin");
        tvProfileName.setText(name);
        tvSubName.setText(name);

        String initial = (!name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "M";
        tvInitial.setText(initial);

        String ssid = NetworkUtils.getWifiSsid(this);
        tvSubNetwork.setText("Connected to: " + ssid);
    }

    private void showChangeNameDialog() {
        String currentName = prefs.getString("user_name", "");
        final EditText input = new EditText(this);
        input.setText(currentName);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Change Name")
                .setMessage("Enter new display name:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        prefs.edit().putString("user_name", newName).apply();
                        LanManager.getInstance(this).setDeviceName(newName);
                        loadProfileData();
                        Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
