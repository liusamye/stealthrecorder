package com.example.stealthrecorder;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
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
            String fileName = "Note_" + timeStamp + ".m4a";
            
            // 保存到应用私有目录
            File recordsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes");
            
            if (!recordsDir.exists()) {
                boolean created = recordsDir.mkdirs();
                if (!created) {
                    Toast.makeText(this, "无法创建目录", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // 检查目录是否可写
            if (!recordsDir.canWrite()) {
                Toast.makeText(this, "目录不可写，请检查权限", Toast.LENGTH_LONG).show();
                return;
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            
            // 调试信息
            Log.d("StealthRecorder", "开始录音，文件路径: " + outputFile);
            Log.d("StealthRecorder", "目录可写: " + recordsDir.canWrite());
            Log.d("StealthRecorder", "目录路径: " + recordsDir.getAbsolutePath());
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            
            // 使用更兼容的设置
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            
            // 降低质量以提高兼容性
            mediaRecorder.setAudioEncodingBitRate(64000);  // 从128k降低到64k
            mediaRecorder.setAudioSamplingRate(22050);     // 从44.1k降低到22.05k
            
            mediaRecorder.setOutputFile(outputFile);
            
            // 准备录音
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                Log.e("StealthRecorder", "准备录音失败: " + e.getMessage(), e);
                Toast.makeText(this, "准备录音失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                mediaRecorder.release();
                mediaRecorder = null;
                return;
            }
            
            // 开始录音
            try {
                mediaRecorder.start();
            } catch (IllegalStateException e) {
                Log.e("StealthRecorder", "开始录音失败: " + e.getMessage(), e);
                Toast.makeText(this, "开始录音失败，请重试", Toast.LENGTH_LONG).show();
                mediaRecorder.release();
                mediaRecorder = null;
                return;
            }
            
            isRecording = true;
            startTime = System.currentTimeMillis();
            
            // 更新UI
            recordButton.setText("■ 停止记录");
            statusText.setText("🔴 记录中...");
            timerText.setVisibility(View.VISIBLE);
            timerText.setText("00:00");
            fileInfoText.setText("文件: " + fileName);
            
            // 动态改变按钮背景为录音状态
            recordButton.setBackgroundResource(R.drawable.record_button_recording);
            Log.d("StealthRecorder", "按钮背景已设置为录音状态");
            
            // 只使用低 profile 模式，不隐藏状态栏和导航栏
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE);
            
            // 启动计时器
            timerHandler.postDelayed(timerRunnable, 1000);
            
            Log.d("StealthRecorder", "录音已成功开始");
            
        } catch (Exception e) {
            Log.e("StealthRecorder", "录音启动异常: " + e.getMessage(), e);
            Toast.makeText(this, "录音启动异常: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
        }
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                
                // 检查文件是否创建成功
                File recordedFile = new File(outputFile);
                if (recordedFile.exists()) {
                    long fileSize = recordedFile.length();
                    Log.d("StealthRecorder", "录音文件已保存，大小: " + fileSize + " 字节");
                    
                    if (fileSize < 1024) { // 小于1KB可能是空的
                        Toast.makeText(this, "录音文件可能为空 (" + fileSize + "字节)", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.w("StealthRecorder", "录音文件未创建: " + outputFile);
                    Toast.makeText(this, "录音文件未创建", Toast.LENGTH_LONG).show();
                }
                
                mediaRecorder.release();
                mediaRecorder = null;
                
                isRecording = false;
                timerHandler.removeCallbacks(timerRunnable);
                
                // 更新UI
                recordButton.setText("● 开始记录");
                statusText.setText("🟢 记录已保存");
                timerText.setVisibility(View.GONE);
                fileInfoText.setText("文件已保存到笔记");
                
                // 恢复按钮背景为正常状态
                recordButton.setBackgroundResource(R.drawable.record_button_bg);
                Log.d("StealthRecorder", "按钮背景已恢复为正常状态");
                
                // 显示保存信息
                String fileName = new File(outputFile).getName();
                Toast.makeText(this, 
                    "笔记已保存: " + fileName, 
                    Toast.LENGTH_LONG).show();
                
            } catch (RuntimeException e) {
                Log.e("StealthRecorder", "停止录音失败: " + e.getMessage(), e);
                Toast.makeText(this, "停止录音失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                
                // 即使停止失败也要释放资源
                try {
                    mediaRecorder.release();
                } catch (Exception ex) {
                    Log.e("StealthRecorder", "释放MediaRecorder失败", ex);
                }
                mediaRecorder = null;
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