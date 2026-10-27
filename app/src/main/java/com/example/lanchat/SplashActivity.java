package com.example.lanchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("lanchat_prefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", null);

        if (name != null && !name.trim().isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, SetupActivity.class));
            finish();
        });
    }
}
