#!/bin/bash
echo "直接构建APK（不使用gradle wrapper）..."
cd "$(dirname "$0")"

# 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 如果有gradle-home，使用它
if [ -d "gradle-home" ]; then
    echo "使用本地gradle"
    ./gradle-home/bin/gradle assembleDebug --no-daemon --stacktrace
else
    echo "错误：gradle未安装"
    echo "请先运行: wget https://services.gradle.org/distributions/gradle-8.5-bin.zip && unzip gradle-8.5-bin.zip"
    exit 1
fi