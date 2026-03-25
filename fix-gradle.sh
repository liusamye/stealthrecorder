#!/bin/bash
echo "修复Gradle配置..."

# 删除有问题的wrapper文件
rm -f gradlew
rm -f gradle/wrapper/gradle-wrapper.jar

# 下载正确的gradlew
curl -s https://raw.githubusercontent.com/gradle/gradle/master/gradlew -o gradlew
chmod +x gradlew

# 创建正确的gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

echo "Gradle配置修复完成"
echo "注意：gradle-wrapper.jar会在首次运行时自动下载"