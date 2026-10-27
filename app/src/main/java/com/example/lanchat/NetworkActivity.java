package com.example.lanchat;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lanchat.network.LanManager;
import com.example.lanchat.utils.NetworkUtils;

public class NetworkActivity extends AppCompatActivity {

    private TextView tvConnStatus, tvSsidHeader, tvSsidVal, tvIpVal, tvDevicesVal;
    private Button btnTestConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        initViews();
        loadNetworkInfo();
    }

    private void initViews() {
        tvConnStatus = findViewById(R.id.tv_conn_status);
        tvSsidHeader = findViewById(R.id.tv_ssid_header);
        tvSsidVal = findViewById(R.id.tv_ssid_val);
        tvIpVal = findViewById(R.id.tv_ip_val);
        tvDevicesVal = findViewById(R.id.tv_devices_val);
        btnTestConn = findViewById(R.id.btn_test_conn);

        findViewById(R.id.btn_back_network).setOnClickListener(v -> finish());

        btnTestConn.setOnClickListener(v -> {
            LanManager.getInstance(this).announce();
            loadNetworkInfo();
            int devicesCount = LanManager.getInstance(this).getPeersList().size();
            String ip = NetworkUtils.getLocalIpAddress(this);
            Toast.makeText(this, "⚡ Scan triggered! IP: " + ip + " • Found " + devicesCount + " LAN user(s)", Toast.LENGTH_LONG).show();
        });
    }

    private void loadNetworkInfo() {
        String ssid = NetworkUtils.getWifiSsid(this);
        String ip = NetworkUtils.getLocalIpAddress(this);
        int devicesCount = LanManager.getInstance(this).getPeersList().size();

        boolean isConnected = ip != null && !ip.equals("127.0.0.1") && !ip.equals("0.0.0.0");

        if (isConnected) {
            tvConnStatus.setText("● Connected & Active");
            tvConnStatus.setTextColor(Color.parseColor("#22C55E"));
        } else {
            tvConnStatus.setText("● Disconnected / Connect Wi-Fi");
            tvConnStatus.setTextColor(Color.parseColor("#EF4444"));
        }

        tvSsidHeader.setText(ssid);
        tvSsidVal.setText(ssid);
        tvIpVal.setText(ip);
        tvDevicesVal.setText(devicesCount + (devicesCount == 1 ? " User" : " Users"));
    }
}
