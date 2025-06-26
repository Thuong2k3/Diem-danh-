package com.example.qlnv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    private static final String LOG_TAG = "UserListActivity";

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;
    private RequestQueue requestQueue;
    private String purpose;
    private String authToken; // Biến để lưu token
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // Lấy Token từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE);
        authToken = prefs.getString(LoginActivity.TOKEN_KEY, null);

        // Lấy mục đích từ Intent
        purpose = getIntent().getStringExtra("PURPOSE");
        if (purpose == null) {
            purpose = "MANAGE_USERS"; // Mặc định
        }

        // Kiểm tra quyền truy cập (token)
        if (authToken == null) {
            Toast.makeText(this, "Yêu cầu đăng nhập để thực hiện chức năng này.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarUserList);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if ("VIEW_ATTENDANCE".equals(purpose)) {
                getSupportActionBar().setTitle("Chọn NV xem điểm danh");
            } else {
                getSupportActionBar().setTitle("Quản lý Người dùng");
            }
        }

        // Ánh xạ và thiết lập RecyclerView
        progressBar = findViewById(R.id.progressBarUserList); // Ánh xạ ProgressBar
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        // Giả định bạn đã có UserAdapter
        userAdapter = new UserAdapter(this, userList, this);
        recyclerViewUsers.setAdapter(userAdapter);

        requestQueue = Volley.newRequestQueue(this);

        fetchUsersFromServer();
    }

    @Override
    public void onUserClick(User user) {
        if ("VIEW_ATTENDANCE".equals(purpose)) {
            // Giả định bạn đã có AttendanceDetailActivity
            Intent intent = new Intent(UserListActivity.this, AttendanceDetailActivity.class);
            intent.putExtra("USER_ID", user.getId());
            intent.putExtra("USER_NAME", user.getFullName());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Đã chọn quản lý: " + user.getFullName(), Toast.LENGTH_SHORT).show();
            // Nơi để implement logic sửa/xóa user sau này
        }
    }

    private void fetchUsersFromServer() {
        progressBar.setVisibility(View.VISIBLE);

        // Sử dụng ApiConfig để lấy URL động
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/users"; // Route để lấy danh sách người dùng

        Log.d(LOG_TAG, "Fetching users from: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        // Backend trả về một object có key "users" chứa mảng
                        JSONArray usersArray = response.getJSONArray("users");
                        userList.clear();
                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject userObject = usersArray.getJSONObject(i);
                            // Giả định bạn đã có class User
                            User user = new User();
                            user.setId(userObject.getString("_id"));
                            user.setEmail(userObject.getString("email"));
                            user.setRole(userObject.optString("role", "employee"));
                            user.setFullName(userObject.optString("fullName", "Chưa cập nhật"));
                            user.setGender(userObject.optString("gender", ""));
                            user.setDateOfBirth(userObject.optString("dateOfBirth", ""));
                            user.setHometown(userObject.optString("hometown", ""));
                            user.setPhoneNumber(userObject.optString("phoneNumber", ""));
                            user.setProfileCompleted(userObject.optBoolean("profileCompleted", false));
                            userList.add(user);
                        }
                        // Cập nhật adapter với dữ liệu mới
                        userAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(UserListActivity.this, "Lỗi phân tích dữ liệu server", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    handleVolleyError(error);
                }) {
            // Gửi token trong header để xác thực
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authToken);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    private void handleVolleyError(Exception error) {
        String errorMessage = "Đã có lỗi xảy ra. Vui lòng thử lại.";
        if (error instanceof TimeoutError) {
            errorMessage = "Hết thời gian chờ kết nối server.";
        } else if (error instanceof NoConnectionError) {
            errorMessage = "Không có kết nối mạng. Vui lòng kiểm tra lại.";
        } else if (error instanceof ServerError || error instanceof AuthFailureError) {
            errorMessage = "Lỗi từ server hoặc bạn không có quyền truy cập.";
            if (((com.android.volley.VolleyError) error).networkResponse != null && ((com.android.volley.VolleyError) error).networkResponse.data != null) {
                try {
                    String responseBody = new String(((com.android.volley.VolleyError) error).networkResponse.data, StandardCharsets.UTF_8);
                    JSONObject data = new JSONObject(responseBody);
                    String serverMessage = data.optString("message");
                    if (!TextUtils.isEmpty(serverMessage)) {
                        errorMessage = serverMessage;
                    }
                    Log.e(LOG_TAG, "Server Error Body: " + responseBody);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error parsing server error response", e);
                }
            }
        } else if (error instanceof ParseError) {
            errorMessage = "Lỗi phân tích dữ liệu từ server.";
        }
        Log.e(LOG_TAG, "Lỗi Volley: " + error.toString());
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
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