package com.example.stealthrecorder;

import android.app.*;
import android.content.*;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "recording_channel";
    
    private MediaRecorder mediaRecorder;
    private String outputFile;
    private PowerManager.WakeLock wakeLock;
    private Handler durationHandler;
    
    // 鸿蒙适配：记录服务启动时间
    private long serviceStartTime;
    private int recordingMinutes = 0;
    
    // 广播Action
    public static final String ACTION_STOP_RECORDING = "com.example.stealthrecorder.STOP_RECORDING";
    public static final String ACTION_UPDATE_DURATION = "com.example.stealthrecorder.UPDATE_DURATION";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();
        
        // 初始化Handler用于更新时长
        durationHandler = new Handler(Looper.getMainLooper());
        
        // 记录服务启动时间（用于诊断）
        serviceStartTime = System.currentTimeMillis();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand, startId: " + startId);
        
        if (intent != null && ACTION_STOP_RECORDING.equals(intent.getAction())) {
            Log.d(TAG, "收到停止录音指令");
            stopRecordingAndService();
            return START_NOT_STICKY;
        }
        
        // 从Intent获取文件路径
        if (intent != null && intent.hasExtra("outputFile")) {
            outputFile = intent.getStringExtra("outputFile");
            Log.d(TAG, "从Intent获取文件路径: " + outputFile);
        }
        
        // 关键：启动前台服务
        startForegroundService();
        
        // 开始录音
        if (startRecording()) {
            // 关键：对于鸿蒙/华为设备，使用START_REDELIVER_INTENT
            // 这样服务被杀死后会重新启动
            return START_REDELIVER_INTENT;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }
    
    private void startForegroundService() {
        // 创建不可清除的持续通知
        Notification notification = createRecordingNotification();
        
        // 关键：启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        Log.d(TAG, "Foreground service started with notification");
    }
    
    private Notification createRecordingNotification() {
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "录音通知",
                NotificationManager.IMPORTANCE_LOW  // 使用LOW避免打扰
            );
            channel.setDescription("录音进行中通知");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // 关键：设置不静音、不振动
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        
        // 创建PendingIntent用于点击通知返回应用
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("语音备忘录 - 录音中")
            .setContentText("点击返回应用")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)  // 使用系统麦克风图标
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)  // 低优先级，减少打扰
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)  // 关键：设置为进行中，不可清除
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        // 添加停止录音的Action按钮
        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(android.R.drawable.ic_media_pause, "停止录音", stopPendingIntent);
        
        return builder.build();
    }
    
    private boolean startRecording() {
        try {
            // 如果outputFile为空，创建新的文件路径
            if (outputFile == null || outputFile.isEmpty()) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "Note_" + timeStamp + ".m4a";
                
                File recordsDir;
                // 优先尝试：保存到根目录简单路径
                // 路径：内部存储/Recordings/
                recordsDir = new File(Environment.getExternalStorageDirectory(), "Recordings");
                
                if (!recordsDir.exists()) {
                    boolean created = recordsDir.mkdirs();
                    if (!created) {
                        Log.e(TAG, "无法创建目录: " + recordsDir.getAbsolutePath());
                        return false;
                    }
                }
                
                outputFile = new File(recordsDir, fileName).getAbsolutePath();
            }
            
            // 获取WakeLock防止CPU休眠
            acquireWakeLock();
            
            // 初始化MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(22050);  // 22.05kHz
            mediaRecorder.setAudioEncodingBitRate(64000);  // 64kbps
            
            // 设置输出文件
            mediaRecorder.setOutputFile(outputFile);
            
            // 准备并开始录音
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            Log.d(TAG, "Recording started: " + outputFile);
            Log.d(TAG, "Service uptime: " + (System.currentTimeMillis() - serviceStartTime) + "ms");
            
            // 开始更新时长
            startUpdatingDuration();
            
            // 鸿蒙诊断：每分钟记录一次日志
            startHarmonyDiagnostic();
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting recording", e);
            return false;
        }
    }
    
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StealthRecorder:RecordingWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L /*10分钟*/);  // 设置超时，避免忘记释放
        Log.d(TAG, "WakeLock acquired");
    }
    
    private void startUpdatingDuration() {
        // 每分钟更新一次通知，保持活跃
        durationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordingMinutes++;
                Log.d(TAG, "Recording duration: " + recordingMinutes + " minutes");
                
                // 更新通知内容
                updateNotificationDuration(recordingMinutes);
                
                // 关键：每分钟重新调度，保持Handler活跃
                // 这对于鸿蒙系统特别重要
                durationHandler.postDelayed(this, 60000);
                
                // 发送广播更新MainActivity（如果在前台）
                sendDurationUpdateBroadcast(recordingMinutes);
            }
        }, 60000);
    }
    
    private void startHarmonyDiagnostic() {
        // 专门针对鸿蒙的诊断
        Handler harmonyHandler = new Handler(Looper.getMainLooper());
        harmonyHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Harmony diagnostic - Service alive at " + recordingMinutes + " minute(s)");
                Log.d(TAG, "Is recording: " + (mediaRecorder != null));
                Log.d(TAG, "Service total uptime: " + 
                    (System.currentTimeMillis() - serviceStartTime) / 1000 + " seconds");
                
                // 检查是否还在录音状态
                if (mediaRecorder == null) {
                    Log.w(TAG, "MediaRecorder is null at " + recordingMinutes + " minute(s)!");
                    // 尝试重启录音
                    restartRecordingIfNeeded();
                }
                
                // 继续诊断
                harmonyHandler.postDelayed(this, 60000);
            }
        }, 60000);
    }
    
    private void restartRecordingIfNeeded() {
        Log.d(TAG, "Attempting to restart recording...");
        try {
            stopRecording();
            startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart recording", e);
        }
    }
    
    private void updateNotificationDuration(int minutes) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        
        // 更新通知内容
        Notification notification = createRecordingNotification();
        NotificationCompat.Builder builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("语音备忘录 - 录音中 (" + minutes + "分钟)")
            .setContentText("文件: " + new File(outputFile).getName())
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setAutoCancel(false);
        
        // 添加停止按钮
        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(android.R.drawable.ic_media_pause, "停止录音", stopPendingIntent);
        
        manager.notify(NOTIFICATION_ID, builder.build());
    }
    
    private void sendDurationUpdateBroadcast(int minutes) {
        Intent intent = new Intent(ACTION_UPDATE_DURATION);
        intent.putExtra("durationMinutes", minutes);
        sendBroadcast(intent);
    }
    
    private void stopRecordingAndService() {
        stopRecording();
        stopSelf();
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                Log.d(TAG, "Recording stopped and saved: " + outputFile);
                
                // 检查文件
                File recordedFile = new File(outputFile);
                if (recordedFile.exists()) {
                    long fileSize = recordedFile.length();
                    Log.d(TAG, "录音文件大小: " + fileSize + " 字节");
                    
                    // 发送录音完成广播
                    Intent intent = new Intent("com.example.stealthrecorder.RECORDING_COMPLETE");
                    intent.putExtra("filePath", outputFile);
                    intent.putExtra("fileSize", fileSize);
                    intent.putExtra("durationMinutes", recordingMinutes);
                    sendBroadcast(intent);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
            }
            mediaRecorder = null;
        }
        
        // 释放WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }
        
        // 移除Handler回调
        if (durationHandler != null) {
            durationHandler.removeCallbacksAndMessages(null);
        }
        
        // 取消通知
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "录音通知",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("录音进行中通知");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // 关键：设置不静音
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        
        // 停止录音
        stopRecording();
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}