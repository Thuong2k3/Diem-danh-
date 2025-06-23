package com.example.qlnv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmailForgot;
    private Button btnSendOtp;
    private RequestQueue requestQueue;
    private static final String FORGOT_PASSWORD_URL = "http://192.168.1.6:3000/api/forgot-password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmailForgot = findViewById(R.id.etEmailForgot);
        btnSendOtp = findViewById(R.id.btnSendOtp);
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
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, FORGOT_PASSWORD_URL, requestBody,
                response -> {
                    Toast.makeText(this, "Mã OTP đã được gửi. Vui lòng kiểm tra email.", Toast.LENGTH_LONG).show();
                    // Chuyển sang màn hình đặt lại mật khẩu
                    Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("USER_EMAIL", email); // Gửi email sang để dùng cho bước sau
                    startActivity(intent);
                    finish();
                },
                error -> {
                    String message = "Lỗi khi gửi mã.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            message = new JSONObject(responseBody).optString("message", message);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
        requestQueue.add(request);
    }
}
