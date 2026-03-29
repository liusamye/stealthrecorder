#!/bin/bash
echo "创建StealthRecorder签名证书..."
echo "⚠️ 注意：这只是一个示例证书，用于测试"
echo "正式发布时请使用强密码并妥善保管！"

# 检查keytool是否可用
which keytool > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "错误：keytool未安装，请安装Java JDK"
    exit 1
fi

# 创建证书
keytool -genkeypair -v \
  -keystore app/certs/stealthrecorder.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias key0 \
  -storepass 123456 \
  -keypass 123456 \
  -dname "CN=liusamye, OU=Personal Developer, O=Individual, L=Beijing, ST=Beijing, C=CN"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 证书创建成功！"
    echo "证书文件：app/certs/stealthrecorder.jks"
    echo "别名：key0"
    echo "密码：123456"
    echo "有效期：10000天"
    echo ""
    echo "⚠️ 重要提醒："
    echo "1. 备份证书文件到安全位置"
    echo "2. 正式发布时修改为强密码"
    echo "3. 不要将证书提交到GitHub"
    echo "4. 证书丢失将无法更新应用"
else
    echo "❌ 证书创建失败"
fi
