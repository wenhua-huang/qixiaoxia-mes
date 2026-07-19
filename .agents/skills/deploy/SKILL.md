---
name: deploy
description: Use to publish this project to the production server (115.29.234.204). Triggers on "deploy", "发布", "上线", "发布到生产", "发布生产", "ship to prod". Runs: build frontend locally → ssh to server → git pull → mvn build backend → restart backend → scp dist → reload nginx → verify all endpoints return 200. Also covers optional mobile app (uni-app) upload if the user just built it in HBuilder. Reads server credentials and paths from root AGENTS.md.
---

# 生产环境发布

发布流程：本机构建前端 → 服务器拉代码 → 编译后端 → 重启 → 上传前端 → 重载 Nginx →（可选）上传移动端 app。

## 前置条件

- **SSH 免密已配**：`~/.ssh/config` 含 `Host qxx` 别名 → `115.29.234.204`（root，密钥 `~/.ssh/id_ed25519`）。本 skill 所有 ssh/scp 均用 `qxx` 别名，无需密码
- 服务器 Docker 容器正常运行（MySQL:3307, Redis:6380, MinIO:9010）
- **服务器仅 1.8GB 内存，前端构建必须在本机完成，禁止在服务器跑 `vite build`**
- 项目根路径：`/Users/huangwenhua/company/self/qixiaoxia-mes`（本机，main 分支）与 `/var/www/qixiaoxia-mes`（服务器）

## 发布范围决策

发布时主动问用户："本次发布要包含哪些？" 选项：
- **PC 前端 + 后端**（默认）：步骤 1-4
- **移动端 app**：步骤 5（独立，可与上面任意组合）
- **全量**：1-5 全做

不问就默认走 1-4。

## 发布步骤

### 1. 本机构建前端

```bash
cd /Users/huangwenhua/company/self/qixiaoxia-mes/frontend && npx vite build
```

### 2. 服务器端：拉代码 → 编译 → 重启后端

```bash
ssh qxx << 'EOF'
cd /var/www/qixiaoxia-mes && git pull origin main

# 编译（必须用 JDK 17）— Flyway 在应用启动时自动执行 db/migration/*.sql
export JAVA_HOME=/usr/lib/jvm/java-17-alibaba-dragonwell-17.0.9.0.10.9-1.al8.x86_64
export PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn clean package -pl ruoyi-admin -am -DskipTests -Dcheckstyle.skip=true -q

# 重启（⚠️ 服务器仅 1.8GB 内存，必须限制全部 JVM 内存区，否则 OOM Killer 会杀进程）
# ⚠️ 关键：仅 -Xmx 不够！Metaspace + DirectMemory + 线程栈会让总虚拟内存飙到 3.8GB，仍触发全局 OOM
pkill -f 'org.codehaus.plexus.classworlds.launcher.Launcher' 2>/dev/null  # 清 Maven 残留进程
sleep 2
kill $(lsof -ti :8081) 2>/dev/null; sleep 2
nohup java -Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+UseG1GC \
  -jar /var/www/qixiaoxia-mes/backend/ruoyi-admin/target/ruoyi-admin.jar \
  --server.port=8081 --ruoyi.profile=/var/www/qixiaoxia-mes/uploadPath \
  > /tmp/qxx-backend.log 2>&1 &

# 等待就绪（最多 150s，Flyway 迁移 + Spring 启动约需 20-40s）
for i in $(seq 1 150); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/login -X POST -H "Content-Type: application/json" -d '{}' 2>/dev/null)
  if [ "$code" != "000" ] && [ -n "$code" ]; then echo "后端就绪 HTTP $code (${i}s)"; break; fi
  if ! kill -0 $! 2>/dev/null; then echo "❌ 进程退出（查 /tmp/qxx-backend.log）"; tail -20 /tmp/qxx-backend.log; exit 1; fi
  sleep 1
done
EOF
```

### 3. 本机上传前端 + 重载 Nginx

```bash
scp -r /Users/huangwenhua/company/self/qixiaoxia-mes/frontend/dist/* \
  qxx:/var/www/qixiaoxia-mes/frontend/dist/

ssh qxx 'nginx -s reload'
```

### 4. 验证

```bash
ssh qxx 'bash -s' << 'EOF'
TOKEN=$(curl -s -X POST http://localhost:8081/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","code":"","uuid":""}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 后端直连
curl -s -o /dev/null -w "后端 :8081 → %{http_code}\n" http://localhost:8081/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"
# Nginx 代理
curl -s -o /dev/null -w "Nginx /prod-api → %{http_code}\n" http://localhost/prod-api/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"
# PC 前端页面
curl -s -o /dev/null -w "PC / → %{http_code}\n" http://localhost/
EOF
```

期望全部返回 `200`。

### 5. （可选）发布移动端 app

⚠️ **app 构建只能手动在 HBuilder 里做**（GUI 工具，无法命令行触发）。本步骤只负责上传已构建好的 web 包。

**前置**：用户在 HBuilder 里点了"发行 → 网站-PC Web 或手机 H5"，输出到
`app/unpackage/dist/build/web/`（日志里会看到 `项目 app 导出Web成功`）。

确认本机输出目录存在再继续：
```bash
ls /Users/huangwenhua/company/self/qixiaoxia-mes/app/unpackage/dist/build/web/ 2>/dev/null \
  && echo "✅ HBuilder 已导出" || echo "❌ 请先在 HBuilder 里发行 web"
```

**部署**（备份旧版 → 上传新版 → reload nginx）：
```bash
# 备份当前版本（带时间戳,便于回滚）
ssh qxx 'mv /var/www/qixiaoxia-mes/app/dist /var/www/qixiaoxia-mes/app/dist.bak.$(date +%s)'

# 上传新构建（HBuilder 输出路径 → 服务器 nginx 实际路径）
scp -r /Users/huangwenhua/company/self/qixiaoxia-mes/app/unpackage/dist/build/web \
  qxx:/var/www/qixiaoxia-mes/app/dist

# reload + 验证
ssh qxx 'nginx -s reload && curl -s -o /dev/null -w "app /app/ → %{http_code}\n" http://localhost/app/'
```

期望 `HTTP 200`。

**关键路径对应**：
- 本机 HBuilder 输出：`app/unpackage/dist/build/web/`
- 服务器 nginx 实际目录：`app/dist/`（不是 `app/unpackage/dist/build/web/`！）
- nginx location：`/app/` → `alias /var/www/qixiaoxia-mes/app/dist/;`

**回滚**：
```bash
ssh qxx 'ls /var/www/qixiaoxia-mes/app/dist.bak.* | tail -1'  # 找到最近备份
ssh qxx 'rm -rf /var/www/qixiaoxia-mes/app/dist && mv /var/www/qixiaoxia-mes/app/dist.bak.<timestamp> /var/www/qixiaoxia-mes/app/dist && nginx -s reload'
```

## 故障排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 后端启动后立即退出 | Docker 容器未启动 | `docker start qxx-mysql qxx-redis qxx-minio` |
| 后端进程被 Killed（OOM） | JVM 总虚拟内存超物理内存被 OOM Killer 杀（**仅 `-Xmx` 不够**，Metaspace/DirectMemory 会让总内存飙到 ~3.8GB） | 启动前 `pkill -f classworlds` 清 Maven 残留；启动命令必须同时限制 `-Xmx512m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m`（见步骤 2）；等 `free -m` available > 800MB 再启；查 `dmesg -T \| grep -i oom` 确认是否 OOM |
| Nginx 502 | 后端未就绪 | 等 `curl :8081` 返回 200 后再重载 |
| 前端 404 | dist/ 未上传或路径错误 | 确认 scp 目标路径 `/var/www/qixiaoxia-mes/frontend/dist/` |
| 登录验证码报错 | 服务器已关闭验证码 | `"uuid":""` 传空字符串 |
| app `/app/` 404 | scp 路径写错（常见错：传到 `app/unpackage/dist/build/web/` 而非 `app/dist/`） | 确认服务器 `app/dist/index.html` 存在；nginx location 是 `alias /var/www/qixiaoxia-mes/app/dist/` |
| app `/app/` 白屏 | HBuilder 输出后没 reload nginx，或浏览器缓存 | `ssh qxx 'nginx -s reload'`；强制刷新（Cmd+Shift+R） |
| app 接口跨域 | app 走相对路径 `/prod-api/`，但访问路径带了 `/app/` 前缀 | 检查 `app/config.js` 的 `baseUrl` 配置是否相对路径，nginx 是否正确代理 `/app/prod-api/` |
| 步骤 5 找不到本机 web 包 | HBuilder 未发行 | 用户先在 HBuilder 点"发行 → 网站"，看到 `项目 app 导出Web成功` 再跑步骤 5 |
