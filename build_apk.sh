#!/bin/bash
# 简化版APK构建脚本

echo "开始构建简化版APK..."
cd "$(dirname "$0")"

# 创建临时目录
rm -rf build_temp
mkdir -p build_temp

# 创建基本的APK结构
mkdir -p build_temp/res/layout
mkdir -p build_temp/res/values
mkdir -p build_temp/classes

# 复制资源文件
cp app/src/main/res/layout/activity_main.xml build_temp/res/layout/
cp app/src/main/res/values/*.xml build_temp/res/values/

# 创建简化的AndroidManifest.xml
cat > build_temp/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.stealthrecorder">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <application
        android:label="语音备忘录"
        android:theme="@android:style/Theme.Light">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

echo "APK结构创建完成"
echo "注意：这是一个简化版本，需要进一步处理才能生成可安装的APK"
echo "建议使用完整Android SDK构建"