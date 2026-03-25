#!/bin/bash
# 更新gradle wrapper配置
cd "$(dirname "$0")"

echo "更新Gradle wrapper配置..."
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-all.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

echo "更新build.gradle..."
cat > build.gradle << 'EOF'
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '8.1.0' apply false
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
EOF

echo "更新完成"
echo "如果需要，提交并推送这些更改："