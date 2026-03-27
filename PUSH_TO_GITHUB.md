# 推送到GitHub并启用自动构建

## 当前状态
代码已修改完成，提交已准备好。需要推送到GitHub仓库以触发自动构建。

## 推送方法选择

### 方法1：使用SSH密钥（推荐）
#### 步骤：
1. **检查是否有SSH密钥**：
   ```bash
   ls -la ~/.ssh/
   # 应该看到 id_rsa 和 id_rsa.pub 或 id_ed25519 和 id_ed25519.pub
   ```

2. **如果没有，生成SSH密钥**：
   ```bash
   ssh-keygen -t ed25519 -C "your_email@example.com"
   # 按Enter接受默认位置
   # 可以设置密码或留空
   ```

3. **添加公钥到GitHub**：
   ```bash
   cat ~/.ssh/id_ed25519.pub
   # 复制输出的内容
   ```
   - 访问 https://github.com/settings/keys
   - 点击"New SSH key"
   - 标题：例如 "My Computer"
   - 密钥：粘贴复制的公钥
   - 点击"Add SSH key"

4. **测试SSH连接**：
   ```bash
   ssh -T git@github.com
   # 应该看到：Hi liusamye! You've successfully authenticated...
   ```

5. **修改remote并推送**：
   ```bash
   cd /root/.openclaw/workspace/android-recorder
   git remote set-url origin git@github.com:liusamye/stealthrecorder.git
   git push origin main
   ```

### 方法2：使用GitHub CLI
#### 步骤：
1. **安装GitHub CLI**：
   - macOS: `brew install gh`
   - Linux: 查看 https://github.com/cli/cli#installation
   - Windows: 下载安装包

2. **登录**：
   ```bash
   gh auth login
   # 选择GitHub.com
   # 选择SSH或HTTPS
   # 按照提示完成登录
   ```

3. **推送**：
   ```bash
   cd /root/.openclaw/workspace/android-recorder
   git push origin main
   ```

### 方法3：使用HTTPS with token
#### 步骤：
1. **生成GitHub token**：
   - 访问 https://github.com/settings/tokens
   - 点击"Generate new token (classic)"
   - Note: "StealthRecorder Push"
   - 过期时间：选择"90 days"或"no expiration"
   - 权限：勾选"repo"
   - 点击"Generate token"
   - **立即复制token**（只显示一次！）

2. **推送**：
   ```bash
   cd /root/.openclaw/workspace/android-recorder
   git push https://<YOUR_TOKEN>@github.com/liusamye/stealthrecorder.git main
   # 将<YOUR_TOKEN>替换为实际的token
   ```

### 方法4：手动上传（备选）
如果以上方法都不可行：

1. **下载代码包**：
   - 文件：`/root/.openclaw/workspace/stealthrecorder-fixed.tar.gz`
   - 大小：118KB

2. **上传到GitHub**：
   - 访问 https://github.com/liusamye/stealthrecorder
   - 点击"Add file" → "Upload files"
   - 拖拽或选择 `stealthrecorder-fixed.tar.gz`
   - 解压后替换现有文件
   - 提交更改

## 验证自动构建

推送成功后：

1. **访问Actions页面**：
   - https://github.com/liusamye/stealthrecorder/actions

2. **查看构建状态**：
   - 应该看到"Android CI" workflow正在运行
   - 点击查看详细日志

3. **下载APK**：
   - 构建完成后，在"Artifacts"部分下载APK
   - 文件名：`stealth-recorder-apk`

## 故障排除

### 问题1：权限被拒绝
```
Permission denied (publickey).
```
**解决**：确保SSH密钥已正确添加到GitHub。

### 问题2：token无效
```
remote: Invalid username or password.
```
**解决**：重新生成token，确保复制完整。

### 问题3：Actions未触发
**解决**：
1. 检查仓库设置：Settings → Actions → General
2. 确保"Allow all actions"已启用
3. 手动触发：Actions → Android CI → Run workflow

### 问题4：构建失败
**解决**：
1. 查看构建日志中的错误信息
2. 检查代码是否有语法错误
3. 确保gradle配置正确

## 成功标志
1. ✅ GitHub Actions显示绿色勾号
2. ✅ 可以下载APK文件
3. ✅ APK可以在鸿蒙4.2设备上安装运行
4. ✅ 后台录音不再一分钟中断

## 紧急联系
如果遇到问题，请提供：
1. 错误信息的完整截图
2. 使用的推送方法
3. GitHub Actions的日志链接