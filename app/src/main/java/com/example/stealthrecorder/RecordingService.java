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
    private boolean isRecording = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d( "Service onCreate");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d( "Service onStartCommand");
        
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
        // 创建通知渠道（Android 8.0+要求）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "recording_channel",
                "录音服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("语音备忘录录音服务");
            channel.setShowBadge(false);
            
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        
        // 创建通知
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "recording_channel");
        } else {
            builder = new Notification.Builder(this);
        }
        
        Notification notification = builder
            .setContentTitle("语音备忘录 - 录音中")
            .setContentText("点击返回应用")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
        
        startForeground(NOTIFICATION_ID, notification);
        LogUtil.d( "Foreground service started with notification");
    }
    
    private boolean startRecording() {
        try {
            LogUtil.d( "=== Service: 开始录音 ===");
            
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_" + timeStamp + ".m4a";
            LogUtil.d( "文件名: " + fileName);
            
            File recordsDir = new File(android.os.Environment.getExternalStorageDirectory(), "Recordings");
            LogUtil.d( "录音目录: " + recordsDir.getAbsolutePath());
            
            if (!recordsDir.exists()) {
                boolean created = recordsDir.mkdirs();
                LogUtil.d( "创建目录结果: " + created);
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            LogUtil.d( "输出文件: " + outputFile);
            
            // 检查目录权限
            LogUtil.d( "目录可写: " + recordsDir.canWrite());
            LogUtil.d( "目录存在: " + recordsDir.exists());
            
            // 获取WakeLock
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StealthRecorder:RecordingWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L);
            LogUtil.d( "WakeLock获取成功");
            
            // 初始化MediaRecorder
            mediaRecorder = new MediaRecorder();
            LogUtil.d( "MediaRecorder创建成功");
            
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(22050);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setOutputFile(outputFile);
            
            LogUtil.d( "MediaRecorder配置完成");
            mediaRecorder.prepare();
            LogUtil.d( "MediaRecorder准备完成");
            
            mediaRecorder.start();
            LogUtil.d( "Recording started: " + outputFile);
            isRecording = true;
            
            // 立即检查文件大小
            File file = new File(outputFile);
            if (file.exists()) {
                LogUtil.d( "文件已创建，大小: " + file.length() + " bytes");
            } else {
                LogUtil.e( "文件未创建!");
            }
            
            return true;
            
        } catch (Exception e) {
            LogUtil.e( "Failed to start recording", e);
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public void onDestroy() {
        LogUtil.d( "Service onDestroy");
        stopRecording();
        super.onDestroy();
    }
    
    private void stopRecording() {
        LogUtil.d( "=== Service: 停止录音 ===");
        
        if (!isRecording) {
            LogUtil.w( "录音未开始，无需停止");
            return;
        }
        
        if (mediaRecorder != null) {
            try {
                LogUtil.d( "停止MediaRecorder...");
                mediaRecorder.stop();
                LogUtil.d( "MediaRecorder已停止");
                mediaRecorder.release();
                LogUtil.d( "MediaRecorder已释放");
                LogUtil.d( "Recording stopped: " + outputFile);
            } catch (Exception e) {
                LogUtil.e( "Error stopping recording", e);
                e.printStackTrace();
            }
            mediaRecorder = null;
        } else {
            LogUtil.w( "mediaRecorder为null，无法停止");
        }
        
        isRecording = false;
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            LogUtil.d( "WakeLock released");
        } else {
            LogUtil.w( "WakeLock未持有或为null");
        }
        
        // 检查最终文件大小
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.exists()) {
                LogUtil.d( "最终文件大小: " + file.length() + " bytes, 路径: " + outputFile);
                if (file.length() <= 1024) { // 小于1KB
                    LogUtil.e( "警告：文件大小异常小，可能录音失败");
                }
            } else {
                LogUtil.e( "文件不存在: " + outputFile);
            }
        } else {
            LogUtil.w( "outputFile为null");
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
