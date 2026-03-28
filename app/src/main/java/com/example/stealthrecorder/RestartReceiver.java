package com.example.stealthrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class RestartReceiver extends BroadcastReceiver {
    private static final String TAG = "RestartReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.d( "Received broadcast: " + action);
        
        if (action == null) return;
        
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                LogUtil.d( "设备重启完成");
                // 可以在这里恢复未完成的录音（如果需要）
                break;
                
            case Intent.ACTION_USER_PRESENT:
                LogUtil.d( "用户解锁设备");
                // 用户解锁屏幕，可以检查录音状态
                break;
                
            case "com.example.stealthrecorder.RESTART_RECORDING":
                LogUtil.d( "收到重启录音指令");
                // 检查是否有未完成的录音需要恢复
                checkAndRestartRecording(context);
                break;
        }
    }
    
    private void checkAndRestartRecording(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("RecordingPrefs", Context.MODE_PRIVATE);
        boolean wasRecording = prefs.getBoolean("isRecording", false);
        String recordingFilePath = prefs.getString("recordingFilePath", "");
        
        if (wasRecording && !recordingFilePath.isEmpty()) {
            LogUtil.d( "发现未完成的录音，文件: " + recordingFilePath);
            
            // 这里可以尝试重启录音服务
            // 但由于MediaRecorder状态难以恢复，通常建议通知用户
            // 或者重新开始新的录音
            
            // 清除无效状态
            clearRecordingState(context);
        }
    }
    
    private void clearRecordingState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("RecordingPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("isRecording");
        editor.remove("recordingStartTime");
        editor.remove("recordingFilePath");
        editor.apply();
        LogUtil.d( "已清除录音状态");
    }
}