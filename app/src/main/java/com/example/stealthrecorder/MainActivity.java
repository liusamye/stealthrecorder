package com.example.stealthrecorder;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
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
    private MediaRecorder mediaRecorder = null;
    private boolean isRecording = false;
    
    private Button recordButton;
    private TextView statusText;
    
    private String outputFile;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        recordButton = findViewById(R.id.recordButton);
        statusText = findViewById(R.id.statusText);
        
        // 检查权限（原生API）
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
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "RECORDING_" + timeStamp + ".mp3";
            
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (storageDir != null) {
                outputFile = new File(storageDir, fileName).getAbsolutePath();
            } else {
                outputFile = new File(getFilesDir(), fileName).getAbsolutePath();
            }
            
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
            recordButton.setText("停止录音");
            statusText.setText("录音中... 文件: " + fileName);
            
            // 尝试最小化状态栏显示（基础版本）
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE);
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "录音启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                isRecording = false;
                recordButton.setText("开始录音");
                statusText.setText("录音已保存: " + outputFile);
                
                Toast.makeText(this, "录音已保存", Toast.LENGTH_SHORT).show();
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(this, "录音停止失败", Toast.LENGTH_SHORT).show();
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