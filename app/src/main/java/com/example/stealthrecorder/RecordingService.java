package com.example.stealthrecorder;

import android.app.*;
import android.content.*;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private static final int NOTIFICATION_ID = 1001;
    
    private MediaRecorder mediaRecorder;
    private String outputFile;
    private PowerManager.WakeLock wakeLock;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        // 启动前台服务
        startForegroundService();
        
        // 开始录音
        if (startRecording()) {
            return START_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }
    
    private void startForegroundService() {
        // 创建简单通知（兼容旧版本）
        Notification notification = new Notification.Builder(this)
            .setContentTitle("语音备忘录 - 录音中")
            .setContentText("点击返回应用")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build();
        
        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "Foreground service started");
    }
    
    private boolean startRecording() {
        try {
            Log.d(TAG, "=== Service: 开始录音 ===");
            
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_" + timeStamp + ".m4a";
            Log.d(TAG, "文件名: " + fileName);
            
            File recordsDir = new File(android.os.Environment.getExternalStorageDirectory(), "Recordings");
            Log.d(TAG, "录音目录: " + recordsDir.getAbsolutePath());
            
            if (!recordsDir.exists()) {
                boolean created = recordsDir.mkdirs();
                Log.d(TAG, "创建目录结果: " + created);
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            Log.d(TAG, "输出文件: " + outputFile);
            
            // 检查目录权限
            Log.d(TAG, "目录可写: " + recordsDir.canWrite());
            Log.d(TAG, "目录存在: " + recordsDir.exists());
            
            // 获取WakeLock
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StealthRecorder:RecordingWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L);
            Log.d(TAG, "WakeLock获取成功");
            
            // 初始化MediaRecorder
            mediaRecorder = new MediaRecorder();
            Log.d(TAG, "MediaRecorder创建成功");
            
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(22050);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setOutputFile(outputFile);
            
            Log.d(TAG, "MediaRecorder配置完成");
            mediaRecorder.prepare();
            Log.d(TAG, "MediaRecorder准备完成");
            
            mediaRecorder.start();
            Log.d(TAG, "Recording started: " + outputFile);
            
            // 立即检查文件大小
            File file = new File(outputFile);
            if (file.exists()) {
                Log.d(TAG, "文件已创建，大小: " + file.length() + " bytes");
            } else {
                Log.e(TAG, "文件未创建!");
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        stopRecording();
        super.onDestroy();
    }
    
    private void stopRecording() {
        Log.d(TAG, "=== Service: 停止录音 ===");
        
        if (mediaRecorder != null) {
            try {
                Log.d(TAG, "停止MediaRecorder...");
                mediaRecorder.stop();
                Log.d(TAG, "MediaRecorder已停止");
                mediaRecorder.release();
                Log.d(TAG, "MediaRecorder已释放");
                Log.d(TAG, "Recording stopped: " + outputFile);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
                e.printStackTrace();
            }
            mediaRecorder = null;
        } else {
            Log.w(TAG, "mediaRecorder为null，无法停止");
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        } else {
            Log.w(TAG, "WakeLock未持有或为null");
        }
        
        // 检查最终文件大小
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.exists()) {
                Log.d(TAG, "最终文件大小: " + file.length() + " bytes, 路径: " + outputFile);
                if (file.length() <= 1024) { // 小于1KB
                    Log.e(TAG, "警告：文件大小异常小，可能录音失败");
                }
            } else {
                Log.e(TAG, "文件不存在: " + outputFile);
            }
        } else {
            Log.w(TAG, "outputFile为null");
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
