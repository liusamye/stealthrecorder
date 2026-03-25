package com.example.stealthrecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
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
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 202;
    private MediaRecorder mediaRecorder = null;
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler();
    
    private Button recordButton;
    private Button openFolderButton;
    private TextView statusText;
    private TextView timerText;
    private TextView fileInfoText;
    
    private String outputFile;
    private File currentRecordsDir; // 当前录音文件目录
    
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
        openFolderButton = findViewById(R.id.openFolderButton);
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
                String message = "📝 笔记助手使用说明：\n\n";
                message += "• 点击按钮开始/停止记录\n";
                message += "• 文件保存位置：\n";
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    // Android 11+
                    if (Environment.isExternalStorageManager()) {
                        message += "   内部存储/Recordings/\n";
                        message += "   ✅ 根目录，应用卸载时文件保留";
                    } else {
                        message += "   需要文件管理权限\n";
                        message += "   ⚠️ 首次录音时会提示授权\n";
                        message += "   授权后可保存到根目录";
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10
                    message += "   应用私有目录\n";
                    message += "   ⚠️ 应用卸载时会删除文件！";
                } else {
                    // Android 9及以下
                    message += "   内部存储/Recordings/\n";
                    message += "   ✅ 根目录，应用卸载时文件保留";
                }
                
                message += "\n\n• 点击📁按钮直接打开文件夹";
                
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                return true;
            }
        });
        
        // 打开文件夹按钮点击事件
        openFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRecordsFolder();
            }
        });
        
        // 检查是否已有录音文件夹
        checkExistingRecordsFolder();
    }
    
    private void checkAndRequestPermissions() {
        // 检查录音权限（必须）
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
        
        // 对于Android 10（API 29），请求存储权限
        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_STORAGE_PERMISSION);
            }
        }
        
        // 对于Android 11+，检查是否有管理所有文件的权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // 不在这里请求，在用户尝试录音时再请求
                Log.d("StealthRecorder", "需要文件管理权限");
            }
        }
    }
    
    private void startRecording() {
        try {
            // 创建输出文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_" + timeStamp + ".m4a";
            
            File recordsDir;
            String storageType = "根目录";
            
            // 优先尝试：保存到根目录简单路径
            // 路径：内部存储/Recordings/
            recordsDir = new File(Environment.getExternalStorageDirectory(), "Recordings");
            
            // 检查是否有权限写入根目录
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android 11+ 需要特殊权限
                if (!Environment.isExternalStorageManager()) {
                    storageType = "应用私有目录（需要授权）";
                    recordsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes");
                    
                    // 提示用户授权
                    Toast.makeText(this, 
                        "需要文件管理权限保存到根目录\n请点击确定授权", 
                        Toast.LENGTH_LONG).show();
                    
                    // 跳转到设置页面请求权限
                    requestManageExternalStoragePermission();
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10：使用应用私有目录
                storageType = "应用私有目录";
                recordsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes");
            }
            // Android 9及以下：可以直接写入根目录
            
            if (!recordsDir.exists()) {
                boolean created = recordsDir.mkdirs();
                if (!created) {
                    Toast.makeText(this, "无法创建目录: " + recordsDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // 检查目录是否可写
            if (!recordsDir.canWrite()) {
                Toast.makeText(this, "目录不可写，请检查存储权限", Toast.LENGTH_LONG).show();
                return;
            }
            
            outputFile = new File(recordsDir, fileName).getAbsolutePath();
            currentRecordsDir = recordsDir; // 保存目录引用
            
            // 显示打开文件夹按钮
            openFolderButton.setVisibility(View.VISIBLE);
            
            // 调试信息
            Log.d("StealthRecorder", "开始录音，文件路径: " + outputFile);
            Log.d("StealthRecorder", "存储类型: " + storageType);
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
            fileInfoText.setText("文件: " + fileName + "\n位置: " + storageType);
            
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
                String parentDir = new File(outputFile).getParent();
                String message = "✅ 笔记已保存: " + fileName + "\n";
                
                // 根据存储位置给出不同的提示
                if (parentDir.contains("Recordings")) {
                    message += "位置: 内部存储/Recordings/\n";
                    message += "✅ 根目录，安全保存";
                } else if (parentDir.contains("Android/data")) {
                    message += "位置: 应用私有目录\n";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        message += "⚠️ 需要文件管理权限保存到根目录";
                    } else {
                        message += "⚠️ 应用卸载时会删除文件！";
                    }
                } else {
                    message += "位置: " + parentDir + "\n";
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                
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
    
    /**
     * 打开录音文件夹
     */
    private void openRecordsFolder() {
        if (currentRecordsDir == null || !currentRecordsDir.exists()) {
            Toast.makeText(this, "录音文件夹不存在或尚未创建", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(currentRecordsDir);
            
            // 设置URI和类型
            intent.setDataAndType(uri, "resource/folder");
            
            // 对于Android 7.0+，尝试使用更兼容的方式
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setDataAndType(uri, "*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }
            
            // 创建选择器
            Intent chooser = Intent.createChooser(intent, "选择应用打开录音文件夹");
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                // 回退方案：显示路径
                showFolderPath();
            }
            
        } catch (Exception e) {
            Log.e("StealthRecorder", "打开文件夹失败: " + e.getMessage(), e);
            showFolderPath();
        }
    }
    
    /**
     * 显示文件夹路径（回退方案）
     */
    private void showFolderPath() {
        if (currentRecordsDir != null) {
            String path = currentRecordsDir.getAbsolutePath();
            Toast.makeText(this, 
                "请手动打开文件管理器并导航到：\n" + path, 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 请求管理所有文件的特殊权限（Android 11+）
     */
    private void requestManageExternalStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            } catch (Exception e) {
                // 回退方案：打开应用信息页面
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                
                Toast.makeText(this, 
                    "请手动开启'允许管理所有文件'权限", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "文件管理权限已授权", Toast.LENGTH_SHORT).show();
                    // 可以重新尝试录音
                } else {
                    Toast.makeText(this, "文件管理权限被拒绝", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    /**
     * 检查是否已有录音文件夹
     */
    private void checkExistingRecordsFolder() {
        // 优先检查根目录的Recordings文件夹
        File recordsDir = new File(Environment.getExternalStorageDirectory(), "Recordings");
        
        if (!recordsDir.exists() || !recordsDir.isDirectory()) {
            // 检查旧版本的可能位置
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                // Android 9及以下：公共音乐目录
                recordsDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC), "Notes");
            } else {
                // Android 10+：应用私有目录
                recordsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes");
            }
        }
        
        // 检查文件夹是否存在且有文件
        if (recordsDir.exists() && recordsDir.isDirectory()) {
            File[] files = recordsDir.listFiles();
            if (files != null && files.length > 0) {
                currentRecordsDir = recordsDir;
                openFolderButton.setVisibility(View.VISIBLE);
                Log.d("StealthRecorder", "发现已有录音文件夹，包含 " + files.length + " 个文件");
            }
        }
    }
}