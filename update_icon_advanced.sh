#!/bin/bash
echo "=== 高级图标更新工具 ==="
echo "用于更新应用图标到新图片"

# 检查ImageMagick是否可用
if ! command -v convert &> /dev/null; then
    echo "错误：需要安装ImageMagick"
    echo "运行: sudo apt-get install imagemagick"
    exit 1
fi

# 询问用户新图标路径
echo ""
echo "请提供新图标文件的完整路径："
echo "例如：/root/.openclaw/media/inbound/新图标.jpg"
read -p "图标路径: " ICON_PATH

# 检查文件是否存在
if [ ! -f "$ICON_PATH" ]; then
    echo "错误：文件不存在: $ICON_PATH"
    echo "请检查路径并重新运行脚本"
    exit 1
fi

# 检查文件类型
FILE_TYPE=$(file -b --mime-type "$ICON_PATH")
if [[ ! "$FILE_TYPE" =~ ^image/ ]]; then
    echo "错误：不是有效的图片文件: $FILE_TYPE"
    exit 1
fi

echo ""
echo "新图标信息："
echo "- 路径: $ICON_PATH"
echo "- 类型: $FILE_TYPE"
echo "- 大小: $(du -h "$ICON_PATH" | cut -f1)"

# Android图标尺寸要求
echo ""
echo "Android图标尺寸要求："
echo "- mdpi:   48x48 像素"
echo "- hdpi:   72x72 像素"
echo "- xhdpi:  96x96 像素"
echo "- xxhdpi: 144x144 像素"
echo "- xxxhdpi: 192x192 像素"

# 备份当前图标
echo ""
echo "备份当前图标..."
BACKUP_DIR="icon_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp app/src/main/res/mipmap-*/ic_launcher.png "$BACKUP_DIR/" 2>/dev/null || true
echo "图标已备份到: $BACKUP_DIR/"

# 创建新图标
echo ""
echo "创建新图标..."

# mdpi (48x48)
echo "创建 mdpi (48x48)..."
convert "$ICON_PATH" -resize 48x48^ -gravity center -extent 48x48 \
    -quality 100 app/src/main/res/mipmap-mdpi/ic_launcher.png

# hdpi (72x72)
echo "创建 hdpi (72x72)..."
convert "$ICON_PATH" -resize 72x72^ -gravity center -extent 72x72 \
    -quality 100 app/src/main/res/mipmap-hdpi/ic_launcher.png

# xhdpi (96x96)
echo "创建 xhdpi (96x96)..."
convert "$ICON_PATH" -resize 96x96^ -gravity center -extent 96x96 \
    -quality 100 app/src/main/res/mipmap-xhdpi/ic_launcher.png

# xxhdpi (144x144)
echo "创建 xxhdpi (144x144)..."
convert "$ICON_PATH" -resize 144x144^ -gravity center -extent 144x144 \
    -quality 100 app/src/main/res/mipmap-xxhdpi/ic_launcher.png

# xxxhdpi (192x192)
echo "创建 xxxhdpi (192x192)..."
convert "$ICON_PATH" -resize 192x192^ -gravity center -extent 192x192 \
    -quality 100 app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# 验证创建结果
echo ""
echo "验证新图标："
for dir in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    FILE="app/src/main/res/mipmap-$dir/ic_launcher.png"
    if [ -f "$FILE" ]; then
        SIZE=$(identify -format "%wx%h" "$FILE" 2>/dev/null || echo "未知")
        echo "- $dir: $SIZE ($(du -h "$FILE" | cut -f1))"
    else
        echo "- $dir: 创建失败"
    fi
done

echo ""
echo "=== 图标更新完成 ==="
echo "新图标已应用到所有DPI级别"
echo "请重新构建应用以查看效果"
echo ""
echo "如果需要恢复旧图标，文件在: $BACKUP_DIR/"