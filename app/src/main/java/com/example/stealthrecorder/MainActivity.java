package com.example.stealthrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
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
    private TextView fileInfoText;
    private Button openFolderButton;
    
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
        fileInfoText = findViewById(R.id.fileInfoText);
        openFolderButton = findViewById(R.id.openFolderButton);
        
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
                    showRecordingWarningDialog();
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
        
        // 打开文件夹按钮
        openFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRecordingsFolder();
            }
        });
        
        // 检查服务是否正在运行（Activity恢复时）
        // 当用户从通知点击返回时，需要更新UI状态
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
                } else if (id == R.id.menu_website) {
                    openWebsite();
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
        intent.setData(Uri.parse("mailto:liusamye@163.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "StealthRecorder 反馈");
        intent.putExtra(Intent.EXTRA_TEXT, "请在此处写下您的反馈、建议或遇到的问题：\n\n");
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.menu_feedback)));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_email_failed), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showAbout() {
        // 使用自定义布局创建关于对话框
        View aboutView = getLayoutInflater().inflate(R.layout.dialog_recording_warning, null);
        TextView messageText = aboutView.findViewById(R.id.dialog_message);
        CheckBox dontShowCheckbox = aboutView.findViewById(R.id.dont_show_checkbox);
        
        // 隐藏勾选框
        dontShowCheckbox.setVisibility(View.GONE);
        
        // 设置关于信息
        String versionName = "1.3";
        String aboutMessage = String.format(getString(R.string.about_version), versionName) + 
                             "\n\n开发者: liusamye" +
                             "\n\n一款简洁高效的后台录音应用，支持折叠屏优化。";
        
        messageText.setText(aboutMessage);
        
        // 检查调试模式状态
        SharedPreferences debugPrefs = getSharedPreferences("debug_settings", Context.MODE_PRIVATE);
        boolean isDebugMode = debugPrefs.getBoolean("hide_notification_mode", false);
        
        // 根据调试模式状态设置标题
        String aboutTitle = getString(R.string.about_title);
        if (isDebugMode) {
            aboutTitle = aboutTitle + "...";  // 调试模式下加三个点
        }
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(aboutTitle);
        builder.setView(aboutView);
        
        // 添加点击监听器用于隐藏功能
        final int[] clickCount = {0};
        final long[] lastClickTime = {0};
        
        messageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis();
                
                // 检查是否在短时间内连续点击
                if (currentTime - lastClickTime[0] < 1000) { // 1秒内
                    clickCount[0]++;
                    
                    // 检查是否连续点击了3次
                    if (clickCount[0] >= 3) {
                        // 切换调试模式
                        SharedPreferences prefs = getSharedPreferences("debug_settings", Context.MODE_PRIVATE);
                        boolean debugMode = prefs.getBoolean("hide_notification_mode", false);
                        boolean newDebugMode = !debugMode;
                        
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("hide_notification_mode", newDebugMode);
                        editor.apply();
                        
                        // 重置点击计数
                        clickCount[0] = 0;
                        
                        // 静默切换，不显示任何提示
                        LogUtil.d("调试模式已" + (newDebugMode ? "开启" : "关闭"));
                    }
                } else {
                    // 重置点击计数
                    clickCount[0] = 1;
                }
                
                lastClickTime[0] = currentTime;
            }
        });
        
        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void openSettings() {
        // 创建简单的应用设置对话框
        View settingsView = getLayoutInflater().inflate(R.layout.dialog_recording_warning, null);
        TextView messageText = settingsView.findViewById(R.id.dialog_message);
        CheckBox dontShowCheckbox = settingsView.findViewById(R.id.dont_show_checkbox);
        
        messageText.setText("应用设置");
        dontShowCheckbox.setText("显示录音警告对话框");
        
        // 读取当前设置
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean dontShowWarning = prefs.getBoolean("dont_show_recording_warning", false);
        dontShowCheckbox.setChecked(!dontShowWarning); // 反选：勾选=显示，不勾选=不显示
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置");
        builder.setView(settingsView);
        
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 保存用户选择
                boolean showWarning = dontShowCheckbox.isChecked();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("dont_show_recording_warning", !showWarning); // 反选保存
                editor.apply();
                
                if (showWarning) {
                    Toast.makeText(MainActivity.this, "已启用录音警告对话框", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "已禁用录音警告对话框", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        builder.setNeutralButton("系统设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 打开系统设置中的应用详情页
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });
        
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void openWebsite() {
        // 尝试多个端口
        String[] urls = {
            "http://124.70.136.239",
            "http://124.70.136.239:8080",
            "http://124.70.136.239:8888"
        };
        
        for (String url : urls) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                return;
            } catch (Exception e) {
                // 继续尝试下一个
            }
        }
        Toast.makeText(this, getString(R.string.toast_website_failed), Toast.LENGTH_SHORT).show();
    }
    
    private void openRecordingsFolder() {
        try {
            File recordsDir = new File(Environment.getExternalStorageDirectory(), getString(R.string.recording_folder));
            if (!recordsDir.exists()) {
                recordsDir.mkdirs();
            }
            
            LogUtil.d("尝试打开录音文件夹: " + recordsDir.getAbsolutePath());
            
            // 方法1：使用最可靠的方式 - 发送文件路径给文件管理器
            // 构建正确的文件夹路径
            String folderPath = recordsDir.getAbsolutePath();
            
            // 尝试多种Intent，确保能打开正确的文件夹
            
            // 方法1A：使用标准的文件管理器Intent（最兼容）
            Intent intent1 = new Intent(Intent.ACTION_VIEW);
            Uri uri1 = Uri.parse("file://" + folderPath);
            intent1.setDataAndType(uri1, "resource/folder");
            
            // 方法1B：使用DocumentsUI（Android 5.0+）
            Intent intent2 = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 使用DocumentsContract打开特定目录
                Uri uri2 = Uri.parse("content://com.android.externalstorage.documents/document/primary:" + 
                    getString(R.string.recording_folder).replace("/", "%2F"));
                intent2.setDataAndType(uri2, "vnd.android.document/directory");
            } else {
                intent2.setDataAndType(Uri.fromFile(recordsDir), "resource/folder");
            }
            
            // 方法1C：使用系统文件管理器（通用）
            Intent intent3 = new Intent(Intent.ACTION_VIEW);
            intent3.setData(Uri.parse("file://" + folderPath));
            
            // 方法2：备用方案 - 使用文档选择器
            Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fallbackIntent.setType("*/*");
            fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fallbackIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            
            // 方法3：备用方案 - 发送路径到剪贴板，让用户手动粘贴
            Intent clipboardIntent = new Intent(Intent.ACTION_SEND);
            clipboardIntent.setType("text/plain");
            clipboardIntent.putExtra(Intent.EXTRA_TEXT, folderPath);
            clipboardIntent.putExtra(Intent.EXTRA_SUBJECT, "录音文件夹路径");
            
            // 尝试第一种方法A
            if (intent1.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(intent1);
                    Toast.makeText(this, getString(R.string.toast_folder_opened), Toast.LENGTH_SHORT).show();
                    LogUtil.d("使用方法1A打开文件夹");
                    return;
                } catch (Exception e) {
                    LogUtil.e("方法1A失败", e);
                }
            }
            
            // 尝试第一种方法B
            if (intent2.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(intent2);
                    Toast.makeText(this, getString(R.string.toast_folder_opened), Toast.LENGTH_SHORT).show();
                    LogUtil.d("使用方法1B打开文件夹");
                    return;
                } catch (Exception e) {
                    LogUtil.e("方法1B失败", e);
                }
            }
            
            // 尝试第一种方法C
            if (intent3.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(intent3);
                    Toast.makeText(this, getString(R.string.toast_folder_opened), Toast.LENGTH_SHORT).show();
                    LogUtil.d("使用方法1C打开文件夹");
                    return;
                } catch (Exception e) {
                    LogUtil.e("方法1C失败", e);
                }
            }
            
            // 尝试第二种方法
            if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(fallbackIntent);
                    Toast.makeText(this, "请在文件管理器中选择录音文件夹", Toast.LENGTH_LONG).show();
                    LogUtil.d("使用方法2打开文件选择器");
                    return;
                } catch (Exception e) {
                    LogUtil.e("方法2失败", e);
                }
            }
            
            // 备用方案：复制路径到剪贴板并提示用户
            try {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("录音文件夹路径", folderPath);
                clipboard.setPrimaryClip(clip);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("打开录音文件夹")
                       .setMessage("已复制文件夹路径到剪贴板:\n\n" + folderPath + "\n\n请打开文件管理器并粘贴此路径。")
                       .setPositiveButton("确定", null)
                       .show();
                LogUtil.d("已复制路径到剪贴板: " + folderPath);
            } catch (Exception e) {
                // 最后手段：显示路径
                Toast.makeText(this, getString(R.string.file_info_saved, folderPath), Toast.LENGTH_LONG).show();
                LogUtil.d("显示路径: " + folderPath);
            }
            
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_folder_failed), Toast.LENGTH_SHORT).show();
            LogUtil.e("打开文件夹失败", e);
        }
    }
    
    private void showRecordingWarningDialog() {
        // 检查用户是否选择了"不再显示"
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean dontShowAgain = prefs.getBoolean("dont_show_recording_warning", false);
        
        if (dontShowAgain) {
            // 用户选择了不再显示，直接开始录音
            startRecording();
            return;
        }
        
        // 使用自定义布局创建对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_recording_warning, null);
        TextView messageText = dialogView.findViewById(R.id.dialog_message);
        CheckBox dontShowCheckbox = dialogView.findViewById(R.id.dont_show_checkbox);
        
        messageText.setText(getString(R.string.dialog_recording_warning));
        dontShowCheckbox.setText(getString(R.string.dialog_dont_show_again));
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.status_recording));
        builder.setView(dialogView);
        
        builder.setPositiveButton(getString(R.string.dialog_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 保存用户选择
                if (dontShowCheckbox.isChecked()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("dont_show_recording_warning", true);
                    editor.apply();
                    LogUtil.d("用户选择不再显示录音警告对话框");
                }
                
                // 用户确认，开始录音
                startRecording();
            }
        });
        
        builder.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户取消，不做任何操作
                dialog.dismiss();
            }
        });
        
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void checkServiceStatus() {
        // 使用静态变量检查服务状态
        // 这是最简单且没有权限问题的方法
        
        try {
            // 检查RecordingService的静态变量
            boolean serviceRunning = RecordingService.isServiceRunning();
            
            if (serviceRunning) {
                isRecording = true;
                updateUIForRecording();
                LogUtil.d("检测到录音服务正在运行（通过静态变量）");
            } else {
                isRecording = false;
                updateUIForRecording();
                LogUtil.d("录音服务未运行");
            }
            
        } catch (Exception e) {
            LogUtil.e("检查服务状态时出错", e);
            // 出错时默认设置为未录音
            isRecording = false;
            updateUIForRecording();
        }
    }
    
    private void updateUIForRecording() {
        if (isRecording) {
            recordButton.setText(getString(R.string.record_button_stop));
            statusText.setText(getString(R.string.status_recording));
            fileInfoText.setText(getString(R.string.file_info_recording));
            timerText.setVisibility(View.VISIBLE);
            recordButton.setBackgroundResource(R.drawable.record_button_recording);
            openFolderButton.setVisibility(View.GONE);
        } else {
            recordButton.setText(getString(R.string.record_button_start));
            statusText.setText(getString(R.string.status_ready));
            fileInfoText.setText(getString(R.string.file_info_default));
            timerText.setVisibility(View.GONE);
            recordButton.setBackgroundResource(R.drawable.record_button_bg);
            openFolderButton.setVisibility(View.GONE);
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
            
            // 显示明确的提示信息
            Toast.makeText(this, getString(R.string.toast_recording_started), Toast.LENGTH_LONG).show();
            
            // 延迟500ms后最小化应用，让用户看到提示
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 最小化应用
                    moveTaskToBack(true);
                    LogUtil.d( "应用已最小化");
                }
            }, 500);
            
        } catch (Exception e) {
            LogUtil.e( "录音启动异常", e);
            Toast.makeText(this, getString(R.string.error_recording_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopRecording() {
        // 停止服务
        Intent serviceIntent = new Intent(this, RecordingService.class);
        stopService(serviceIntent);
        
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        updateUIForRecording();
        statusText.setText(getString(R.string.status_saved));
        
        // 显示保存路径
        File recordsDir = new File(Environment.getExternalStorageDirectory(), getString(R.string.recording_folder));
        String savePath = recordsDir.getAbsolutePath();
        fileInfoText.setText(getString(R.string.file_info_saved, savePath));
        
        // 显示打开文件夹按钮
        openFolderButton.setVisibility(View.VISIBLE);
        
        Toast.makeText(this, getString(R.string.toast_recording_stopped, savePath), Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.permission_record_audio), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.permission_storage), Toast.LENGTH_LONG).show();
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
        // 暂时禁用，避免权限问题导致崩溃
        // checkServiceStatus();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
