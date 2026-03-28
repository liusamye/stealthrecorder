package com.example.stealthrecorder;

import android.util.Log;

public class LogUtil {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "StealthRecorder";
    
    public static void d(String message) {
        if (DEBUG) {
            LogUtil.d( message);
        }
    }
    
    public static void i(String message) {
        if (DEBUG) {
            LogUtil.i( message);
        }
    }
    
    public static void w(String message) {
        LogUtil.w( message);
    }
    
    public static void e(String message) {
        LogUtil.e( message);
    }
    
    public static void e(String message, Throwable t) {
        LogUtil.e( message, t);
    }
}