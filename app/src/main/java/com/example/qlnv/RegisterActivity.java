package com.example.qlnv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    // Views cho Bước 1
    private LinearLayout layoutStep1;
    private EditText etEmailRegister, etPasswordRegister;
    private Spinner spinnerRole;
    private Button btnRequestOtp;

    // Views cho Bước 2
    private LinearLayout layoutStep2;
    private TextView tvOtpInfo;
    private EditText etOtpCode;
    private Button btnFinalRegister;
    private TextView tvResendOtp;

    private TextView tvLoginHere;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "http://192.168.1.6:3000/api/"; // Thay IP nếu cần
    private static final String REQUEST_OTP_URL = BASE_URL + "request-otp";
    private static final String REGISTER_URL = BASE_URL + "register";

    // Regex để kiểm tra mật khẩu mạnh
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
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
        setContentView(R.layout.activity_register);

        // Ánh xạ Views
        layoutStep1 = findViewById(R.id.layoutStep1);
        etEmailRegister = findViewById(R.id.etEmailRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);

        layoutStep2 = findViewById(R.id.layoutStep2);
        tvOtpInfo = findViewById(R.id.tvOtpInfo);
        etOtpCode = findViewById(R.id.etOtpCode);
        btnFinalRegister = findViewById(R.id.btnFinalRegister);
        tvResendOtp = findViewById(R.id.tvResendOtp);

        tvLoginHere = findViewById(R.id.tvLoginHere);

        requestQueue = Volley.newRequestQueue(this);

        // Thiết lập Spinner
        String[] rolesDisplay = {"Nhân viên", "Quản trị viên"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rolesDisplay);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Thiết lập sự kiện click
        btnRequestOtp.setOnClickListener(v -> handleRequestOtp());
        btnFinalRegister.setOnClickListener(v -> handleFinalRegistration());
        tvLoginHere.setOnClickListener(v -> finish());
        tvResendOtp.setOnClickListener(v -> {
            // Cho phép gửi lại OTP (logic đơn giản là gọi lại handleRequestOtp)
            Toast.makeText(this, "Đang gửi lại mã...", Toast.LENGTH_SHORT).show();
            handleRequestOtp();
        });
    }

    private boolean validateStep1() {
        String email = etEmailRegister.getText().toString().trim();
        String password = etPasswordRegister.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailRegister.setError("Email không hợp lệ");
            etEmailRegister.requestFocus();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            etPasswordRegister.setError("Mật khẩu không đủ mạnh");
            etPasswordRegister.requestFocus();
            Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, thường, số, ký tự đặc biệt.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void handleRequestOtp() {
        if (!validateStep1()) {
            return;
        }

        String email = etEmailRegister.getText().toString().trim();
        String password = etPasswordRegister.getText().toString().trim();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("password", password); // Gửi cả mật khẩu để server kiểm tra độ mạnh
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, REQUEST_OTP_URL, requestBody,
                response -> {
                    Toast.makeText(this, "Mã OTP đã được gửi. Vui lòng kiểm tra email.", Toast.LENGTH_LONG).show();
                    // Chuyển sang bước 2
                    layoutStep1.setVisibility(View.GONE);
                    layoutStep2.setVisibility(View.VISIBLE);
                    tvOtpInfo.setText("Một mã OTP đã được gửi đến\n" + email);
                },
                error -> {
                    String message = "Lỗi khi gửi mã.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("message", message);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
        requestQueue.add(request);
    }

    private void handleFinalRegistration() {
        String email = etEmailRegister.getText().toString().trim();
        String password = etPasswordRegister.getText().toString().trim();
        String otp = etOtpCode.getText().toString().trim();
        String selectedRoleDisplay = spinnerRole.getSelectedItem().toString();
        String role = "employee"; // Mặc định
        if ("Quản trị viên".equals(selectedRoleDisplay)) {
            role = "admin";
        }

        if (otp.length() != 6) {
            etOtpCode.setError("Mã OTP phải có 6 số");
            etOtpCode.requestFocus();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("role", role);
            requestBody.put("otp", otp);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, REGISTER_URL, requestBody,
                response -> {
                    Toast.makeText(this, "Đăng ký tài khoản thành công!", Toast.LENGTH_LONG).show();
                    // Chuyển về màn hình đăng nhập
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                },
                error -> {
                    String message = "Đăng ký thất bại.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("message", message);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
        requestQueue.add(request);
    }
}
