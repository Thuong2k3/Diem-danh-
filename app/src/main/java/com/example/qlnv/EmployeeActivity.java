package com.example.qlnv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EmployeeActivity extends AppCompatActivity {

    private TextView tvWelcomeEmployee;
    private Button btnGoToAttendance, btnGoToEditProfile, btnGoToChangePassword;
    private String userId, userEmail;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Thêm phương thức này để xử lý sự kiện click vào item trong menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        // Xử lý nút back trên Toolbar (nếu có)
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Thêm phương thức này để thực hiện logic đăng xuất
    private void logoutUser() {
        // Xóa toàn bộ dữ liệu đã lưu trong SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Chuyển người dùng về màn hình Đăng nhập
        Intent intent = new Intent(EmployeeActivity.this, LoginActivity.class);
        // Xóa tất cả các Activity cũ khỏi stack để người dùng không thể nhấn back quay lại
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng Activity hiện tại
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        Toolbar toolbar = findViewById(R.id.toolbarEmployee);
        setSupportActionBar(toolbar);

        // Lấy thông tin người dùng đã lưu
        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
        userEmail = prefs.getString("USER_EMAIL", "Nhân viên");

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Ánh xạ các View
        tvWelcomeEmployee = findViewById(R.id.tvWelcomeEmployee);
        btnGoToAttendance = findViewById(R.id.btnGoToAttendance);
        btnGoToEditProfile = findViewById(R.id.btnGoToEditProfile);
        btnGoToChangePassword = findViewById(R.id.btnGoToChangePassword);

        // Hiển thị lời chào
        tvWelcomeEmployee.setText("Xin chào, " + userEmail);

        // Thiết lập sự kiện click cho các nút
        btnGoToAttendance.setOnClickListener(v -> {
            // Mở màn hình điểm danh mới
            Intent intent = new Intent(EmployeeActivity.this, AttendanceActivity.class);
            startActivity(intent);
        });

        btnGoToEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeActivity.this, UpdateProfileActivity.class);
            // Gửi thông tin cần thiết sang màn hình cập nhật
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_EMAIL", userEmail);
            startActivity(intent);
        });

        btnGoToChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(EmployeeActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }
}