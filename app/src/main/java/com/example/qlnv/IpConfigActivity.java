package com.example.qlnv;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.regex.Pattern;

public class IpConfigActivity extends AppCompatActivity {

    private EditText etIpAddress;
    private Button btnSaveIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_config);

        etIpAddress = findViewById(R.id.etIpAddress);
        btnSaveIp = findViewById(R.id.btnSaveIp);

        // Lấy và hiển thị IP đã lưu trước đó (nếu có)
        String savedIp = ApiConfig.getSavedIp(this);
        if (!TextUtils.isEmpty(savedIp)) {
            etIpAddress.setText(savedIp);
        }

        btnSaveIp.setOnClickListener(v -> {
            String ip = etIpAddress.getText().toString().trim();
            if (isValidIp(ip)) {
                // Lưu IP mới bằng phương thức trong ApiConfig
                ApiConfig.setIpAddress(this, ip);
                Toast.makeText(this, "Đã lưu địa chỉ IP: " + ip, Toast.LENGTH_SHORT).show();

                // Chuyển đến màn hình Login
                Intent intent = new Intent(IpConfigActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Đóng màn hình này lại
            } else {
                etIpAddress.setError("Địa chỉ IP không hợp lệ");
            }
        });
    }

    private boolean isValidIp(String ip) {
        if (TextUtils.isEmpty(ip)) return false;
        // Biểu thức chính quy đơn giản để kiểm tra định dạng IPv4
        String ipRegex = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        return Pattern.compile(ipRegex).matcher(ip).matches();
    }
}
