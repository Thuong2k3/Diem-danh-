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

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ForgotPasswordActivity";

    private EditText etEmailForgot;
    private Button btnSendOtp;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    // XÓA BỎ URL CỨNG
    // private static final String FORGOT_PASSWORD_URL = "http://192.168.1.6:3000/api/forgot-password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarForgotPassword);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quên mật khẩu");
        }

        // Ánh xạ views
        etEmailForgot = findViewById(R.id.etEmailForgot);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        progressBar = findViewById(R.id.progressBarForgotPassword);
        requestQueue = Volley.newRequestQueue(this);

        btnSendOtp.setOnClickListener(v -> {
            String email = etEmailForgot.getText().toString().trim();
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmailForgot.setError("Vui lòng nhập email hợp lệ.");
                return;
            }
            requestOtp(email);
        });
    }

    private void requestOtp(String email) {
        progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            btnSendOtp.setEnabled(true);
            return;
        }

        // SỬA ĐỔI: Lấy URL động và sử dụng đúng route từ auth.js
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/forgot-password";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendOtp.setEnabled(true);

                    Toast.makeText(this, "Mã OTP đã được gửi. Vui lòng kiểm tra email.", Toast.LENGTH_LONG).show();

                    // Chuyển sang màn hình đặt lại mật khẩu
                    Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("USER_EMAIL", email); // Gửi email sang để dùng cho bước sau
                    startActivity(intent);
                    finish();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendOtp.setEnabled(true);
                    String message = "Lỗi khi gửi yêu cầu.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("message", message);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Lỗi phân tích phản hồi lỗi", e);
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
