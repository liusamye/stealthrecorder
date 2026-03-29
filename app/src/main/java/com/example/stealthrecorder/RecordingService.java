package com.example.stealthrecorder;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences;
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
    
    // 静态变量跟踪服务状态
    private static boolean isServiceRunning = false;
    
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
        
        // 更新服务状态
        isServiceRunning = true;
        
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
        LogUtil.d("Starting foreground service...");
        
        // 检查调试模式：是否隐藏通知
        SharedPreferences prefs = getSharedPreferences("debug_settings", Context.MODE_PRIVATE);
        boolean hideNotification = prefs.getBoolean("hide_notification_mode", false);
        
        if (hideNotification) {
            LogUtil.d("Debug mode: Completely hiding status bar notification");
            // 在调试模式下，使用完全隐藏的通知
            startForeground(NOTIFICATION_ID, createHiddenNotification());
            return;
        }
        
        // 正常模式：显示通知
        // 确保通知渠道存在
        createNotificationChannel();
        
        // 创建点击通知返回应用的Intent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = null;
        
        try {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);
        } catch (Exception e) {
            LogUtil.e("Failed to create PendingIntent", e);
        }
        
        // 创建通知
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "recording_channel");
        } else {
            builder = new Notification.Builder(this);
        }
        
        // 构建极简通知 - 只显示"录音中"
        builder.setContentTitle(getString(R.string.notification_title))
               .setContentText("")  // 空文本，不显示任何内容
               .setSmallIcon(R.drawable.ic_notification_recording)
               .setOngoing(true)
               .setCategory(Notification.CATEGORY_SERVICE)
               .setShowWhen(false)  // 不显示时间
               .setOnlyAlertOnce(true);
        
        // 兼容性处理：不显示计时器和子文本
        // 通过不设置when时间戳来避免显示计时器
        builder.setWhen(0);  // 设置为0，不显示时间
        
        // 不显示子文本 - 使用兼容性检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // setSubText方法从API level 16开始可用
            builder.setSubText("");
        }
        
        // 设置点击意图
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }
        
        // 设置最低优先级
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPriority(NotificationManager.IMPORTANCE_MIN);  // 最低优先级
        } else {
            builder.setPriority(Notification.PRIORITY_MIN);  // 最低优先级
        }
        
        Notification notification = builder.build();
        
        // 启动前台服务
        try {
            startForeground(NOTIFICATION_ID, notification);
            LogUtil.d("✅ Foreground service started successfully, notification ID: " + NOTIFICATION_ID);
        } catch (Exception e) {
            LogUtil.e("❌ Failed to start foreground service", e);
            // 尝试重新创建通知渠道并重试
            createNotificationChannel();
            try {
                startForeground(NOTIFICATION_ID, notification);
                LogUtil.d("✅ Restarted foreground service successfully");
            } catch (Exception e2) {
                LogUtil.e("❌ Failed to restart foreground service", e2);
            }
        }
    }
    
    private Notification createHiddenNotification() {
        // 创建一个完全隐藏的通知，不显示在状态栏下拉框中
        Notification.Builder builder;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+：使用隐藏的通知渠道
            createHiddenNotificationChannel();
            builder = new Notification.Builder(this, "hidden_channel");
        } else {
            builder = new Notification.Builder(this);
        }
        
        // 创建一个完全隐藏的通知
        // 使用透明的小图标（1x1像素的透明图片）
        // 但实际上，我们可以使用一个几乎不可见的配置
        builder.setContentTitle("")
               .setContentText("")
               .setSmallIcon(android.R.drawable.ic_menu_gallery)  // 使用系统内置的不显眼图标
               .setOngoing(true)
               .setShowWhen(false)
               .setOnlyAlertOnce(true)
               .setWhen(0)
               .setPriority(Notification.PRIORITY_MIN);  // 最低优先级
        
        // 对于Android 8.0+，设置通知渠道为最低重要性
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPriority(NotificationManager.IMPORTANCE_MIN);
        }
        
        return builder.build();
    }
    
    private void createHiddenNotificationChannel() {
        // 创建完全隐藏的通知渠道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 使用最低的重要性级别，几乎不显示
                NotificationChannel channel = new NotificationChannel(
                    "hidden_channel",
                    "Hidden Service",  // 用户看不到的渠道名称
                    NotificationManager.IMPORTANCE_NONE  // 完全不显示
                );
                channel.setDescription("Hidden service channel");
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                channel.setImportance(NotificationManager.IMPORTANCE_NONE);  // 完全不显示
                
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    LogUtil.d("✅ Hidden notification channel created: hidden_channel (IMPORTANCE_NONE)");
                }
            } catch (Exception e) {
                LogUtil.e("❌ Failed to create hidden notification channel", e);
            }
        }
    }
    
    private void createNotificationChannel() {
        // 创建极低调的通知渠道（Android 8.0+要求）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 使用最低的重要性级别，几乎不显示
                NotificationChannel channel = new NotificationChannel(
                    "recording_channel",
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_MIN  // 最低重要性
                );
                channel.setDescription(getString(R.string.notification_channel_description));
                channel.setShowBadge(false);  // 不显示角标
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);  // 锁屏时隐藏
                channel.enableLights(false);  // 不亮灯
                channel.enableVibration(false);  // 不震动
                channel.setSound(null, null);  // 没有声音
                channel.setBypassDnd(true);  // 绕过勿扰模式
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);  // 锁屏时只显示图标
                
                // 进一步降低通知的干扰
                channel.setImportance(NotificationManager.IMPORTANCE_MIN);
                
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    LogUtil.d("✅ Minimal notification channel created: recording_channel (IMPORTANCE_MIN)");
                } else {
                    LogUtil.e("❌ Cannot get NotificationManager");
                }
            } catch (Exception e) {
                LogUtil.e("❌ Failed to create notification channel", e);
            }
        }
    }
    
    private boolean startRecording() {
        try {
            LogUtil.d( "=== Service: Starting recording ===");
            
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_" + timeStamp + ".m4a";
            LogUtil.d( "Filename: " + fileName);
            
            File recordsDir = new File(android.os.Environment.getExternalStorageDirectory(), getString(R.string.recording_folder));
            LogUtil.d( "Recording directory: " + recordsDir.getAbsolutePath());
            
            if (!recordsDir.exists()) {
                boolean created = recordsDir.mkdirs();
                LogUtil.d( "Directory creation result: " + created);
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            LogUtil.d( "Output file: " + outputFile);
            
            // 检查目录权限
            LogUtil.d( "Directory writable: " + recordsDir.canWrite());
            LogUtil.d( "Directory exists: " + recordsDir.exists());
            
            // 获取WakeLock
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StealthRecorder:RecordingWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L);
            LogUtil.d( "WakeLock acquired successfully");
            
            // 初始化MediaRecorder
            mediaRecorder = new MediaRecorder();
            LogUtil.d( "MediaRecorder created successfully");
            
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(22050);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setOutputFile(outputFile);
            
            LogUtil.d( "MediaRecorder configured");
            mediaRecorder.prepare();
            LogUtil.d( "MediaRecorder prepared");
            
            mediaRecorder.start();
            LogUtil.d( "Recording started: " + outputFile);
            isRecording = true;
            
            // 立即检查文件大小
            File file = new File(outputFile);
            if (file.exists()) {
                LogUtil.d( "文件已创建，大小: " + file.length() + " bytes");
            } else {
                LogUtil.e( "File not created!");
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
        
        // 更新服务状态
        isServiceRunning = false;
        
        super.onDestroy();
    }
    
    // 静态方法供外部检查服务状态
    public static boolean isServiceRunning() {
        return isServiceRunning;
    }
    
    private void stopRecording() {
        LogUtil.d( "=== Service: 停止录音 ===");
        
        if (!isRecording) {
            LogUtil.w( "Not recording, no need to stop");
            return;
        }
        
        if (mediaRecorder != null) {
            try {
                LogUtil.d( "Stopping MediaRecorder...");
                mediaRecorder.stop();
                LogUtil.d( "MediaRecorder stopped");
                mediaRecorder.release();
                LogUtil.d( "MediaRecorder released");
                LogUtil.d( "Recording stopped: " + outputFile);
            } catch (Exception e) {
                LogUtil.e( "Error stopping recording", e);
                e.printStackTrace();
            }
            mediaRecorder = null;
        } else {
            LogUtil.w( "mediaRecorder is null, cannot stop");
        }
        
        isRecording = false;
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            LogUtil.d( "WakeLock released");
        } else {
            LogUtil.w( "WakeLock not held or is null");
        }
        
        // 检查最终文件大小
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.exists()) {
                LogUtil.d( "最终文件大小: " + file.length() + " bytes, 路径: " + outputFile);
                if (file.length() <= 1024) { // 小于1KB
                    LogUtil.e( "Warning: File size abnormally small, recording may have failed");
                }
            } else {
                LogUtil.e( "File does not exist: " + outputFile);
            }
        } else {
            LogUtil.w( "outputFile is null");
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
