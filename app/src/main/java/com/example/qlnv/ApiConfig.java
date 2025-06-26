package com.example.qlnv;

import android.content.Context;
import android.content.SharedPreferences;
public class ApiConfig {

    // Lấy BASE_URL động từ SharedPreferences, nếu không có thì dùng từ BuildConfig
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(IpConfigActivity.CONFIG_PREFS, Context.MODE_PRIVATE);

        // Lấy IP, nếu không có thì lấy chuỗi rỗng
        String ip = prefs.getString(IpConfigActivity.KEY_IP_ADDRESS, "");
        // Lấy Port, nếu không có thì lấy chuỗi rỗng
        String port = prefs.getString(IpConfigActivity.KEY_PORT, "");

        // Nếu người dùng đã nhập và lưu IP/Port
        if (ip != null && !ip.isEmpty() && port != null && !port.isEmpty()) {
            return "http://" + ip + ":" + port + "/api/";
        }

        // Nếu không, dùng URL mặc định từ build.gradle
        return BuildConfig.BASE_URL;
    }
}