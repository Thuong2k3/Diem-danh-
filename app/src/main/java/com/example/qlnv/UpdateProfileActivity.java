package com.example.qlnv;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class UpdateProfileActivity extends AppCompatActivity {

    private EditText etFullName, etDateOfBirth, etPhoneNumber;
    private Spinner spinnerHometown;
    private RadioGroup radioGroupGender;
    private RadioButton rbMale, rbFemale;
    private Button btnSaveProfile;
    private RequestQueue requestQueue;
    private Toolbar toolbar;

    private String currentUserId;
    private String token;
    private Calendar selectedDateCalendar;

    private static final String GENDER_MALE = "male";
    private static final String GENDER_FEMALE = "female";

    private static final String PROFILE_BASE_URL = "http://192.168.1.6:3000/api"; // Thay IP nếu cần
    private static final String GET_PROFILE_URL = PROFILE_BASE_URL + "/profile";
    private static final String UPDATE_PROFILE_URL = PROFILE_BASE_URL + "/profile/update";

    private final SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat serverSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        toolbar = findViewById(R.id.toolbarUpdateProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etFullName = findViewById(R.id.etFullName);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        spinnerHometown = findViewById(R.id.spinnerHometown);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        selectedDateCalendar = Calendar.getInstance();

        requestQueue = Volley.newRequestQueue(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.vietnam_provinces, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHometown.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", null);
        token = prefs.getString("AUTH_TOKEN", null);

        if (currentUserId == null) {
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
        String url = GET_PROFILE_URL + "?userIdForProfile=" + currentUserId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if ("success".equals(response.optString("status"))) {
                            JSONObject userObject = response.getJSONObject("user");
                            populateUIWithUserData(userObject);
                        } else {
                            Toast.makeText(this, "Không thể tải thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("UpdateProfile", "Error parsing profile response", e);
                    }
                },
                error -> {
                    Log.e("UpdateProfile", "Error fetching profile: " + error.toString());
                    Toast.makeText(this, "Lỗi tải thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };
        requestQueue.add(request);
    }

    private void populateUIWithUserData(JSONObject user) {
        // --- SỬA LẠI LOGIC Ở ĐÂY ---
        // Đối với mỗi trường, kiểm tra xem nó có null hay không trước khi hiển thị

        // Họ tên
        if (user.has("fullName") && !user.isNull("fullName")) {
            etFullName.setText(user.optString("fullName"));
        } else {
            etFullName.setText(""); // Để trống nếu là null
        }

        // Quê quán
        String hometown = user.optString("hometown", null);
        if (hometown != null) {
            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerHometown.getAdapter();
            int position = adapter.getPosition(hometown);
            if (position >= 0) {
                spinnerHometown.setSelection(position);
            }
        }

        // Số điện thoại
        if (user.has("phoneNumber") && !user.isNull("phoneNumber")) {
            etPhoneNumber.setText(user.optString("phoneNumber"));
        } else {
            etPhoneNumber.setText("");
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
        if (dobString != null && !dobString.isEmpty() && !dobString.equalsIgnoreCase("null")) {
            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            try {
                Date date = serverFormat.parse(dobString);
                if (date != null) {
                    selectedDateCalendar.setTime(date);
                    etDateOfBirth.setText(displaySdf.format(date));
                }
            } catch (ParseException e) {
                Log.e("UpdateProfile", "Error parsing DOB from server: " + dobString, e);
            }
        }
    }

    private boolean validateInput() {
        if (etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("Họ tên không được để trống");
            etFullName.requestFocus();
            return false;
        }
        if (radioGroupGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDateOfBirth.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày sinh", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Calendar today = Calendar.getInstance();
            Calendar dob = (Calendar) selectedDateCalendar.clone();
            dob.add(Calendar.YEAR, 18); // Thêm 18 năm vào ngày sinh
            if (dob.after(today)) { // Nếu ngày sinh + 18 năm sau ngày hiện tại -> chưa đủ 18 tuổi
                Toast.makeText(this, "Bạn phải đủ 18 tuổi để đăng ký.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        if (spinnerHometown.getSelectedItemPosition() == 0) { // Item đầu tiên là "-- Chọn Tỉnh/Thành phố --"
            Toast.makeText(this, "Vui lòng chọn quê quán", Toast.LENGTH_SHORT).show();
            return false;
        }
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            etPhoneNumber.setError("Số điện thoại không được để trống");
            etPhoneNumber.requestFocus();
            return false;
        }
        // Pattern cho SĐT Việt Nam: bắt đầu bằng 0, theo sau là 9 chữ số.
        Pattern phonePattern = Pattern.compile("^0[0-9]{9}$");
        if (!phonePattern.matcher(phoneNumber).matches()) {
            etPhoneNumber.setError("Số điện thoại không hợp lệ");
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
        String fullName = etFullName.getText().toString().trim();
        String hometown = spinnerHometown.getSelectedItem().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        String selectedGender = null;
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        if (selectedGenderId == R.id.rbMale) {
            selectedGender = GENDER_MALE;
        } else if (selectedGenderId == R.id.rbFemale) {
            selectedGender = GENDER_FEMALE;
        }

        String dateOfBirthForServer = null;
        if (etDateOfBirth.getText().length() > 0) {
            dateOfBirthForServer = serverSdf.format(selectedDateCalendar.getTime());
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", currentUserId);
            requestBody.put("fullName", fullName);
            requestBody.put("gender", selectedGender);
            requestBody.put("dateOfBirth", dateOfBirthForServer);
            requestBody.put("hometown", hometown);
            requestBody.put("phoneNumber", phoneNumber);
        } catch (JSONException e) {
            Log.e("UpdateProfile", "Error creating JSON body", e);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, UPDATE_PROFILE_URL, requestBody,
                response -> {
                    try {
                        String status = response.getString("status");
                        String message = response.optString("message", "Cập nhật thành công!");
                        if ("success".equals(status)) {
                            Toast.makeText(UpdateProfileActivity.this, message, Toast.LENGTH_LONG).show();

                            SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("PROFILE_COMPLETED", true);
                            editor.apply();

                            String role = prefs.getString("USER_ROLE", "employee");

                            Intent intent;
                            if ("admin".equals(role)) {
                                intent = new Intent(UpdateProfileActivity.this, AdminActivity.class);
                            } else {
                                intent = new Intent(UpdateProfileActivity.this, EmployeeActivity.class);
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(UpdateProfileActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("UpdateProfile", "Error parsing success response", e);
                    }
                },
                error -> {
                    String errorMessage = "Lỗi cập nhật thông tin";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            errorMessage = data.optString("message", errorMessage);
                        } catch (Exception e) {
                            Log.e("UpdateProfile", "Error parsing server error: " + e.getMessage());
                        }
                    } else if (error instanceof NoConnectionError) {
                        errorMessage = "Không có kết nối mạng.";
                    } else if (error instanceof TimeoutError) {
                        errorMessage = "Hết thời gian chờ kết nối server.";
                    }
                    Toast.makeText(UpdateProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
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

