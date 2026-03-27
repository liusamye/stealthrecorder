package com.example.stealthrecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean isRecording = false;
    private Handler timerHandler = new Handler();
    private AudioManager audioManager;
    
    private Button recordButton;
    private TextView statusText;
    private TextView timerText;
    
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            // 简单计时器逻辑
            timerHandler.postDelayed(this, 1000);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        recordButton = findViewById(R.id.recordButton);
        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        // 检查权限
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
        
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
    }
    
    private void startRecording() {
        try {
            // 请求音频焦点
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            
            // 启动服务
            Intent serviceIntent = new Intent(this, RecordingService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            isRecording = true;
            recordButton.setText("■ 停止记录");
            statusText.setText("🔴 记录中...");
            timerText.setVisibility(View.VISIBLE);
            timerText.setText("00:00");
            
            recordButton.setBackgroundResource(R.drawable.record_button_recording);
            
            // 启动计时器
            timerHandler.postDelayed(timerRunnable, 1000);
            
            // 最小化应用
            moveTaskToBack(true);
            Toast.makeText(this, "录音已开始，应用已最小化", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("StealthRecorder", "录音启动异常", e);
            Toast.makeText(this, "录音启动失败", Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopRecording() {
        // 停止服务
        Intent serviceIntent = new Intent(this, RecordingService.class);
        stopService(serviceIntent);
        
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        recordButton.setText("● 开始记录");
        statusText.setText("🟢 记录已保存");
        timerText.setVisibility(View.GONE);
        
        recordButton.setBackgroundResource(R.drawable.record_button_bg);
        
        Toast.makeText(this, "录音已保存", Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
