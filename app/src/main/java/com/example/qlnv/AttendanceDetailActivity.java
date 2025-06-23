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

import com.android.volley.AuthFailureError;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

public class AttendanceDetailActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private Toolbar toolbar;

    private RequestQueue requestQueue;
    private String viewedUserId; // ID của người dùng mà Admin đang xem
    private String adminToken;   // Token của Admin để xác thực API
    private YearMonth currentMonth;

    private final Set<LocalDate> attendedDates = new HashSet<>();

    private static final String BASE_URL = "http://192.168.1.6:3000/api/attendance/"; // Thay IP nếu cần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_detail);

        toolbar = findViewById(R.id.toolbarAttendanceDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Lấy thông tin được truyền từ UserListActivity
        viewedUserId = getIntent().getStringExtra("USER_ID");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");

        // Đặt tiêu đề cho Toolbar
        if (getSupportActionBar() != null && userEmail != null) {
            getSupportActionBar().setTitle("Lịch điểm danh: " + userEmail);
        }

        // Lấy token của Admin đã đăng nhập để gọi API
        SharedPreferences prefs = getSharedPreferences("AUTH_PREFS", MODE_PRIVATE);
        adminToken = prefs.getString("AUTH_TOKEN", null);

        requestQueue = Volley.newRequestQueue(this);

        if (viewedUserId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nhân viên.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        calendarView = findViewById(R.id.calendarViewDetail);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

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
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

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
                        iconView.setImageDrawable(ContextCompat.getDrawable(AttendanceDetailActivity.this, R.drawable.ic_check_circle_green));
                        iconView.setVisibility(View.VISIBLE);
                    } else {
                        // Admin chỉ xem, không hiển thị nút hay dấu X cho ngày tương lai
                        // Có thể hiển thị dấu X cho ngày quá khứ mà không điểm danh
                        if (date.isBefore(LocalDate.now())) {
                            iconView.setImageDrawable(ContextCompat.getDrawable(AttendanceDetailActivity.this, R.drawable.ic_cancel_red));
                            iconView.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
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
        String url = BASE_URL + "history?userId=" + viewedUserId + "&year=" + year + "&month=" + month;
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
                        calendarView.notifyCalendarChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("AttendanceDetail", "Fetch history error: " + error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                if (adminToken != null) {
                    // Gửi token của Admin để xác thực quyền xem
                    headers.put("Authorization", "Bearer " + adminToken);
                }
                return headers;
            }
        };
        requestQueue.add(request);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Xử lý nút back trên Toolbar
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
