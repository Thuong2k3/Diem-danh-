package com.example.qlnv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // Thời gian chờ: 2 giây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Luôn chuyển đến màn hình IpConfigActivity mỗi khi khởi động ứng dụng
            Intent intent = new Intent(SplashActivity.this, IpConfigActivity.class);
            startActivity(intent);
            finish(); // Đóng SplashActivity để người dùng không thể quay lại
        }, SPLASH_DELAY);
    }
}
