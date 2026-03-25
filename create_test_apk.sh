#!/bin/bash
echo "创建极简测试APK..."
cd "$(dirname "$0")"

# 清理旧文件
rm -rf test_app
mkdir -p test_app

# 1. 创建基础目录结构
mkdir -p test_app/res/layout
mkdir -p test_app/res/values
mkdir -p test_app/assets
mkdir -p test_app/lib
mkdir -p test_app/META-INF

# 2. 创建AndroidManifest.xml
cat > test_app/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.test.stealthrecorder"
    android:versionCode="1"
    android:versionName="1.0">

    <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="34" />
    
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <application
        android:label="语音测试"
        android:icon="@drawable/ic_launcher"
        android:theme="@android:style/Theme.Light">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
EOF

# 3. 创建简单布局
cat > test_app/res/layout/main.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="录音测试应用"
        android:textSize="24sp"
        android:layout_gravity="center"
        android:layout_marginTop="50dp"/>

    <Button
        android:id="@+id/recordBtn"
        android:layout_width="200dp"
        android:layout_height="100dp"
        android:text="开始录音"
        android:textSize="18sp"
        android:layout_gravity="center"
        android:layout_marginTop="50dp"/>

    <TextView
        android:id="@+id/statusText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="准备就绪"
        android:textSize="16sp"
        android:layout_gravity="center"
        android:layout_marginTop="30dp"/>

</LinearLayout>
EOF

# 4. 创建字符串资源
cat > test_app/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">语音测试</string>
</resources>
EOF

# 5. 创建简单的classes.dex占位文件（实际需要编译Java代码）
echo "创建classes.dex占位文件..."
echo "NOTE: 这是一个占位文件，实际需要编译Java代码生成classes.dex" > test_app/classes.dex.txt

echo "基础APK结构创建完成"
echo "位置: $(pwd)/test_app/"
echo ""
echo "注意：这只是一个APK结构，需要编译Java代码和资源才能生成可安装的APK"
echo "建议通过SSH连接后使用Android Studio或完整构建工具"