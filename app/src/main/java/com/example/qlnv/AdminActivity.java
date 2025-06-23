package com.example.qlnv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.SharedPreferences;


import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity implements AdminFunctionAdapter.OnFunctionClickListener {

    private RecyclerView recyclerViewAdminFunctions;
    private AdminFunctionAdapter adapter;
    private List<AdminFunction> functionList;

    // Định nghĩa các ID cho chức năng
    private static final int FUNC_ID_MANAGE_USERS = 1;
    private static final int FUNC_ID_VIEW_ATTENDANCE_REPORTS = 2;
    private static final int FUNC_ID_CHANGE_PASSWORD = 3;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }
        recyclerViewAdminFunctions = findViewById(R.id.recyclerViewAdminFunctions);
        recyclerViewAdminFunctions = findViewById(R.id.recyclerViewAdminFunctions);
        recyclerViewAdminFunctions.setLayoutManager(new LinearLayoutManager(this));

        loadFunctions();

        adapter = new AdminFunctionAdapter(functionList, this);
        recyclerViewAdminFunctions.setAdapter(adapter);
    }

    private void loadFunctions() {
        functionList = new ArrayList<>();
        functionList.add(new AdminFunction(FUNC_ID_MANAGE_USERS, "Quản lý Người dùng/Nhân viên"));
        functionList.add(new AdminFunction(FUNC_ID_VIEW_ATTENDANCE_REPORTS, "Xem Thông tin Điểm danh"));
        functionList.add(new AdminFunction(FUNC_ID_CHANGE_PASSWORD, "Đổi mật khẩu"));
    }

    @Override
    public void onFunctionClick(AdminFunction adminFunction) {
        Intent intent;
        switch (adminFunction.getId()) {
            case FUNC_ID_MANAGE_USERS:
                // Mở màn hình quản lý người dùng
                intent = new Intent(this, UserListActivity.class);
                intent.putExtra("PURPOSE", "MANAGE_USERS"); // Gửi mục đích để UserListActivity biết
                startActivity(intent);
                break;

            case FUNC_ID_VIEW_ATTENDANCE_REPORTS:
                // Mở màn hình danh sách người dùng để chọn xem điểm danh
                intent = new Intent(this, UserListActivity.class);
                intent.putExtra("PURPOSE", "VIEW_ATTENDANCE"); // Gửi mục đích khác
                startActivity(intent);
                break;

            case FUNC_ID_CHANGE_PASSWORD:
                // Mở màn hình đổi mật khẩu
                intent = new Intent(this, ChangePasswordActivity.class);
                startActivity(intent);
                break;

            default:
                Toast.makeText(this, "Chức năng chưa được triển khai.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
