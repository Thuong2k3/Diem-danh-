package com.example.qlnv;

import android.content.Intent;
import android.content.SharedPreferences; // Thêm import này
import android.os.Bundle;
import android.util.Log;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private TextView tvForgotPassword;
    // Đảm bảo URL này đúng, có port :3000 và trỏ đến IP chính xác của server
    private static final String LOGIN_URL = "http://192.168.1.6:3000/api/login"; // << KIỂM TRA LẠI PREFIX /api/auth NẾU BẠN TÁCH FILE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

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
        StringRequest request = new StringRequest(Request.Method.POST, LOGIN_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String status = json.optString("status");

                        if ("success".equals(status)) {
                            // --- BẮT ĐẦU PHẦN SỬA ĐỔI LOGIC ---

                            String role = json.optString("role");
                            boolean profileCompleted = json.optBoolean("profileCompleted", false); // Lấy giá trị profileCompleted
                            String token = json.optString("token");
                            String userId = "";
                            String userEmail = "";

                            if (json.has("user")) {
                                JSONObject userObject = json.getJSONObject("user");
                                userId = userObject.optString("id");
                                userEmail = userObject.optString("email");
                            }

                            // Lưu thông tin đăng nhập vào SharedPreferences để dùng ở các màn hình khác
                            SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("AUTH_TOKEN", token);
                            editor.putString("USER_ID", userId);
                            editor.putString("USER_EMAIL", userEmail);
                            editor.putString("USER_ROLE", role);
                            editor.putBoolean("PROFILE_COMPLETED", profileCompleted);
                            editor.apply();

                            // Kiểm tra xem đã hoàn thành profile chưa để chuyển hướng
                            if (!profileCompleted) {
                                // Nếu chưa hoàn thành profile, chuyển đến màn hình cập nhật
                                Toast.makeText(this, "Vui lòng cập nhật thông tin cá nhân của bạn.", Toast.LENGTH_LONG).show();
                                Intent updateProfileIntent = new Intent(LoginActivity.this, UpdateProfileActivity.class);
                                // Truyền thông tin cần thiết sang UpdateProfileActivity
                                updateProfileIntent.putExtra("USER_ID", userId);

                                // Dùng email để server định danh nếu chưa có JWT hoàn chỉnh
                                // (Sau này khi có JWT, chỉ cần truyền token trong header là đủ)
                                updateProfileIntent.putExtra("USER_EMAIL", userEmail);
                                startActivity(updateProfileIntent);
                            } else {
                                // Nếu đã hoàn thành, chuyển đến màn hình chính theo vai trò
                                if ("admin".equals(role)) {
                                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                } else {
                                    startActivity(new Intent(LoginActivity.this, EmployeeActivity.class));
                                }
                            }
                            finish(); // Đóng LoginActivity sau khi chuyển hướng thành công

                            // --- KẾT THÚC PHẦN SỬA ĐỔI LOGIC ---

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
                    // ... (phần xử lý lỗi giữ nguyên như cũ, đã khá tốt) ...
                    String errorMessage = "Lỗi kết nối đến server.";
                    if (error instanceof TimeoutError) {
                        errorMessage = "Hết thời gian chờ kết nối server.";
                    } else if (error instanceof NoConnectionError) {
                        errorMessage = "Không có kết nối mạng.";
                    } else if (error instanceof ServerError || error instanceof AuthFailureError) {
                        // Gộp ServerError và AuthFailureError vì cả hai đều có thể chứa response body
                        errorMessage = "Server gặp sự cố hoặc xác thực thất bại."; // Thông báo mặc định
                        if (error.networkResponse != null) {
                            Log.e("Login", "Server/Auth Error Status code: " + error.networkResponse.statusCode);
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                JSONObject data = new JSONObject(responseBody);
                                String serverMessage = data.optString("message");
                                if (serverMessage != null && !serverMessage.isEmpty()) {
                                    errorMessage = serverMessage; // Hiển thị lỗi cụ thể từ server
                                }
                                Log.e("Login", "Server/Auth Error Response Body: " + responseBody);
                            } catch (Exception e) {
                                Log.e("Login", "Error parsing server error response", e);
                            }
                        }
                    } else if (error instanceof NetworkError) {
                        errorMessage = "Lỗi kết nối mạng.";
                    } else if (error instanceof ParseError) {
                        errorMessage = "Lỗi phân tích dữ liệu phản hồi.";
                    }
                    Log.e("Login", "Lỗi Volley: " + error.toString(), error);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                    final String requestBody = jsonBody.toString();
                    return requestBody.getBytes("utf-8");
                } catch (JSONException | java.io.UnsupportedEncodingException e) {
                    Log.e("Login", "Không thể tạo JSON body cho login", e);
                    return null;
                }
            }
        };

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                20000,
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}