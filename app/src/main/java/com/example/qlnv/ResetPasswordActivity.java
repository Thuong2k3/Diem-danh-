package com.example.qlnv;

import android.content.Intent;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ResetPasswordActivity";

    private EditText etOtp, etNewPassword, etConfirmPassword;
    private Button btnResetPassword;
    private ProgressBar progressBar;
    private String userEmail;
    private RequestQueue requestQueue;

    // Regex mật khẩu mạnh (giữ nguyên từ file cũ của bạn)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^" +
            "(?=.*[0-9])" +         // phải có ít nhất 1 chữ số
            "(?=.*[a-z])" +         // phải có ít nhất 1 chữ thường
            "(?=.*[A-Z])" +         // phải có ít nhất 1 chữ hoa
            "(?=.*[@#$%^&+=])" +    // phải có ít nhất 1 ký tự đặc biệt
            "(?=\\S+$)" +           // không có khoảng trắng
            ".{8,}" +               // ít nhất 8 ký tự
            "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarResetPassword);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đặt lại mật khẩu");
        }

        // Lấy email được gửi từ ForgotPasswordActivity
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null) {
            Toast.makeText(this, "Lỗi, không tìm thấy thông tin email.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ views
        etOtp = findViewById(R.id.etOtpReset);
        etNewPassword = findViewById(R.id.etNewPasswordReset);
        etConfirmPassword = findViewById(R.id.etConfirmNewPasswordReset);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBarResetPassword);
        requestQueue = Volley.newRequestQueue(this);

        btnResetPassword.setOnClickListener(v -> {
            if (validateInput()) {
                performResetPassword();
            }
        });
    }

    private boolean validateInput() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (otp.length() != 6) {
            etOtp.setError("Mã OTP phải có 6 chữ số");
            etOtp.requestFocus();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(newPass).matches()) {
            etNewPassword.setError("Mật khẩu không đủ mạnh");
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ hoa, thường, số, ký tự đặc biệt.", Toast.LENGTH_LONG).show();
            etNewPassword.requestFocus();
            return false;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void performResetPassword() {
        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", userEmail);
            requestBody.put("otp", otp);
            requestBody.put("newPassword", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            btnResetPassword.setEnabled(true);
            return;
        }

        // SỬA ĐỔI: Lấy URL động và sử dụng đúng route từ auth.js
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/reset-password";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();

                    // Chuyển về màn hình đăng nhập và xóa các màn hình cũ khỏi stack
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnResetPassword.setEnabled(true);
                    String message = "Lỗi khi đặt lại mật khẩu.";
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
                });

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
