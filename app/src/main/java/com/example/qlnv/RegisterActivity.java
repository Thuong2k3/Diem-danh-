package com.example.qlnv;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RegisterActivity";

    // Views chung
    private TextView tvLoginHere;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    // Views cho Bước 1: Nhập thông tin
    private LinearLayout layoutStep1;
    private EditText etEmailRegister, etPasswordRegister;
    private Spinner spinnerRole;
    private Button btnRequestOtp;

    // Views cho Bước 2: Nhập OTP
    private LinearLayout layoutStep2;
    private TextView tvOtpInfo, tvResendOtp;
    private EditText etOtpCode;
    private Button btnFinalRegister;

    // Regex kiểm tra mật khẩu mạnh
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
        initViews();

        requestQueue = Volley.newRequestQueue(this);

        // Thiết lập Spinner
        setupSpinner();

        // Thiết lập sự kiện click
        setupClickListeners();
    }

    private void initViews() {
        // Chung
        tvLoginHere = findViewById(R.id.tvLoginHere);
        progressBar = findViewById(R.id.progressBar);

        // Bước 1
        layoutStep1 = findViewById(R.id.layoutStep1);
        etEmailRegister = findViewById(R.id.etEmailRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);

        // Bước 2
        layoutStep2 = findViewById(R.id.layoutStep2);
        tvOtpInfo = findViewById(R.id.tvOtpInfo);
        etOtpCode = findViewById(R.id.etOtpCode);
        btnFinalRegister = findViewById(R.id.btnFinalRegister);
        tvResendOtp = findViewById(R.id.tvResendOtp);
    }

    private void setupSpinner() {
        String[] rolesDisplay = {"Nhân viên", "Quản trị viên"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rolesDisplay);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void setupClickListeners() {
        tvLoginHere.setOnClickListener(v -> finish());
        btnRequestOtp.setOnClickListener(v -> handleRequestOtp());
        btnFinalRegister.setOnClickListener(v -> handleFinalRegistration());
        tvResendOtp.setOnClickListener(v -> handleRequestOtp()); // Gửi lại OTP cũng gọi hàm handleRequestOtp
    }

    private boolean validateStep1() {
        String email = etEmailRegister.getText().toString().trim();
        String password = etPasswordRegister.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailRegister.setError("Email không hợp lệ");
            etEmailRegister.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPasswordRegister.setError("Vui lòng nhập mật khẩu");
            etPasswordRegister.requestFocus();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            etPasswordRegister.setError("Mật khẩu không đủ mạnh");
            etPasswordRegister.requestFocus();
            Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, thường, số, và ký tự đặc biệt.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void handleRequestOtp() {
        if (!validateStep1()) {
            return;
        }

        setLoading(true);

        String email = etEmailRegister.getText().toString().trim();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            setLoading(false);
            return;
        }

        // GIẢ ĐỊNH: Bạn cần có một route mới trên server để gửi OTP
        String url = ApiConfig.getBaseUrl(this) + "auth/request-otp";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    setLoading(false);
                    Toast.makeText(this, "Mã OTP đã được gửi đến email của bạn.", Toast.LENGTH_SHORT).show();
                    // Chuyển sang bước 2
                    layoutStep1.setVisibility(View.GONE);
                    layoutStep2.setVisibility(View.VISIBLE);
                    tvOtpInfo.setText("Một mã OTP đã được gửi đến\n" + email);
                },
                error -> {
                    setLoading(false);
                    showErrorToast(error, "Gửi mã OTP thất bại.");
                });

        requestQueue.add(request);
    }

    private void handleFinalRegistration() {
        String otp = etOtpCode.getText().toString().trim();
        if (otp.length() != 6) {
            etOtpCode.setError("Mã OTP phải có 6 chữ số");
            etOtpCode.requestFocus();
            return;
        }

        setLoading(true);

        String email = etEmailRegister.getText().toString().trim();
        String password = etPasswordRegister.getText().toString().trim();
        String selectedRoleDisplay = spinnerRole.getSelectedItem().toString();
        String role = "employee".equals(selectedRoleDisplay) ? "employee" : "admin";

        JSONObject requestBody = new JSONObject();
        try {
            // Dựa theo User.js, bạn cần username (lấy từ email), email, password, role và otp
            String username = email.split("@")[0];
            requestBody.put("username", username);
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("role", role);
            requestBody.put("otp", otp); // Thêm mã OTP vào request
        } catch (JSONException e) {
            e.printStackTrace();
            setLoading(false);
            return;
        }

        // GIẢ ĐỊNH: Route đăng ký cuối cùng của bạn sẽ xử lý cả OTP
        String url = ApiConfig.getBaseUrl(this) + "auth/register";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    setLoading(false);
                    Toast.makeText(this, "Đăng ký tài khoản thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                    finish(); // Quay lại màn hình đăng nhập
                },
                error -> {
                    setLoading(false);
                    showErrorToast(error, "Đăng ký thất bại.");
                });

        requestQueue.add(request);
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnRequestOtp.setEnabled(false);
            btnFinalRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnRequestOtp.setEnabled(true);
            btnFinalRegister.setEnabled(true);
        }
    }

    private void showErrorToast(com.android.volley.VolleyError error, String defaultMessage) {
        String message = defaultMessage;
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
    }
}