package com.example.stealthrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
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
    private static final int REQUEST_STORAGE_PERMISSION = 201;
    private static final String TAG = "StealthRecorder";
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
        
        // 检查存储权限（Android 11+需要特殊处理）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要MANAGE_EXTERNAL_STORAGE权限
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            // Android 10及以下使用传统存储权限
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                        REQUEST_STORAGE_PERMISSION);
            }
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
        
        // 菜单按钮
        Button menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });
        
        // 检查服务是否正在运行（Activity恢复时）
        checkServiceStatus();
    }
    
    private void showMenu(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_donate) {
                    openDonation();
                    return true;
                } else if (id == R.id.menu_feedback) {
                    openFeedback();
                    return true;
                } else if (id == R.id.menu_about) {
                    showAbout();
                    return true;
                } else if (id == R.id.menu_settings) {
                    openSettings();
                    return true;
                }
                return false;
            }
        });
        
        popup.show();
    }
    
    private void openDonation() {
        Intent intent = new Intent(this, DonationActivity.class);
        startActivity(intent);
    }
    
    private void openFeedback() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"feedback@你的域名.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "StealthRecorder 反馈");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "未找到邮件应用", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showAbout() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("关于 StealthRecorder")
            .setMessage("版本: 1.0\n开发者: liusamye\n\n一款简洁高效的后台录音应用，支持折叠屏优化。")
            .setPositiveButton("确定", null)
            .show();
    }
    
    private void openSettings() {
        // 打开系统设置中的应用详情页
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
    
    private void checkServiceStatus() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RecordingService.class.getName().equals(service.service.getClassName())) {
                LogUtil.d( "检测到录音服务正在运行");
                isRecording = true;
                updateUIForRecording();
                return;
            }
        }
        LogUtil.d( "录音服务未运行");
    }
    
    private void updateUIForRecording() {
        if (isRecording) {
            recordButton.setText("■ 停止记录");
            statusText.setText("🔴 记录中...");
            timerText.setVisibility(View.VISIBLE);
            recordButton.setBackgroundResource(R.drawable.record_button_recording);
        } else {
            recordButton.setText("● 开始记录");
            statusText.setText("🟢 准备就绪");
            timerText.setVisibility(View.GONE);
            recordButton.setBackgroundResource(R.drawable.record_button_bg);
        }
    }
    
    private void startRecording() {
        try {
            LogUtil.d("=== 开始录音 ===");
            
            // 检查是否已经在录音
            if (isRecording) {
                LogUtil.w( "已经在录音中，忽略重复启动");
                Toast.makeText(this, "已经在录音中", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查权限
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                LogUtil.e( "录音权限未授予");
                Toast.makeText(this, "需要录音权限", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 检查存储权限（Android 11+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    LogUtil.e( "存储权限未授予（Android 11+）");
                    Toast.makeText(this, "需要所有文件访问权限", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    LogUtil.e( "存储权限未授予");
                    Toast.makeText(this, "需要存储权限", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            LogUtil.d( "权限检查通过");
            
            // 请求音频焦点
            int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            LogUtil.d( "音频焦点请求结果: " + result);
            
            // 启动服务
            Intent serviceIntent = new Intent(this, RecordingService.class);
            LogUtil.d( "创建服务Intent");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LogUtil.d( "启动前台服务");
                startForegroundService(serviceIntent);
            } else {
                LogUtil.d( "启动普通服务");
                startService(serviceIntent);
            }
            
            isRecording = true;
            updateUIForRecording();
            timerText.setText("00:00");
            
            // 启动计时器
            timerHandler.postDelayed(timerRunnable, 1000);
            
            // 最小化应用
            moveTaskToBack(true);
            LogUtil.d( "应用已最小化");
            Toast.makeText(this, "录音已开始，应用已最小化", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            LogUtil.e( "录音启动异常", e);
            Toast.makeText(this, "录音启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopRecording() {
        // 停止服务
        Intent serviceIntent = new Intent(this, RecordingService.class);
        stopService(serviceIntent);
        
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        updateUIForRecording();
        statusText.setText("🟢 记录已保存");
        
        Toast.makeText(this, "录音已保存", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要录音权限才能使用应用", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能保存录音", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isRecording", isRecording);
        LogUtil.d( "保存状态: isRecording=" + isRecording);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isRecording = savedInstanceState.getBoolean("isRecording", false);
        LogUtil.d( "恢复状态: isRecording=" + isRecording);
        if (isRecording) {
            updateUIForRecording();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到前台时检查服务状态
        checkServiceStatus();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
