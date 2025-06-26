package com.example.qlnv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError; // Thêm import này
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private TextView tvForgotPassword;
    private RequestQueue requestQueue;

    // Các hằng số để lưu trữ thông tin vào SharedPreferences
    public static final String AUTH_PREFS = "AUTH_PREFS";
    public static final String USER_ID_KEY = "USER_ID";
    public static final String TOKEN_KEY = "TOKEN";
    public static final String USER_ROLE_KEY = "USER_ROLE";
    public static final String PROFILE_COMPLETED_KEY = "PROFILE_COMPLETED";
    public static final String USER_EMAIL_KEY = "USER_EMAIL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        requestQueue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }
            loginUser(email, password);
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(final String email, final String password) {
        // Vô hiệu hóa nút để tránh người dùng nhấn nhiều lần
        btnLogin.setEnabled(false);

        // Lấy URL động từ ApiConfig
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/login";

        Log.d("LoginActivity", "Connecting to URL: " + url);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Bật lại nút sau khi có phản hồi
                    btnLogin.setEnabled(true);
                    try {
                        JSONObject json = new JSONObject(response);
                        String status = json.optString("status");

                        if ("success".equals(status)) {
                            String role = json.optString("role");
                            boolean profileCompleted = json.optBoolean("profileCompleted", false);
                            String token = json.optString("token");
                            String userId = "";
                            String userEmail = "";

                            if (json.has("user")) {
                                JSONObject userObject = json.getJSONObject("user");
                                userId = userObject.optString("id");
                                userEmail = userObject.optString("email");
                            }

                            // Lưu thông tin đăng nhập
                            SharedPreferences prefs = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(TOKEN_KEY, token);
                            editor.putString(USER_ID_KEY, userId);
                            editor.putString(USER_EMAIL_KEY, userEmail);
                            editor.putString(USER_ROLE_KEY, role);
                            editor.putBoolean(PROFILE_COMPLETED_KEY, profileCompleted);
                            editor.apply();

                            // Chuyển hướng
                            if (!profileCompleted) {
                                Toast.makeText(this, "Vui lòng cập nhật thông tin cá nhân của bạn.", Toast.LENGTH_LONG).show();
                                Intent updateProfileIntent = new Intent(LoginActivity.this, UpdateProfileActivity.class);
                                startActivity(updateProfileIntent);
                            } else {
                                if ("admin".equals(role)) {
                                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                } else {
                                    startActivity(new Intent(LoginActivity.this, EmployeeActivity.class));
                                }
                            }
                            finish();
                        } else {
                            String message = json.optString("message", "Sai tài khoản hoặc mật khẩu");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("Login", "Lỗi phân tích JSON: " + response, e);
                        Toast.makeText(this, "Lỗi xử lý dữ liệu phản hồi", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Bật lại nút nếu có lỗi
                    btnLogin.setEnabled(true);
                    handleVolleyError(error);
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                    return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                } catch (JSONException e) {
                    Log.e("Login", "Không thể tạo JSON body cho login", e);
                    return null;
                }
            }
        };

        requestQueue.add(request);
    }

    // SỬA ĐỔI: Thay đổi kiểu tham số từ Exception thành VolleyError
    private void handleVolleyError(VolleyError error) {
        String errorMessage = "Đã có lỗi xảy ra. Vui lòng thử lại.";
        if (error instanceof TimeoutError) {
            errorMessage = "Hết thời gian chờ kết nối server.";
        } else if (error instanceof NoConnectionError) {
            errorMessage = "Không có kết nối mạng. Vui lòng kiểm tra lại.";
        } else if (error instanceof ServerError || error instanceof AuthFailureError) {
            errorMessage = "Lỗi từ server hoặc xác thực thất bại.";
            // Bây giờ `error.networkResponse` có thể truy cập được
            if (error.networkResponse != null && error.networkResponse.data != null) {
                try {
                    String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    JSONObject data = new JSONObject(responseBody);
                    String serverMessage = data.optString("message");
                    if (!TextUtils.isEmpty(serverMessage)) {
                        errorMessage = serverMessage;
                    }
                    Log.e("LoginActivity", "Server Error Body: " + responseBody);
                } catch (Exception e) {
                    Log.e("LoginActivity", "Error parsing server error response", e);
                }
            }
        } else if (error instanceof ParseError) {
            errorMessage = "Lỗi phân tích dữ liệu từ server.";
        }
        Log.e("LoginActivity", "Lỗi Volley: " + error.toString());
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}
