package com.example.qlnv;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class UpdateProfileActivity extends AppCompatActivity {

    private static final String LOG_TAG = "UpdateProfileActivity";

    private EditText etFullName, etDateOfBirth, etPhoneNumber;
    private Spinner spinnerHometown;
    private RadioGroup radioGroupGender;
    private RadioButton rbMale, rbFemale;
    private Button btnSaveProfile;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    private String token;
    private Calendar selectedDateCalendar;

    private static final String GENDER_MALE = "male";
    private static final String GENDER_FEMALE = "female";

    // Định dạng ngày tháng để hiển thị và gửi lên server
    private final SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat serverSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        Toolbar toolbar = findViewById(R.id.toolbarUpdateProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cập nhật thông tin");
        }

        // Ánh xạ views
        etFullName = findViewById(R.id.etFullName);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        spinnerHometown = findViewById(R.id.spinnerHometown);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBarUpdateProfile);
        selectedDateCalendar = Calendar.getInstance();
        requestQueue = Volley.newRequestQueue(this);

        // Thiết lập Spinner quê quán
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.vietnam_provinces, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHometown.setAdapter(adapter);

        // Lấy token đã lưu
        SharedPreferences prefs = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE);
        token = prefs.getString(LoginActivity.TOKEN_KEY, null);

        if (token == null) {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog());
        btnSaveProfile.setOnClickListener(v -> {
            if (validateInput()) {
                updateProfile();
            }
        });

        fetchCurrentUserProfile();
    }

    private void fetchCurrentUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/profile"; // GET /api/auth/profile

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        // SỬA Ở ĐÂY: Backend giờ trả về trực tiếp đối tượng user, không có key "user"
                        // JSONObject user = response.getJSONObject("user"); (Dòng cũ)
                        populateUIWithUserData(response); // Dùng trực tiếp response
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing profile response", e);
                        Toast.makeText(this, "Lỗi phân tích dữ liệu cá nhân.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(LOG_TAG, "Error fetching profile: " + error.toString());
                    // Thêm xử lý lỗi chi tiết hơn
                    String errorMessage = "Lỗi tải thông tin cá nhân.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject data = new JSONObject(responseBody);
                            errorMessage = data.optString("message", errorMessage);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error parsing error response", e);
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        requestQueue.add(request);
    }

    private void populateUIWithUserData(JSONObject user) {
        etFullName.setText(user.optString("fullName", ""));
        etPhoneNumber.setText(user.optString("phoneNumber", ""));

        // Quê quán
        String hometown = user.optString("hometown");
        if (!TextUtils.isEmpty(hometown)) {
            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerHometown.getAdapter();
            int position = adapter.getPosition(hometown);
            if (position >= 0) {
                spinnerHometown.setSelection(position);
            }
        }

        // Giới tính
        String gender = user.optString("gender");
        if (GENDER_MALE.equalsIgnoreCase(gender)) {
            rbMale.setChecked(true);
        } else if (GENDER_FEMALE.equalsIgnoreCase(gender)) {
            rbFemale.setChecked(true);
        }

        // Ngày sinh
        String dobString = user.optString("dateOfBirth");
        if (!TextUtils.isEmpty(dobString) && !dobString.equalsIgnoreCase("null")) {
            // Thử định dạng chuẩn ISO 8601 trước
            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            try {
                Date date = serverFormat.parse(dobString);
                if (date != null) {
                    selectedDateCalendar.setTime(date);
                    etDateOfBirth.setText(displaySdf.format(date));
                }
            } catch (ParseException e) {
                // Nếu thất bại, thử định dạng yyyy-MM-dd
                try {
                    Date date = serverSdf.parse(dobString);
                    if (date != null) {
                        selectedDateCalendar.setTime(date);
                        etDateOfBirth.setText(displaySdf.format(date));
                    }
                } catch (ParseException e2) {
                    Log.e(LOG_TAG, "Không thể phân tích ngày sinh: " + dobString, e2);
                }
            }
        }
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(etFullName.getText().toString().trim())) {
            etFullName.setError("Họ tên không được để trống");
            etFullName.requestFocus();
            return false;
        }
        if (radioGroupGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(etDateOfBirth.getText().toString().trim())) {
            Toast.makeText(this, "Vui lòng chọn ngày sinh", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Calendar today = Calendar.getInstance();
            Calendar dob = (Calendar) selectedDateCalendar.clone();
            dob.add(Calendar.YEAR, 18);
            if (dob.after(today)) {
                Toast.makeText(this, "Bạn phải đủ 18 tuổi để đăng ký.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        if (spinnerHometown.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Vui lòng chọn quê quán", Toast.LENGTH_SHORT).show();
            return false;
        }
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        if (!Pattern.compile("^0[0-9]{9}$").matcher(phoneNumber).matches()) {
            etPhoneNumber.setError("Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng 0)");
            etPhoneNumber.requestFocus();
            return false;
        }
        return true;
    }

    private void showDatePickerDialog() {
        int year = selectedDateCalendar.get(Calendar.YEAR);
        int month = selectedDateCalendar.get(Calendar.MONTH);
        int day = selectedDateCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    selectedDateCalendar.set(yearSelected, monthOfYear, dayOfMonth);
                    etDateOfBirth.setText(displaySdf.format(selectedDateCalendar.getTime()));
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateProfile() {
        progressBar.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        String fullName = etFullName.getText().toString().trim();
        String hometown = spinnerHometown.getSelectedItem().toString();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String selectedGender = rbMale.isChecked() ? GENDER_MALE : GENDER_FEMALE;
        String dateOfBirthForServer = serverSdf.format(selectedDateCalendar.getTime());

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("fullName", fullName);
            requestBody.put("gender", selectedGender);
            requestBody.put("dateOfBirth", dateOfBirthForServer);
            requestBody.put("hometown", hometown);
            requestBody.put("phoneNumber", phoneNumber);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error creating JSON body", e);
            progressBar.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
            return;
        }

        // SỬA ĐỔI: Lấy URL động và gọi đúng route
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "auth/profile"; // PUT /api/auth/profile

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestBody,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UpdateProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_LONG).show();

                    // Cập nhật trạng thái đã hoàn thành profile
                    SharedPreferences prefs = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(LoginActivity.PROFILE_COMPLETED_KEY, true);
                    editor.apply();

                    String role = prefs.getString(LoginActivity.USER_ROLE_KEY, "employee");

                    Intent intent = "admin".equals(role) ?
                            new Intent(UpdateProfileActivity.this, AdminActivity.class) :
                            new Intent(UpdateProfileActivity.this, EmployeeActivity.class);

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    String errorMessage = "Lỗi cập nhật thông tin";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            errorMessage = new JSONObject(responseBody).optString("message", errorMessage);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error parsing server error", e);
                        }
                    }
                    Toast.makeText(UpdateProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
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
