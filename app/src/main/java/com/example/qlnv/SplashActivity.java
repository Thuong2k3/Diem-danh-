package com.example.qlnv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity { // Hoặc tên Activity đầu tiên của bạn

    private static final int SPLASH_DELAY = 2000; // Thời gian hiển thị màn hình splash (ms)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Layout cho màn hình splash

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(IpConfigActivity.CONFIG_PREFS, Context.MODE_PRIVATE);
            String ipAddress = prefs.getString(IpConfigActivity.KEY_IP_ADDRESS, null);
            String port = prefs.getString(IpConfigActivity.KEY_PORT, null);

            Intent nextIntent;
            if (ipAddress == null || port == null || ipAddress.isEmpty() || port.isEmpty()) {
                // Chưa có IP, chuyển đến màn hình cấu hình IP
                nextIntent = new Intent(SplashActivity.this, IpConfigActivity.class);
            } else {
                // Đã có IP, chuyển đến màn hình Login (hoặc màn hình chính nếu đã đăng nhập)
                nextIntent = new Intent(SplashActivity.this, LoginActivity.class); // Thay LoginActivity bằng Activity phù hợp
            }
            startActivity(nextIntent);
            finish();
        }, SPLASH_DELAY);
    }
}