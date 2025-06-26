package com.example.qlnv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ChangePasswordActivity";

    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSaveChanges;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private String userId;
    private String token;

    // XÓA BỎ URL CỨNG
    // private static final String CHANGE_PASSWORD_URL = "http://192.168.1.6:3000/api/change-password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Toolbar toolbar = findViewById(R.id.toolbarChangePassword);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đổi mật khẩu");
        }

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        progressBar = findViewById(R.id.progressBarChangePassword); // Tham chiếu ProgressBar

        requestQueue = Volley.newRequestQueue(this);

        // Lấy thông tin user đã lưu
        SharedPreferences prefs = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE);
        userId = prefs.getString(LoginActivity.USER_ID_KEY, null);
        token = prefs.getString(LoginActivity.TOKEN_KEY, null);

        if (userId == null || token == null) {
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
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();

        if (currentPass.isEmpty()) {
            etCurrentPassword.setError("Mật khẩu hiện tại không được để trống");
            etCurrentPassword.requestFocus();
            return false;
        }
        if (newPass.length() < 8) {
            etNewPassword.setError("Mật khẩu mới phải có ít nhất 8 ký tự");
            etNewPassword.requestFocus();
            return false;
        }
        if (!newPass.equals(confirmPass)) {
            etConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmNewPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void performChangePassword() {
        progressBar.setVisibility(View.VISIBLE);
        btnSaveChanges.setEnabled(false);

        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        JSONObject requestBody = new JSONObject();
        try {
            // Dựa theo backend, bạn cần userId, currentPassword, và newPassword
            requestBody.put("userId", userId);
            requestBody.put("currentPassword", currentPassword);
            requestBody.put("newPassword", newPassword);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error creating JSON body", e);
            progressBar.setVisibility(View.GONE);
            btnSaveChanges.setEnabled(true);
            return;
        }

        // SỬA ĐỔI: Lấy URL động và sử dụng đúng route
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/change-password";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    try {
                        String message = response.optString("message", "Đổi mật khẩu thành công!");
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        finish(); // Đóng màn hình và quay lại
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing success response", e);
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    String message = "Lỗi khi đổi mật khẩu.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("message", message);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error parsing error response", e);
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
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
