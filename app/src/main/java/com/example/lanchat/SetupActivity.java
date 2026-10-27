package com.example.lanchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lanchat.network.LanManager;

public class SetupActivity extends AppCompatActivity {

    private EditText etUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        etUserName = findViewById(R.id.et_user_name);

        findViewById(R.id.btn_connect).setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("lanchat_prefs", MODE_PRIVATE);
            prefs.edit().putString("user_name", name).apply();

            LanManager.getInstance(this).setDeviceName(name);

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
