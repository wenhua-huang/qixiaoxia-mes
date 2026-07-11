---
description: 生产环境发布 - 本机构建前端/app -> 服务器拉代码/编译后端/重启 -> 上传前端/app -> 重载 Nginx
---

# 生产环境发布

发布流程：本机构建前端 -> 服务器拉代码 -> 编译后端 -> 重启 -> 上传前端 -> 重载 Nginx
（移动端 app H5 单独打包部署，见步骤 5；仅当 `app/` 有变更时才需要）

## 前置条件

- 本机可 SSH 连接服务器（已配置 SSH 密钥免密登录：`ssh qxx`）
- 服务器 Docker 容器正常运行（MySQL:3307, Redis:6380, MinIO:9010）
- **服务器仅 1.8GB 内存**：前端构建必须在本机完成，禁止在服务器跑 `vite build`；后端重启必须带内存限制参数（否则 OOM Kill，见故障排查）

## 发布步骤

### 1. 本机构建前端

```bash
cd /Users/huangwenhua/company/self/qixiaoxia-mes/frontend && npx vite build
```

### 2. 服务器端：拉代码 -> 编译 -> 重启后端

```bash
ssh qxx << 'EOF'
cd /var/www/qixiaoxia-mes && git pull origin main

# 编译（必须用 JDK 17）- Flyway 在应用启动时自动执行 db/migration/*.sql，无需手动跑 SQL
export JAVA_HOME=/usr/lib/jvm/java-17-alibaba-dragonwell-17.0.9.0.10.9-1.al8.x86_64
export PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn clean package -pl ruoyi-admin -am -DskipTests -Dcheckstyle.skip=true -q

# 重启（⚠️ 必须带 -Xmx512m 内存限制，1.8GB 服务器否则 OOM Kill）
kill $(lsof -ti :8081) 2>/dev/null; sleep 2
nohup java -Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=192m \
  -XX:+HeapDumpOnOutOfMemoryError -XX:+UseG1GC -Duser.timezone=Asia/Shanghai \
  -jar /var/www/qixiaoxia-mes/backend/ruoyi-admin/target/ruoyi-admin.jar \
  --server.port=8081 --ruoyi.profile=/var/www/qixiaoxia-mes/uploadPath \
  > /tmp/qxx-backend.log 2>&1 &
EOF
```

> 启动后等 ~10-30 秒 Spring Boot 起来。确认：`ssh qxx 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/'` 应为 200。查看 Flyway 迁移日志：`ssh qxx 'grep -iE "flyway|Successfully applied" /tmp/qxx-backend.log | tail -5'`。

### 3. 本机上传前端 + 重载 Nginx

```bash
scp -r /Users/huangwenhua/company/self/qixiaoxia-mes/frontend/dist/* \
  qxx:/var/www/qixiaoxia-mes/frontend/dist/

ssh qxx 'nginx -s reload'
```

### 4. 验证（PC 前端 + 后端）

> ⚠️ 下面的 `localhost` 是**服务器**的 localhost，需通过 `ssh qxx '...'` 执行。

```bash
ssh qxx 'TOKEN=$(curl -s -X POST http://localhost:8081/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"code\":\"\",\"uuid\":\"\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)[\"token\"])")

# 后端直连
curl -s -o /dev/null -w "后端直连: %{http_code}\n" http://localhost:8081/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"
# Nginx 代理
curl -s -o /dev/null -w "Nginx代理: %{http_code}\n" http://localhost/prod-api/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"
# 前端页面
curl -s -o /dev/null -w "前端页面: %{http_code}\n" http://localhost/'
```

期望全部返回 `200`。

### 5. 移动端 app（uni-app H5）打包与部署

**仅当 `app/` 目录有代码变更时才需要**（如本次报工页 report.vue/history.vue/feedback.js）。无变更可跳过。

#### 5.1 HBuilderX 打包（必须 GUI，不能用 CLI）

app 是 uni-app，打 H5 生产包**必须用 HBuilderX**（发行 -> 网站-H5 手机版）。`npm run build:h5` / `uni build` 产物不完整（只编译进 `@vue/shared` runtime，21 个页面组件、main.js 全部丢失），**不要用 CLI**。

1. HBuilderX 打开 `app/` 项目
2. 菜单：发行 -> 网站-H5 手机版
3. 产物路径：`app/unpackage/dist/build/web`（注意是 `web` 不是 `h5`）

#### 5.2 产物完整性验证（防 easycom 失效的"纯文字"产物）

HBuilderX build 阶段 easycom 可能失效，产物"看着完整"实则 uni-ui 组件定义全缺，页面只渲染纯文字。打包后必须 grep 验证：

```bash
cd /Users/huangwenhua/company/self/qixiaoxia-mes
# uni-grid CSS 引用应 >0（修复前 0）；主包 index.js 应 ~550KB（修复前 363KB）
grep -roc 'uni-grid' app/unpackage/dist/build/web/assets/*.css | awk -F: '{s+=$2} END {print "uni-grid CSS 引用:", s}'
ls -la app/unpackage/dist/build/web/assets/index-*.js | awk '{print "主包大小:", $5}'
```

#### 5.3 部署到服务器

Nginx 已配置 `location /app/ { alias .../app/dist/; try_files $uri $uri/ /app/index.html; }`，静态文件更新无需 reload。访问 `http://115.29.234.204/app/`，API 走同域 `/prod-api/` -> 后端 8081。

```bash
# 清空旧产物（避免旧 chunk 残留），再上传新产物
ssh qxx 'rm -rf /var/www/qixiaoxia-mes/app/dist/*'
scp -r /Users/huangwenhua/company/self/qixiaoxia-mes/app/unpackage/dist/build/web/* \
  qxx:/var/www/qixiaoxia-mes/app/dist/

# 验证
ssh qxx 'curl -s -o /dev/null -w "app页面: %{http_code}\n" http://localhost/app/'
```

期望返回 `200`。

## 故障排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 后端启动后立即退出、日志 0 行 | OOM Kill（1.8GB 服务器，JVM 堆/Metaspace 无界） | 重启命令必须带 `-Xmx512m -XX:MaxMetaspaceSize=192m`（见步骤 2）。项目自带 `ry.sh` 用 `-Xmx1024m` 也偏大，勿直接用 |
| `pkill -f ruoyi-admin.jar` 导致 SSH 退出码 255 | 命令行含 jar 名，pkill 匹配杀掉 SSH 自身远程进程 | 改用 `kill $(lsof -ti :8081)` |
| 后端启动后立即退出（非 OOM） | Docker 容器未启动 | `docker start qxx-mysql qxx-redis qxx-minio` |
| Nginx 502 | 后端未就绪 | 等 `curl :8081` 返回 200 后再重载 |
| 前端 404 | dist/ 未上传或路径错误 | 确认 scp 目标路径 `/var/www/qixiaoxia-mes/frontend/dist/` |
| app 页面只渲染纯文字、无图标无分组 | HBuilderX build easycom 失效，uni-ui 组件未编译进产物 | 页面显式 import uni-ui 组件（含连带依赖）+ `app/utils/uni-ui-register.js` 全局注册，重新 HBuilderX 打包。详见记忆 `app-h5-build-deploy` |
| 登录验证码报错 | 服务器已关闭验证码 | `"uuid":""` 传空字符串 |
