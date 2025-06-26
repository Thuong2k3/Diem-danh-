package com.example.qlnv;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

public class AttendanceActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AttendanceActivity";

    // XÓA BỎ URL CỨNG
    // private static final String BASE_URL = "http://10.0.2.2:3000/api/attendance/";

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private Button btnDoCheckIn;

    private RequestQueue requestQueue;
    private String userId;
    private YearMonth currentMonth;
    private final LocalDate today = LocalDate.now();

    private final Set<LocalDate> attendedDates = new HashSet<>();
    private boolean hasCheckedInToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        Toolbar toolbar = findViewById(R.id.toolbarAttendance);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử điểm danh");
        }

        // Sử dụng hằng số từ LoginActivity để đảm bảo tính nhất quán
        SharedPreferences prefs = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE);
        userId = prefs.getString(LoginActivity.USER_ID_KEY, null);

        requestQueue = Volley.newRequestQueue(this);

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        ImageButton btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        ImageButton btnNextMonth = findViewById(R.id.btnNextMonth);
        btnDoCheckIn = findViewById(R.id.btnDoCheckIn);

        btnPreviousMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));
        btnDoCheckIn.setOnClickListener(v -> performCheckIn());

        setupCalendar();
        // Tải lịch sử lần đầu cho tháng hiện tại
        fetchAttendanceHistory(YearMonth.now().getYear(), YearMonth.now().getMonthValue());
    }

    private void changeMonth(long months) {
        currentMonth = currentMonth.plusMonths(months);
        calendarView.smoothScrollToMonth(currentMonth);
    }

    private void setupCalendar() {
        currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);
        DayOfWeek firstDayOfWeek = DayOfWeek.SUNDAY;

        calendarView.setup(startMonth, endMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);

        class DayViewContainer extends ViewContainer {
            final TextView textView;
            final ImageView iconView;
            public DayViewContainer(@NonNull View view) {
                super(view);
                textView = view.findViewById(R.id.calendarDayText);
                iconView = view.findViewById(R.id.calendarDayIcon);
            }
        }

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull @Override
            public DayViewContainer create(@NonNull View view) { return new DayViewContainer(view); }
            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                TextView textView = container.textView;
                ImageView iconView = container.iconView;
                textView.setText(String.valueOf(day.getDate().getDayOfMonth()));
                iconView.setVisibility(View.GONE);

                if (day.getPosition() == DayPosition.MonthDate) {
                    textView.setTextColor(Color.BLACK);
                    LocalDate date = day.getDate();
                    if (attendedDates.contains(date)) {
                        iconView.setImageResource(R.drawable.ic_check_circle_green);
                        iconView.setVisibility(View.VISIBLE);
                    } else if (date.isBefore(today)) {
                        iconView.setImageResource(R.drawable.ic_cancel_red);
                        iconView.setVisibility(View.VISIBLE);
                    }
                } else {
                    textView.setTextColor(Color.GRAY);
                }
            }
        });

        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth = calendarMonth.getYearMonth();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("vi"));
            tvMonthYear.setText(currentMonth.format(formatter));
            fetchAttendanceHistory(currentMonth.getYear(), currentMonth.getMonthValue());
            return null;
        });
    }

    private void fetchAttendanceHistory(int year, int month) {
        // SỬA ĐỔI: Lấy URL động từ ApiConfig
        String baseUrl = ApiConfig.getBaseUrl(this);
        // Route này được lấy từ `user-auth-api/routes/attendance.js`
        String url = baseUrl + "attendance/history/" + userId + "/" + year + "/" + month;

        // SỬA ĐỔI QUAN TRỌNG: Dùng JsonArrayRequest vì backend trả về một mảng JSON
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        attendedDates.clear();
                        for (int i = 0; i < response.length(); i++) {
                            // Backend trả về mảng các chuỗi ngày tháng "YYYY-MM-DD"
                            String dateStr = response.getString(i);
                            attendedDates.add(LocalDate.parse(dateStr));
                        }
                        hasCheckedInToday = attendedDates.contains(today);
                        updateCheckInButtonState();
                        calendarView.notifyCalendarChanged();
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Lỗi phân tích JSON từ mảng lịch sử", e);
                        Toast.makeText(this, "Lỗi phân tích dữ liệu lịch sử", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(LOG_TAG, "Lỗi tải lịch sử điểm danh: " + error.toString());
                    // Có thể thêm xử lý lỗi chi tiết hơn nếu muốn
                    Toast.makeText(this, "Không thể tải lịch sử điểm danh", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void performCheckIn() {
        btnDoCheckIn.setEnabled(false);

        // SỬA ĐỔI: Lấy URL động
        String baseUrl = ApiConfig.getBaseUrl(this);
        // Route điểm danh là POST /api/attendance/ (dựa theo attendance.js)
        String url = baseUrl + "attendance/";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
        } catch (JSONException e) {
            e.printStackTrace();
            btnDoCheckIn.setEnabled(true);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    // Logic đúng: Tải lại lịch sử để đảm bảo đồng bộ
                    Toast.makeText(this, "Điểm danh thành công!", Toast.LENGTH_SHORT).show();
                    fetchAttendanceHistory(currentMonth.getYear(), currentMonth.getMonthValue());
                },
                error -> {
                    String message = "Lỗi khi điểm danh.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("message", message);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Lỗi phân tích phản hồi lỗi", e);
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    btnDoCheckIn.setEnabled(true);
                });

        requestQueue.add(request);
    }

    private void updateCheckInButtonState() {
        if (hasCheckedInToday) {
            btnDoCheckIn.setVisibility(View.GONE);
        } else {
            btnDoCheckIn.setVisibility(View.VISIBLE);
            btnDoCheckIn.setEnabled(true);
        }
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
