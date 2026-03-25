package com.example.stealthrecorder;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;
    private MediaRecorder mediaRecorder = null;
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler();
    
    private Button recordButton;
    private TextView statusText;
    private TextView timerText;
    private TextView fileInfoText;
    
    private String outputFile;
    
    // 计时器任务
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        recordButton = findViewById(R.id.recordButton);
        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        fileInfoText = findViewById(R.id.fileInfoText);
        
        // 检查权限
        checkAndRequestPermissions();
        
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });
        
        // 长按提示
        recordButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String savePath;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    savePath = "内部存储/Android/data/com.example.stealthrecorder/files/Documents/Notes/";
                } else {
                    savePath = "内部存储/Notes/";
                }
                Toast.makeText(MainActivity.this, 
                    "点击开始/停止记录\n文件保存在:\n" + savePath, 
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }
    
    private void checkAndRequestPermissions() {
        // 只检查录音权限（必须）
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
        
        // 对于Android 12及以下（API 31及以下），请求存储权限（可选）
        // Android 13（API 33）开始有更严格的存储权限管理
        if (android.os.Build.VERSION.SDK_INT <= 32) {  // Android 12L是API 32
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_STORAGE_PERMISSION);
            }
        }
    }
    
    private void startRecording() {
        try {
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_" + timeStamp + ".m4a";  // 改为.m4a格式，更常见
            
            // 方案A：保存到应用私有目录（Documents子目录，用户可访问）
            File recordsDir;
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+：使用MediaStore或应用私有目录
                recordsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes");
            } else {
                // Android 9及以下：尝试外部存储
                recordsDir = new File(Environment.getExternalStorageDirectory(), "Notes");
            }
            
            if (!recordsDir.exists()) {
                recordsDir.mkdirs();
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(outputFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            startTime = System.currentTimeMillis();
            
            // 更新UI
            recordButton.setText("■ 停止记录");
            statusText.setText("🔴 记录中...");
            timerText.setVisibility(View.VISIBLE);
            timerText.setText("00:00");
            fileInfoText.setText("文件: " + fileName);
            
            // 动态改变按钮颜色为红色
            GradientDrawable drawable = (GradientDrawable) recordButton.getBackground();
            drawable.setColor(0xFFF44336); // 红色
            
            // 只使用低 profile 模式，不隐藏状态栏和导航栏
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE);
            
            // 启动计时器
            timerHandler.postDelayed(timerRunnable, 1000);
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "记录启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "权限被拒绝，请检查应用权限设置", Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                isRecording = false;
                timerHandler.removeCallbacks(timerRunnable);
                
                // 更新UI
                recordButton.setText("● 开始记录");
                statusText.setText("🟢 记录已保存");
                timerText.setVisibility(View.GONE);
                fileInfoText.setText("文件已保存到笔记");
                
                // 恢复按钮颜色为绿色
                GradientDrawable drawable = (GradientDrawable) recordButton.getBackground();
                drawable.setColor(0xFF4CAF50); // 绿色
                
                // 显示保存信息
                String fileName = new File(outputFile).getName();
                String savePath = new File(outputFile).getParent();
                Toast.makeText(this, 
                    "笔记已保存: " + fileName + "\n可在文件管理器中查看", 
                    Toast.LENGTH_LONG).show();
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(this, "记录停止失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}