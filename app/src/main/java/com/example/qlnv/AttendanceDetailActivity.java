package com.example.qlnv;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AttendanceDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AttendanceDetail";

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private ProgressBar progressBar;

    private RequestQueue requestQueue;
    private String viewedUserId; // ID của người dùng mà Admin đang xem
    private String adminToken;   // Token của Admin để xác thực API
    private YearMonth currentMonth;

    private final Set<LocalDate> attendedDates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_detail);

        // Lấy thông tin được truyền từ UserListActivity
        viewedUserId = getIntent().getStringExtra("USER_ID");
        String userName = getIntent().getStringExtra("USER_NAME");

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarAttendanceDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Đặt tiêu đề là tên của nhân viên đang xem
            getSupportActionBar().setTitle("Lịch sử của: " + (userName != null ? userName : "Không rõ"));
        }

        // Lấy token của Admin đã đăng nhập để gọi API
        SharedPreferences prefs = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE);
        adminToken = prefs.getString(LoginActivity.TOKEN_KEY, null);

        // Kiểm tra thông tin cần thiết
        if (viewedUserId == null || adminToken == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin người dùng hoặc quyền truy cập.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        requestQueue = Volley.newRequestQueue(this);

        // Ánh xạ views
        calendarView = findViewById(R.id.calendarViewDetail);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        ImageButton btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        ImageButton btnNextMonth = findViewById(R.id.btnNextMonth);
        progressBar = findViewById(R.id.progressBarAttendanceDetail); // Ánh xạ ProgressBar

        setupCalendar();

        btnPreviousMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));

        // Tải lịch sử lần đầu
        fetchAttendanceHistory(currentMonth.getYear(), currentMonth.getMonthValue());
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
                        iconView.setImageResource(R.drawable.ic_check_circle_green);
                        iconView.setVisibility(View.VISIBLE);
                    } else if (date.isBefore(LocalDate.now())) {
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM uuuu", new Locale("vi"));
            tvMonthYear.setText(currentMonth.format(formatter));
            fetchAttendanceHistory(currentMonth.getYear(), currentMonth.getMonthValue());
            return null;
        });
    }

    private void fetchAttendanceHistory(int year, int month) {
        progressBar.setVisibility(View.VISIBLE);

        // SỬA ĐỔI: Sử dụng ApiConfig
        String baseUrl = ApiConfig.getBaseUrl(this);
        String url = baseUrl + "attendance/history/" + this.viewedUserId + "?year=" + year + "&month=" + month;
        Log.d(LOG_TAG, "Fetching user history from: " + url);

        // SỬA ĐỔI QUAN TRỌNG: Dùng JsonArrayRequest
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        attendedDates.clear();
                        // Duyệt qua mảng các đối tượng JSON nhận được
                        for (int i = 0; i < response.length(); i++) {
                            // 1. Lấy ra đối tượng JSON tại vị trí i
                            JSONObject record = response.getJSONObject(i);

                            // 2. Từ đối tượng đó, lấy ra giá trị của key "date"
                            String dateStr = record.getString("date");

                            // 3. Phân tích chuỗi ngày tháng chính xác
                            attendedDates.add(LocalDate.parse(dateStr));
                        }
                        // Cập nhật lại giao diện lịch
                        calendarView.notifyCalendarChanged();
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Lỗi phân tích JSON", e);
                        Toast.makeText(this, "Lỗi phân tích dữ liệu từ server.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(LOG_TAG, "Lỗi tải lịch sử: " + error.toString());
                    Toast.makeText(this, "Không thể tải lịch sử điểm danh.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + adminToken);
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
