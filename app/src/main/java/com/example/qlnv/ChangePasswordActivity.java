package com.example.qlnv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSaveChanges;
    private RequestQueue requestQueue;
    private String userId;
    private String token;

    private static final String CHANGE_PASSWORD_URL = "http://192.168.1.6:3000/api/change-password"; // Thay IP nếu cần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Toolbar toolbar = findViewById(R.id.toolbarChangePassword);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        requestQueue = Volley.newRequestQueue(this);

        // Lấy thông tin user đã lưu
        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
        token = prefs.getString("AUTH_TOKEN", null);

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnSaveChanges.setOnClickListener(v -> {
            if (validateInput()) {
                performChangePassword();
            }
        });
    }

    private boolean validateInput() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();

        if (etCurrentPassword.getText().toString().trim().isEmpty()) {
            etCurrentPassword.setError("Mật khẩu hiện tại không được để trống");
            return false;
        }
        if (newPass.isEmpty() || newPass.length() < 6) {
            etNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
            return false;
        }
        if (!newPass.equals(confirmPass)) {
            etConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
            return false;
        }
        return true;
    }

    private void performChangePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId); // Gửi userId để server biết đổi mật khẩu cho ai
            requestBody.put("currentPassword", currentPassword);
            requestBody.put("newPassword", newPassword);
        } catch (JSONException e) {
            Log.e("ChangePassword", "Error creating JSON body", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, CHANGE_PASSWORD_URL, requestBody,
                response -> {
                    try {
                        String status = response.getString("status");
                        String message = response.optString("message", "Đổi mật khẩu thành công!");
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        if ("success".equals(status)) {
                            finish(); // Đóng màn hình và quay lại trang trước
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    String message = "Lỗi khi đổi mật khẩu.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("message", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Log.e("ChangePassword", "Error: " + error.toString());
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        requestQueue.add(request);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}