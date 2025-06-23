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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

public class AttendanceActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private Button btnDoCheckIn; // Thêm nút điểm danh mới

    private RequestQueue requestQueue;
    private String userId;
    private YearMonth currentMonth;
    private final LocalDate today = LocalDate.now();

    private final Set<LocalDate> attendedDates = new HashSet<>();
    private boolean hasCheckedInToday = false;

    private static final String BASE_URL = "http://192.168.1.6/api/attendance/"; // Thay IP nếu cần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        Toolbar toolbar = findViewById(R.id.toolbarAttendance);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);

        requestQueue = Volley.newRequestQueue(this);

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnDoCheckIn = findViewById(R.id.btnDoCheckIn); // Ánh xạ nút điểm danh mới

        setupCalendar();

        btnPreviousMonth.setOnClickListener(v -> {
            if (calendarView.findFirstVisibleMonth() != null) {
                currentMonth = calendarView.findFirstVisibleMonth().getYearMonth().minusMonths(1);
                calendarView.smoothScrollToMonth(currentMonth);
            }
        });

        btnNextMonth.setOnClickListener(v -> {
            if (calendarView.findFirstVisibleMonth() != null) {
                currentMonth = calendarView.findFirstVisibleMonth().getYearMonth().plusMonths(1);
                calendarView.smoothScrollToMonth(currentMonth);
            }
        });

        btnDoCheckIn.setOnClickListener(v -> performCheckIn());
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
            public CalendarDay day;
            // Nút điểm danh trong ô ngày đã được xóa

            public DayViewContainer(@NonNull View view) {
                super(view);
                textView = view.findViewById(R.id.calendarDayText);
                iconView = view.findViewById(R.id.calendarDayIcon);
            }
        }

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                container.day = day;
                TextView textView = container.textView;
                ImageView iconView = container.iconView;

                textView.setText(String.valueOf(day.getDate().getDayOfMonth()));

                iconView.setVisibility(View.GONE);

                // --- SỬA LẠI LOGIC MÀU CHỮ Ở ĐÂY ---
                if (day.getPosition() == DayPosition.MonthDate) {
                    // Sử dụng màu mặc định của hệ thống cho văn bản chính
                    textView.setTextColor(ContextCompat.getColor(AttendanceActivity.this, android.R.color.tab_indicator_text));

                    LocalDate date = day.getDate();
                    if (attendedDates.contains(date)) {
                        iconView.setImageDrawable(ContextCompat.getDrawable(AttendanceActivity.this, R.drawable.ic_check_circle_green));
                        iconView.setVisibility(View.VISIBLE);
                    } else if (date.isBefore(today)) {
                        iconView.setImageDrawable(ContextCompat.getDrawable(AttendanceActivity.this, R.drawable.ic_cancel_red));
                        iconView.setVisibility(View.VISIBLE);
                    } else if (date.isEqual(today)) {
                        if (hasCheckedInToday) {
                            iconView.setImageDrawable(ContextCompat.getDrawable(AttendanceActivity.this, R.drawable.ic_check_circle_green));
                            iconView.setVisibility(View.VISIBLE);
                        } else {
                            // Không hiển thị nút điểm danh trong ô lịch nữa
                        }
                    }
                } else { // Ngày của tháng trước/sau
                    // Sử dụng màu xám mờ cho các ngày không thuộc tháng hiện tại
                    textView.setTextColor(Color.GRAY);
                }
            }
        });

        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth = calendarMonth.getYearMonth();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM uuuu", new Locale("vi"));
            tvMonthYear.setText(currentMonth.format(formatter));
            fetchAttendanceHistory(currentMonth.getYear(), currentMonth.getMonthValue());
            return null;
        });
    }

    private void fetchAttendanceHistory(int year, int month) {
        String url = BASE_URL + "history?userId=" + userId + "&year=" + year + "&month=" + month;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        attendedDates.clear();
                        if (response.has("history")) {
                            JSONArray history = response.getJSONArray("history");
                            for (int i = 0; i < history.length(); i++) {
                                JSONObject record = history.getJSONObject(i);
                                String status = record.optString("status", "present");
                                String dateStr = record.optString("date");
                                if ("present".equalsIgnoreCase(status) && !dateStr.isEmpty()) {
                                    attendedDates.add(LocalDate.parse(dateStr));
                                }
                            }
                        }
                        hasCheckedInToday = attendedDates.contains(today);

                        // CẬP NHẬT TRẠNG THÁI NÚT ĐIỂM DANH
                        if (hasCheckedInToday) {
                            btnDoCheckIn.setVisibility(View.GONE);
                        } else {
                            btnDoCheckIn.setVisibility(View.VISIBLE);
                        }

                        calendarView.notifyCalendarChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("AttendanceActivity", "Fetch history error: " + error.toString()));
        requestQueue.add(request);
    }

    private void performCheckIn() {
        String url = BASE_URL + "check-in";
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    Toast.makeText(this, "Điểm danh thành công!", Toast.LENGTH_SHORT).show();
                    attendedDates.add(today);
                    hasCheckedInToday = true;
                    calendarView.notifyDateChanged(today); // Cập nhật ngày hôm nay trên lịch
                    btnDoCheckIn.setVisibility(View.GONE); // Ẩn nút sau khi điểm danh
                },
                error -> {
                    String message = "Lỗi khi điểm danh.";
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
