package com.example.qlnv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class IpConfigActivity extends AppCompatActivity {

    public static final String CONFIG_PREFS = "IP_CONFIG_PREFS";
    public static final String KEY_IP_ADDRESS = "IP_ADDRESS";
    public static final String KEY_PORT = "PORT";

    private EditText etIpAddress, etPort;
    private Button btnSaveIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_config);

        etIpAddress = findViewById(R.id.etIpAddress);
        etPort = findViewById(R.id.etPort);
        btnSaveIp = findViewById(R.id.btnSaveIp);

        btnSaveIp.setOnClickListener(v -> {
            String ip = etIpAddress.getText().toString().trim();
            String port = etPort.getText().toString().trim();

            if (ip.isEmpty() || port.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ IP và Port", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lưu IP và Port vào SharedPreferences
            SharedPreferences prefs = getSharedPreferences(CONFIG_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_IP_ADDRESS, ip);
            editor.putString(KEY_PORT, port);
            editor.apply();

            // Chuyển sang LoginActivity
            Intent intent = new Intent(IpConfigActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Đóng Activity này lại
        });
    }
}