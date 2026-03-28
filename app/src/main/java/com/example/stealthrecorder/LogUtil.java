package com.example.stealthrecorder;

import android.util.Log;

public class LogUtil {
    // 简化版本，避免BuildConfig问题
    public static void d(String message) {
        // 发布版本中禁用debug日志
        // Log.d("StealthRecorder", message);
    }
    
    public static void i(String message) {
        // 发布版本中禁用了info日志
    }
    
    public static void w(String message) {
        Log.w("StealthRecorder", message);
    }
    
    public static void e(String message) {
        Log.e("StealthRecorder", message);
    }
    
    public static void e(String message, Throwable t) {
        Log.e("StealthRecorder", message, t);
    }
}