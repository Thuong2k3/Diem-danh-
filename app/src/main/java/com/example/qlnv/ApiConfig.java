package com.example.qlnv;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiConfig {

    public static final String IP_PREFS = "IP_PREFS";
    public static final String IP_KEY = "server_ip";

    // Địa chỉ IP mặc định nếu chưa được cấu hình
    private static final String DEFAULT_IP = "192.168.1.10";

    /**
     * Lấy URL đầy đủ của server, ưu tiên IP đã lưu.
     * @param context Context của ứng dụng.
     * @return URL dạng "http://<ip_address>:3000/api/"
     */
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(IP_PREFS, Context.MODE_PRIVATE);
        String ipAddress = prefs.getString(IP_KEY, DEFAULT_IP);
        // THÊM DẤU /api/ VÀO ĐÂY ĐỂ ĐỒNG BỘ VỚI CÁCH GỌI TRONG CÁC ACTIVITY KHÁC
        return "http://" + ipAddress + ":3000/api/";
    }

    /**
     * Lưu địa chỉ IP mới vào SharedPreferences.
     * @param context Context của ứng dụng.
     * @param ipAddress Địa chỉ IP mới cần lưu.
     */
    public static void setIpAddress(Context context, String ipAddress) {
        SharedPreferences prefs = context.getSharedPreferences(IP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(IP_KEY, ipAddress);
        editor.apply();
    }

    /**
     * Lấy địa chỉ IP đã được lưu trữ để hiển thị trên UI.
     * @param context Context của ứng dụng.
     * @return Địa chỉ IP đã lưu, hoặc chuỗi rỗng nếu chưa có.
     */
    public static String getSavedIp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(IP_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(IP_KEY, "");
    }

    // Phương thức này không còn dùng để điều hướng chính, nhưng vẫn hữu ích
    public static boolean isIpConfigured(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(IP_PREFS, Context.MODE_PRIVATE);
        return prefs.contains(IP_KEY);
    }
}
