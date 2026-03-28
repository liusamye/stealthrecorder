# 网站部署说明

## 网站文件结构
```
website/
├── index.html          # 首页
├── privacy.html        # 隐私政策页面
├── donate.html         # 打赏页面
└── DEPLOY.md          # 部署说明
```

## 部署步骤

### 方案1：使用Nginx（推荐）

1. **安装Nginx**
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install nginx
   
   # CentOS/RHEL
   sudo yum install nginx
   ```

2. **配置网站**
   ```bash
   # 创建网站目录
   sudo mkdir -p /var/www/stealthrecorder
   
   # 复制网站文件
   sudo cp -r website/* /var/www/stealthrecorder/
   
   # 设置权限
   sudo chown -R www-data:www-data /var/www/stealthrecorder
   sudo chmod -R 755 /var/www/stealthrecorder
   ```

3. **创建Nginx配置文件**
   ```bash
   sudo nano /etc/nginx/sites-available/stealthrecorder
   ```

   添加以下内容：
   ```nginx
   server {
       listen 80;
       server_name 你的域名.com www.你的域名.com;
       
       root /var/www/stealthrecorder;
       index index.html;
       
       # 启用gzip压缩
       gzip on;
       gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
       
       # 安全头
       add_header X-Frame-Options "SAMEORIGIN" always;
       add_header X-Content-Type-Options "nosniff" always;
       add_header X-XSS-Protection "1; mode=block" always;
       
       location / {
           try_files $uri $uri/ =404;
       }
       
       # 缓存静态资源
       location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
           expires 1y;
           add_header Cache-Control "public, immutable";
       }
   }
   ```

4. **启用网站并重启Nginx**
   ```bash
   sudo ln -s /etc/nginx/sites-available/stealthrecorder /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl restart nginx
   ```

### 方案2：使用Apache

1. **安装Apache**
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install apache2
   
   # CentOS/RHEL
   sudo yum install httpd
   ```

2. **部署网站**
   ```bash
   # 复制网站文件
   sudo cp -r website/* /var/www/html/
   
   # 设置权限
   sudo chown -R www-data:www-data /var/www/html
   sudo chmod -R 755 /var/www/html
   ```

3. **重启Apache**
   ```bash
   # Ubuntu/Debian
   sudo systemctl restart apache2
   
   # CentOS/RHEL
   sudo systemctl restart httpd
   ```

### 方案3：使用GitHub Pages（免费）

1. **创建GitHub仓库**
   - 新建仓库：`stealthrecorder-website`
   - 设置为公开仓库

2. **上传网站文件**
   ```bash
   cd website
   git init
   git add .
   git commit -m "Initial website"
   git branch -M main
   git remote add origin https://github.com/你的用户名/stealthrecorder-website.git
   git push -u origin main
   ```

3. **启用GitHub Pages**
   - 进入仓库设置 → Pages
   - 选择分支：`main`
   - 选择文件夹：`/(root)`
   - 保存

4. **访问网站**
   - 地址：`https://你的用户名.github.io/stealthrecorder-website`

### 方案4：使用Vercel/Netlify（免费，推荐）

1. **注册账号**
   - Vercel: https://vercel.com
   - Netlify: https://netlify.com

2. **导入GitHub仓库**
   - 连接GitHub账号
   - 选择仓库
   - 自动部署

3. **自定义域名（可选）**
   - 在设置中添加自定义域名
   - 配置DNS记录

## 域名配置

### 1. 购买域名
- 推荐：阿里云、腾讯云、Namecheap、GoDaddy

### 2. 配置DNS
```
记录类型   名称       值
A          @         服务器IP地址
A          www       服务器IP地址
CNAME      *         你的域名.com
```

### 3. SSL证书（HTTPS）
```bash
# 使用Let's Encrypt（免费）
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d 你的域名.com -d www.你的域名.com
```

## 更新网站

### 手动更新
```bash
# 上传新文件到服务器
scp -r website/* 用户名@服务器IP:/var/www/stealthrecorder/
```

### 自动更新（使用Git）
```bash
# 在服务器上设置Git仓库
cd /var/www/stealthrecorder
git init
git remote add origin https://github.com/你的用户名/stealthrecorder-website.git

# 更新时拉取最新代码
git pull origin main
```

## 测试网站
1. 访问 `http://你的域名.com`
2. 检查所有页面链接
3. 测试移动端显示
4. 验证表单功能（如果有）

## 监控和维护
1. **日志监控**
   ```bash
   # Nginx访问日志
   tail -f /var/log/nginx/access.log
   
   # 错误日志
   tail -f /var/log/nginx/error.log
   ```

2. **性能监控**
   - 使用Google PageSpeed Insights
   - 使用GTmetrix

3. **定期备份**
   ```bash
   # 备份网站文件
   tar -czf stealthrecorder-backup-$(date +%Y%m%d).tar.gz /var/www/stealthrecorder
   
   # 备份数据库（如果有）
   mysqldump -u 用户名 -p 数据库名 > backup.sql
   ```

## 故障排除

### 常见问题
1. **403 Forbidden**
   ```bash
   # 检查文件权限
   sudo chmod -R 755 /var/www/stealthrecorder
   sudo chown -R www-data:www-data /var/www/stealthrecorder
   ```

2. **404 Not Found**
   ```bash
   # 检查Nginx配置
   sudo nginx -t
   sudo systemctl restart nginx
   ```

3. **SSL证书问题**
   ```bash
   # 更新证书
   sudo certbot renew
   ```

### 联系支持
- 邮箱：liusamye@163.com
- GitHub：https://github.com/liusamye/stealthrecorder