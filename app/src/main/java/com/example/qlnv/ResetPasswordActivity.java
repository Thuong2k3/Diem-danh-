package com.example.qlnv;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOtp, etNewPassword, etConfirmPassword;
    private Button btnResetPassword;
    private String userEmail;
    private RequestQueue requestQueue;

    private static final String RESET_PASSWORD_URL = "http://192.168.1.6/api/reset-password";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^" + "(?=.*[0-9])" + "(?=.*[a-z])" + "(?=.*[A-Z])" + "(?=.*[@#$%^&+=])" + "(?=\\S+$)" + ".{8,}" + "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null) {
            Toast.makeText(this, "Lỗi, không tìm thấy email.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etOtp = findViewById(R.id.etOtpReset);
        etNewPassword = findViewById(R.id.etNewPasswordReset);
        etConfirmPassword = findViewById(R.id.etConfirmNewPasswordReset);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        requestQueue = Volley.newRequestQueue(this);

        btnResetPassword.setOnClickListener(v -> {
            if (validateInput()) {
                performResetPassword();
            }
        });
    }

    private boolean validateInput() {
        String newPass = etNewPassword.getText().toString().trim();
        if (!PASSWORD_PATTERN.matcher(newPass).matches()) {
            etNewPassword.setError("Mật khẩu không đủ mạnh");
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ hoa, thường, số, ký tự đặc biệt.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!newPass.equals(etConfirmPassword.getText().toString().trim())) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return false;
        }
        return true;
    }

    private void performResetPassword() {
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", userEmail);
            requestBody.put("otp", otp);
            requestBody.put("newPassword", newPassword);
        } catch (JSONException e) { e.printStackTrace(); return; }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, RESET_PASSWORD_URL, requestBody,
                response -> {
                    Toast.makeText(this, "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    String message = "Lỗi khi đặt lại mật khẩu.";
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