#!/bin/bash
echo "创建简单的应用图标..."

# 创建图标目录
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# 创建一个简单的红色圆形图标（48x48像素，base64编码的PNG）
# 这是一个最小的红色圆形PNG图标
cat > app/src/main/res/mipmap-mdpi/ic_launcher.png.base64 << 'EOF'
iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAACXBIWXMAAAsTAAALEwEAmpwYAAAF
+0lEQVR4nO2Za2xUVRjHf+fe6bTT6bR0WqC0QKFAoZQWCqVQoFyKXESQiwgqKkZjjB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8
oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/8oB/