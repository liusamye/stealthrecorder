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
        LogUtil.d("Service onStartCommand - FAST START");
        
        // 第一步：立即启动前台服务（必须在5秒内完成）
        startForegroundService();
        
        // 第二步：更新状态
        isServiceRunning = true;
        
        // 第三步：异步开始录音（避免阻塞前台服务启动）
        new Thread(() -> {
            LogUtil.d("Starting recording in background thread...");
            if (startRecording()) {
                LogUtil.d("✅ Recording started successfully in background");
            } else {
                LogUtil.e("❌ Recording failed, stopping service");
                stopSelf();
            }
        }).start();
        
        return START_STICKY;
    }
    
    private void startForegroundService() {
        LogUtil.d("Starting foreground service - FAST VERSION");
        long startTime = System.currentTimeMillis();
        
        // 检查调试模式：是否隐藏通知
        SharedPreferences prefs = getSharedPreferences("debug_settings", Context.MODE_PRIVATE);
        boolean hideNotification = prefs.getBoolean("hide_notification_mode", false);
        
        if (hideNotification) {
            LogUtil.d("Debug mode: Using hidden notification");
            startForeground(NOTIFICATION_ID, createHiddenNotification());
            LogUtil.d("✅ Hidden notification started in " + (System.currentTimeMillis() - startTime) + "ms");
            return;
        }
        
        // 快速创建通知渠道（如果不存在）
        createNotificationChannel();
        
        // 创建最简单的通知 - 确保5秒内完成
        Notification notification = createQuickNotification();
        
        // 立即启动前台服务（关键：必须在5秒内完成）
        try {
            startForeground(NOTIFICATION_ID, notification);
            LogUtil.d("✅ Foreground service started in " + (System.currentTimeMillis() - startTime) + "ms, ID: " + NOTIFICATION_ID);
            
            // 异步完善通知（添加点击意图等）
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                enhanceNotificationWithDetails();
            }, 1000); // 1秒后完善通知
            
        } catch (Exception e) {
            LogUtil.e("❌ Failed to start foreground service", e);
            
            // 紧急重试：使用最基本的通知
            try {
                Notification emergencyNotification = createEmergencyNotification();
                startForeground(NOTIFICATION_ID, emergencyNotification);
                LogUtil.d("✅ Emergency foreground service started");
            } catch (Exception e2) {
                LogUtil.e("❌ Emergency start also failed", e2);
            }
        }
    }
    
    private Notification createQuickNotification() {
        // 创建最简单的通知，确保快速完成
        Notification.Builder builder;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "recording_channel");
        } else {
            builder = new Notification.Builder(this);
        }
        
        builder.setContentTitle(getString(R.string.notification_title))
               .setSmallIcon(R.drawable.ic_notification_recording)
               .setOngoing(true)
               .setWhen(System.currentTimeMillis());  // 显示当前时间
        
        // 设置合适的优先级（不是最低，避免被隐藏）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPriority(NotificationManager.IMPORTANCE_LOW);
        } else {
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        
        return builder.build();
    }
    
    private void enhanceNotificationWithDetails() {
        // 完善通知：添加点击意图等
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);
            
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, "recording_channel");
            } else {
                builder = new Notification.Builder(this);
            }
            
            builder.setContentTitle(getString(R.string.notification_title))
                   .setSmallIcon(R.drawable.ic_notification_recording)
                   .setOngoing(true)
                   .setContentIntent(pendingIntent)
                   .setWhen(System.currentTimeMillis());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPriority(NotificationManager.IMPORTANCE_LOW);
            } else {
                builder.setPriority(Notification.PRIORITY_LOW);
            }
            
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                LogUtil.d("✅ Notification enhanced with click intent");
            }
            
        } catch (Exception e) {
            LogUtil.e("Failed to enhance notification", e);
        }
    }
    
    private Notification createEmergencyNotification() {
        // 创建最基本的紧急通知
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Recording")
               .setSmallIcon(android.R.drawable.ic_btn_speak_now)
               .setOngoing(true)
               .setWhen(System.currentTimeMillis());
        
        return builder.build();
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
            LogUtil.d("=== Starting recording (optimized) ===");
            long startTime = System.currentTimeMillis();
            
            // 快速创建文件路径（最小化文件系统操作）
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_" + timeStamp + ".m4a";
            File recordsDir = new File(Environment.getExternalStorageDirectory(), getString(R.string.recording_folder));
            
            // 异步创建目录（不阻塞）
            if (!recordsDir.exists()) {
                new Thread(() -> {
                    recordsDir.mkdirs();
                }).start();
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            
            // 快速获取WakeLock
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StealthRecorder:WakeLock");
            wakeLock.acquire();
            
            // 快速初始化MediaRecorder（最小配置）
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(outputFile);
            
            // 准备并开始录音
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            long totalTime = System.currentTimeMillis() - startTime;
            
            LogUtil.d("✅ Recording started in " + totalTime + "ms: " + outputFile);
            
            // 异步检查文件（不阻塞）
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 等待1秒
                    File file = new File(outputFile);
                    if (file.exists()) {
                        LogUtil.d("📁 File created, size: " + file.length() + " bytes");
                    }
                } catch (Exception e) {
                    // 忽略检查错误
                }
            }).start();
            
            return true;
            
        } catch (Exception e) {
            LogUtil.e("❌ Failed to start recording", e);
            return false;
        }
    }
    
    @Override
    public void onDestroy() {
        LogUtil.d("Service onDestroy");
        stopRecording();
        
        // 确保状态重置
        isServiceRunning = false;
        isRecording = false;
        
        super.onDestroy();
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogUtil.d("Service onTaskRemoved - app removed from recent tasks");
        
        // 应用从最近任务中移除时，确保服务停止
        isServiceRunning = false;
        isRecording = false;
        
        // 停止服务
        stopSelf();
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
