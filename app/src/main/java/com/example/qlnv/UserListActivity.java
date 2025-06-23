package com.example.qlnv;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.content.Intent;

// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< 1. IMPLEMENT INTERFACE CỦA ADAPTER >>>>>>>>>>>>>>>>>>>>>>>>>>>>>
public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;
    private RequestQueue requestQueue;
    private String purpose;
    private static final String GET_USERS_URL = "http://192.168.1.6:3000/api/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // Lấy mục đích từ Intent để biết là xem điểm danh hay quản lý
        purpose = getIntent().getStringExtra("PURPOSE");
        if (purpose == null) {
            purpose = "MANAGE_USERS"; // Mặc định là quản lý
        }

        Toolbar toolbar = findViewById(R.id.toolbarUserList);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Bạn có thể thêm logic thay đổi tiêu đề ở đây nếu muốn
            if ("VIEW_ATTENDANCE".equals(purpose)) {
                getSupportActionBar().setTitle("Chọn NV xem điểm danh");
            } else {
                getSupportActionBar().setTitle("Quản lý Người dùng");
            }
        }

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();

        // <<<<<<<<<<<<<<<<<<<<<<<<<<< 2. SỬA LẠI DÒNG NÀY ĐỂ TRUYỀN LISTENER >>>>>>>>>>>>>>>>>>>>>
        // Truyền "this" vì Activity này đã implement OnUserClickListener
        userAdapter = new UserAdapter(this, userList, this);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        recyclerViewUsers.setAdapter(userAdapter);

        requestQueue = Volley.newRequestQueue(this);

        fetchUsersFromServer();
    }

    // <<<<<<<<<<<<<<<<<<<<<< 3. THÊM PHƯƠNG THỨC NÀY ĐỂ XỬ LÝ SỰ KIỆN CLICK >>>>>>>>>>>>>>>>>>>>>
    @Override
    public void onUserClick(User user) {
        // Đây là nơi xử lý logic khi một người dùng được click

        // Lấy lại logic cũ của bạn
        if ("VIEW_ATTENDANCE".equals(purpose)) {
            // Nếu mục đích là xem điểm danh, mở màn hình chi tiết điểm danh
            Intent intent = new Intent(UserListActivity.this, AttendanceDetailActivity.class);
            intent.putExtra("USER_ID", user.getId());
            intent.putExtra("USER_EMAIL", user.getEmail());
            startActivity(intent);
        } else {
            // Nếu là mục đích khác (quản lý), có thể mở màn hình chi tiết người dùng
            // Tạm thời hiển thị Toast
            Toast.makeText(this, "Đã chọn: " + user.getFullName(), Toast.LENGTH_SHORT).show();

            // Ví dụ: Mở màn hình chi tiết người dùng (nếu có)
            // Intent intent = new Intent(UserListActivity.this, UserDetailActivity.class);
            // intent.putExtra("USER_ID", user.getId());
            // startActivity(intent);
        }
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- PHẦN fetchUsersFromServer() GIỮ NGUYÊN, KHÔNG CẦN THAY ĐỔI ---
    private void fetchUsersFromServer() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, GET_USERS_URL, null,
                response -> {
                    try {
                        String status = response.optString("status");
                        if ("success".equals(status)) {
                            JSONArray usersArray = response.getJSONArray("users");
                            userList.clear();
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObject = usersArray.getJSONObject(i);
                                User user = new User();

                                user.setId(userObject.optString("_id"));
                                user.setEmail(userObject.optString("email"));
                                user.setRole(userObject.optString("role"));
                                user.setFullName(userObject.optString("fullName", null));
                                user.setGender(userObject.optString("gender", null));
                                user.setDateOfBirth(userObject.optString("dateOfBirth", null));
                                user.setHometown(userObject.optString("hometown", null));
                                user.setPhoneNumber(userObject.optString("phoneNumber", null));
                                user.setProfileCompleted(userObject.optBoolean("profileCompleted"));

                                userList.add(user);
                            }
                            userAdapter.setUserList(userList);
                        } else {
                            String message = response.optString("message", "Failed to fetch users");
                            Toast.makeText(UserListActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("UserListActivity", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(UserListActivity.this, "Error parsing server response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("UserListActivity", "Volley error: " + error.toString());
                    String errorMessage = "Error fetching users from server";
                    if (error.networkResponse != null) {
                        Log.e("UserListActivity", "Error status code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            String serverMessage = data.optString("message");
                            if (serverMessage != null && !serverMessage.isEmpty()) {
                                errorMessage = serverMessage;
                            }
                        } catch (Exception e) {
                            // Không làm gì thêm
                        }
                    } else if (error instanceof com.android.volley.NoConnectionError) {
                        errorMessage = "No internet connection";
                    } else if (error instanceof com.android.volley.TimeoutError) {
                        errorMessage = "Connection timed out";
                    }
                    Toast.makeText(UserListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return super.getHeaders(); // Tạm thời
            }
        };
        jsonObjectRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                20000,
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonObjectRequest);
    }
}