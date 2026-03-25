#!/bin/bash
# 简化构建脚本
echo "=== 简化构建脚本 ==="

# 设置环境
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/usr/lib/android-sdk

echo "1. 检查环境..."
java -version
which aapt2

echo "2. 清理旧构建..."
rm -rf app/build

echo "3. 尝试直接构建..."
# 使用gradle wrapper
./gradlew clean assembleDebug --stacktrace --info

echo "4. 查找生成的APK..."
find . -name "*.apk" -type f 2>/dev/null

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ APK生成成功: app/build/outputs/apk/debug/app-debug.apk"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo "❌ APK未生成"
    echo "检查构建日志..."
fi