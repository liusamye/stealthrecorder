# StealthRecorder - 鸿蒙4.2后台录音修复版

## 问题描述
在华为鸿蒙4.2系统上，录音应用进入后台后，在一分钟整时录音中断。

## 解决方案
已修改代码添加前台服务，解决鸿蒙后台限制问题。

## 修改内容

### 1. 新增文件
- `RecordingService_fixed.java` - 前台录音服务（纯原生API，无AndroidX依赖）
- `MainActivity_simple.java` - 简化版主界面，使用前台服务

### 2. 修改文件
- `AndroidManifest.xml` - 添加前台服务权限和声明
- `build.gradle` - 移除AndroidX依赖，使用纯原生API
- `settings.gradle` - 简化配置，兼容旧gradle版本

### 3. 核心改进
1. **前台服务**：使用`startForeground()`创建持续通知
2. **WakeLock**：防止CPU休眠
3. **START_STICKY**：服务被杀死后自动重启
4. **鸿蒙适配**：使用纯原生API，最大兼容性

## 构建方法

### 方法1：Android Studio（最简单）
1. 下载本项目代码
2. 用Android Studio打开
3. 点击 Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK位置：`app/build/outputs/apk/debug/app-debug.apk`

### 方法2：命令行（需要gradle 8.5+）
```bash
# 下载gradle 8.5
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip
unzip gradle-8.5-bin.zip

# 构建APK
./gradle-8.5/bin/gradle assembleDebug

# APK位置
ls -la app/build/outputs/apk/debug/
```

### 方法3：GitHub Actions自动构建
1. 推送代码到GitHub
2. Actions会自动构建
3. 下载构建产物中的APK

## 文件说明

### 必须文件
```
app/src/main/java/com/example/stealthrecorder/MainActivity_simple.java
app/src/main/java/com/example/stealthrecorder/RecordingService_fixed.java
app/src/main/AndroidManifest.xml
app/build.gradle
settings.gradle
```

### 资源文件
```
app/src/main/res/ - 所有资源文件
app/src/main/AndroidManifest.xml - 清单文件
```

## 测试要点
1. 在鸿蒙4.2设备上测试
2. 启动录音后切到后台
3. 观察是否还会一分钟中断
4. 检查通知栏是否有持续通知

## 已知问题
- 需要用户手动授权后台权限（鸿蒙系统限制）
- 通知可能被系统优化关闭（需在设置中允许）

## 紧急构建
如果急需APK，建议：
1. 使用Android Studio快速构建
2. 或找一台有Android开发环境的电脑
3. 或使用在线构建服务（如GitHub Actions）

## 联系方式
如有构建问题，请提供具体错误信息。