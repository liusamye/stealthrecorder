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
                Toast.makeText(MainActivity.this, 
                    "点击开始/停止录音\n文件保存在: /Records/", 
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }
    
    private void checkAndRequestPermissions() {
        // 检查录音权限
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
        
        // 检查存储权限（Android 10+需要）
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_STORAGE_PERMISSION);
        }
    }
    
    private void startRecording() {
        try {
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Recording_" + timeStamp + ".mp3";
            
            // 创建 /Records 目录（在外部存储根目录）
            File recordsDir = new File(Environment.getExternalStorageDirectory(), "Records");
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
            fileInfoText.setText("保存到: " + fileName);
            
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
                fileInfoText.setText("文件位置: /Records/");
                
                // 恢复按钮颜色为绿色
                GradientDrawable drawable = (GradientDrawable) recordButton.getBackground();
                drawable.setColor(0xFF4CAF50); // 绿色
                
                // 显示保存信息
                String fileName = new File(outputFile).getName();
                Toast.makeText(this, 
                    "记录已保存: " + fileName + "\n位置: /Records/", 
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