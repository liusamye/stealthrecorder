#!/bin/bash
echo "更新应用图标..."

# 源图片路径
SOURCE_IMAGE="/root/.openclaw/media/inbound/8fbd3861-c2cf-4e0c-851e-4ef9eb1e6da4.jpg"

# 检查源图片是否存在
if [ ! -f "$SOURCE_IMAGE" ]; then
    echo "错误：源图片不存在: $SOURCE_IMAGE"
    exit 1
fi

echo "源图片: $SOURCE_IMAGE"
echo "图片大小: $(du -h "$SOURCE_IMAGE" | cut -f1)"

# Android图标尺寸要求（像素）
# mdpi: 48x48
# hdpi: 72x72  
# xhdpi: 96x96
# xxhdpi: 144x144
# xxxhdpi: 192x192

# 创建不同尺寸的图标
echo "创建不同分辨率的图标..."

# 如果ImageMagick可用，创建优化版本
if command -v convert &> /dev/null; then
    echo "使用ImageMagick创建优化图标..."
    
    # mdpi (48x48)
    convert "$SOURCE_IMAGE" -resize 48x48^ -gravity center -extent 48x48 \
        -quality 100 app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    # hdpi (72x72)
    convert "$SOURCE_IMAGE" -resize 72x72^ -gravity center -extent 72x72 \
        -quality 100 app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    # xhdpi (96x96)
    convert "$SOURCE_IMAGE" -resize 96x96^ -gravity center -extent 96x96 \
        -quality 100 app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    # xxhdpi (144x144)
    convert "$SOURCE_IMAGE" -resize 144x144^ -gravity center -extent 144x144 \
        -quality 100 app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    # xxxhdpi (192x192)
    convert "$SOURCE_IMAGE" -resize 192x192^ -gravity center -extent 192x192 \
        -quality 100 app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
    
    echo "优化图标创建完成"
else
    echo "ImageMagick不可用，直接复制图片..."
    # 直接复制到所有目录（Android会自动处理缩放）
    cp "$SOURCE_IMAGE" app/src/main/res/mipmap-mdpi/ic_launcher.jpg 2>/dev/null || true
    cp "$SOURCE_IMAGE" app/src/main/res/mipmap-hdpi/ic_launcher.jpg 2>/dev/null || true
    cp "$SOURCE_IMAGE" app/src/main/res/mipmap-xhdpi/ic_launcher.jpg 2>/dev/null || true
    cp "$SOURCE_IMAGE" app/src/main/res/mipmap-xxhdpi/ic_launcher.jpg 2>/dev/null || true
    cp "$SOURCE_IMAGE" app/src/main/res/mipmap-xxxhdpi/ic_launcher.jpg 2>/dev/null || true
    
    echo "注意：直接复制了JPEG文件，可能需要手动转换为PNG"
fi

echo "图标更新完成"
echo "请检查以下文件："
find app/src/main/res -name "ic_launcher.*" -type f | xargs ls -la