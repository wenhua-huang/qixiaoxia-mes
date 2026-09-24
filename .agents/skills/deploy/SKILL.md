---
name: deploy
description: Use to publish this project to the production server (115.29.234.204). Triggers on "deploy", "发布", "上线", "发布到生产", "发布生产", "ship to prod". Runs: build frontend locally → ssh to server → git pull → mvn build backend → systemctl restart qxx-backend → tar-upload dist → reload nginx → verify endpoints (must hit a new endpoint, captcha 200 does not prove the new jar). Also covers optional mobile app (uni-app) tar upload if the user just built it in HBuilder. Reads server credentials and paths from root AGENTS.md.
---

# 生产环境发布

发布流程：本机构建前端 → 服务器拉代码 → 编译后端 → 重启 → 上传前端 → 重载 Nginx →（可选）上传移动端 app。

## 前置条件

- **SSH 免密已配**：`~/.ssh/config` 含 `Host qxx` 别名 → `115.29.234.204`（root，密钥 `~/.ssh/id_ed25519`）。本 skill 所有 ssh/scp 均用 `qxx` 别名，无需密码
- 服务器 Docker 容器正常运行（MySQL:3307, Redis:6380, MinIO:9010）
- **后端由 systemd 单元 `qxx-backend.service` 守护**（enabled，`Restart=always`，`RestartSec=5`，ExecStart 含全部 JVM 内存参数，日志 append 到 `/tmp/qxx-backend.log`）。重启一律 `systemctl restart qxx-backend.service`，**禁止 kill + 手动 nohup**（kill 后 5 秒内会被旧 jar 自动拉起抢 8081，详见故障排查表）
- **服务器仅 1.8GB 内存，前端构建必须在本机完成，禁止在服务器跑 `vite build`**
- 项目根路径：`/Users/huangwenhua/company/self/qixiaoxia-mes`（本机，main 分支）与 `/var/www/qixiaoxia-mes`（服务器）

## 发布范围决策

发布时主动问用户："本次发布要包含哪些？" 选项：
- **PC 前端 + 后端**（默认）：步骤 1-4
- **移动端 app**：步骤 5（独立，可与上面任意组合）
- **全量**：1-5 全做

不问就默认走 1-4。

## nginx 配置

生产 nginx 配置权威副本存在仓库里：`.agents/skills/deploy/nginx.conf`（相对本 skill 目录就是 `./nginx.conf`）。

服务器落地路径：`/etc/nginx/conf.d/qixiaoxia-mes.conf`。

**location 关键点（改前必看）**：
- `/app/prod-api/` → 剥前缀反代后端，**必须存在** —— uni-app H5 的 `<image src="/prod-api/xxx">` 会被自动加 `/app/` 前缀变 `/app/prod-api/xxx`，没这条 app 图片全裂
- `/app/` → app dist 静态 alias + SPA fallback
- `/prod-api/` → PC 端 API 代理
- `/` → PC dist 静态 + SPA fallback

**每次发布前的漂移检测**（30 秒，比事后 debug 图片裂开省心）：

```bash
diff <(ssh qxx 'cat /etc/nginx/conf.d/qixiaoxia-mes.conf') \
     <(grep -v '^##' /Users/huangwenhua/company/self/qixiaoxia-mes/.agents/skills/deploy/nginx.conf) \
  && echo "✅ nginx 配置一致" || echo "⚠️ nginx 配置漂移，看上面 diff 决定谁是权威"
```

（仓库副本首尾有 `##` 开头的说明注释，diff 时过滤掉；服务器实际配置不带这些注释。）

**同步到服务器**（发现漂移或初次装机时用）：

```bash
# 1. 服务器上先备份现有配置
ssh qxx 'cp /etc/nginx/conf.d/qixiaoxia-mes.conf /etc/nginx/conf.d/qixiaoxia-mes.conf.bak.$(date +%s)'

# 2. 上传新配置（用仓库副本，先剥掉 ## 注释行）
grep -v '^##' /Users/huangwenhua/company/self/qixiaoxia-mes/.agents/skills/deploy/nginx.conf \
  | ssh qxx 'cat > /etc/nginx/conf.d/qixiaoxia-mes.conf'

# 3. 语法校验 + reload
ssh qxx 'nginx -t && nginx -s reload && echo "✅ nginx reloaded"'
```

**回滚**：
```bash
ssh qxx 'ls /etc/nginx/conf.d/qixiaoxia-mes.conf.bak.* | tail -1'  # 找到最近备份
ssh qxx 'cp /etc/nginx/conf.d/qixiaoxia-mes.conf.bak.<timestamp> /etc/nginx/conf.d/qixiaoxia-mes.conf && nginx -t && nginx -s reload'
```

## 发布步骤

### 1. 本机构建前端

```bash
cd /Users/huangwenhua/company/self/qixiaoxia-mes/frontend && npx vite build
```

### 2. 服务器端：拉代码 → 编译 → systemctl 重启后端

> ⚠️ **关键：整个构建过程不要停后端。** `qxx-backend.service` 是 `Restart=always`，JVM 一退出 5 秒内就会被 systemd 用**磁盘上的旧 jar** 拉起；若此时新 jar 还没打完，8081 会被旧进程占住，之后手动/再次启动的新 JVM 全部因端口占用退出——表现是 `captchaImage` 一直 200 但跑的是旧代码（2026-09-24 实际踩过）。
> 正确顺序：**旧 JVM 照常服务 → 拉码 → 打包 → `systemctl restart` 一次切换**。Linux 允许运行中的 JVM 继续持有被覆盖前的 jar inode，打包不影响在跑的进程。

#### 2.1 拉代码 + 编译（heredoc，JVM 保持运行）

```bash
ssh qxx << 'EOF'
set -e
cd /var/www/qixiaoxia-mes && git pull origin main

# 编译（必须用 JDK 17）— Flyway 在应用启动时自动执行 db/migration/*.sql
export JAVA_HOME=/usr/lib/jvm/java-17-alibaba-dragonwell-17.0.9.0.10.9-1.al8.x86_64
export PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn clean package -pl ruoyi-admin -am -DskipTests -Dcheckstyle.skip=true -q
ls -la /var/www/qixiaoxia-mes/backend/ruoyi-admin/target/ruoyi-admin.jar
EOF
```

1.8GB 小内存机实测：JVM 运行中打包可行（峰值后仍有余量）。若 `free -m` available 不足 300MB 导致 Maven 被 OOM Killer 杀，正确做法是 `systemctl stop qxx-backend` → 打包 → `systemctl start qxx-backend`（stop 后守护不会自动拉起），**不要**用 kill/pkill。

#### 2.2 systemctl 重启（一条单行 ssh）

```bash
ssh qxx 'systemctl restart qxx-backend.service && sleep 2 && systemctl is-active qxx-backend.service && systemctl show qxx-backend -p MainPID --value && ss -ltnp | grep 8081'
```

期望：`active`、打印新 PID、8081 被该 PID 监听。JVM 参数（`-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+UseG1GC`）和 profile 路径都在 unit 文件里，重启命令不需要也不应该再带。

改 JVM 参数/启动参数：编辑 `/etc/systemd/system/qxx-backend.service` 后 `systemctl daemon-reload && systemctl restart qxx-backend`。看日志：`tail -f /tmp/qxx-backend.log`（应用日志）或 `journalctl -u qxx-backend -n 100`（systemd 事件）。

#### 2.3 等待就绪 + 确认新版本（单行 ssh，最多 180s）

```bash
ssh qxx 'for i in $(seq 1 180); do code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/login -X POST -H "Content-Type: application/json" -d "{}" 2>/dev/null); if [ "$code" != "000" ] && [ -n "$code" ]; then echo "✅ 后端就绪 HTTP $code (${i}s)"; break; fi; if [ "$(systemctl is-active qxx-backend)" != "active" ]; then echo "❌ 服务退出 (${i}s)"; tail -50 /tmp/qxx-backend.log; exit 1; fi; sleep 1; done'
```

期望：`✅ 后端就绪 HTTP 200 (10~40s)`。`captchaImage`/空 body 的 login 200 **只能证明有进程在服务，不能证明跑的是新 jar**。必须再用 token 调一个**本次发布涉及的接口**确认新版本（旧代码没有该路由时会返回 `No static resource ...` 的 500/404），并在有 Flyway 迁移时查一次 `flyway_schema_history`：

```bash
ssh qxx 'docker exec qxx-mysql mysql -uroot -pqxx123456 mes -N -e "SELECT version,success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;" 2>/dev/null'
```

### 3. 本机上传前端 + 重载 Nginx

⚠️ **不要用 `scp -r dist/*`** —— 服务器 SSH 有并发/时长限制，实测多次 `Connection closed by remote host`（退出码 255）。用 **tar 管道单连接** 更稳，且先清空旧 dist 避免残留：

```bash
cd /Users/huangwenhua/company/self/qixiaoxia-mes/frontend/dist && \
ssh qxx 'rm -rf /var/www/qixiaoxia-mes/frontend/dist/* && echo "已清空旧 dist"' && \
tar czf - . 2>/dev/null | ssh qxx 'tar xzf - -C /var/www/qixiaoxia-mes/frontend/dist/ 2>/dev/null && echo "✅ 解包完成"' && \
ssh qxx 'nginx -s reload && echo "✅ nginx reloaded"'
```

> macOS 的 tar 会输出 `Ignoring unknown extended header keyword 'LIBARCHIVE.xattr.com.apple.provenance'` 警告，`2>/dev/null` 已吞掉；不影响内容。

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

**部署**（备份旧版 → tar 管道上传新版 → reload nginx）。app 包小文件多，同样走 tar 单连接，不用 `scp -r`：
```bash
# 备份当前版本（带时间戳，便于回滚；首次部署 dist 不存在也不报错）
ssh qxx 'if [ -d /var/www/qixiaoxia-mes/app/dist ]; then mv /var/www/qixiaoxia-mes/app/dist /var/www/qixiaoxia-mes/app/dist.bak.$(date +%s); fi; mkdir -p /var/www/qixiaoxia-mes/app/dist'

# 上传新构建（HBuilder 输出目录内容 → 服务器 nginx 实际目录）
cd /Users/huangwenhua/company/self/qixiaoxia-mes/app/unpackage/dist/build/web && \
  tar czf - . 2>/dev/null | ssh qxx 'tar xzf - -C /var/www/qixiaoxia-mes/app/dist/ 2>/dev/null'

# reload + 验证（200 之外再确认首页引用的是新构建的 hash 资源）
ssh qxx 'nginx -s reload && curl -s -o /dev/null -w "app /app/ → %{http_code}\n" http://localhost/app/ && curl -s -o /dev/null -w "app 图片代理 /app/prod-api/ → %{http_code}\n" http://localhost/app/prod-api/captchaImage'
```

期望两条均 `HTTP 200`。

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
| 后端启动后立即退出（`systemctl is-active` 非 active） | Docker 容器未启动 | `docker start qxx-mysql qxx-redis qxx-minio`，再 `systemctl restart qxx-backend` |
| 接口 200 但跑的是旧代码；新 JVM 日志有 `Port 8081 was already in use` | 构建前/构建中 kill 了 JVM，systemd 5 秒内用旧 jar 自动拉起并占住 8081，新进程端口冲突退出（2026-09-24 实际事故） | 不要手动 kill；确认旧 PID 后 `systemctl restart qxx-backend` 完成切换，用"调本次新接口"验证版本。构建期确实需要内存：`systemctl stop` → 打包 → `systemctl start`（见 2.1） |
| 改完代码重启，服务仍像旧版本 | 重启打到了别的进程（手动 nohup 的游离 JVM），或 unit 的 ExecStart 路径不是当前 jar 路径 | `systemctl show qxx-backend -p MainPID --value` 与 `ss -ltnp \| grep 8081` 的 PID 必须一致；`systemctl cat qxx-backend` 核对 jar 路径 |
| 后端进程被 Killed（OOM） | 1.8GB 机器上 Maven + JVM 并发内存不足；或 JVM 堆外内存未限制 | 内存紧张时按 2.1 用 `systemctl stop` 停服务再打包，完成后 `systemctl start`；JVM 内存参数在 unit 文件里（`-Xmx512m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m`）；`dmesg -T \| grep -i oom` 确认 OOM |
| `scp -r dist/*` 中途 `Connection closed by remote host`（退出码 255） | scp 建大量小连接超出服务器 SSH 限制 | PC（步骤 3）和 app（步骤 5）一律 tar 管道单连接上传 |
| Nginx 502 | 后端未就绪 | 等 `curl :8081` 返回 200 后再重载 |
| 前端 404 | dist/ 未上传或路径错误 | 确认上传目标路径 `/var/www/qixiaoxia-mes/frontend/dist/`，且 `index.html` 在该目录直接存在 |
| 登录验证码报错 | 服务器已关闭验证码 | `"uuid":""` 传空字符串 |
| app `/app/` 404 | 上传路径写错（常见错：传到 `app/unpackage/dist/build/web/` 而非 `app/dist/`，或把 `web/` 目录本身解成了 `app/dist/web/`） | 确认服务器 `app/dist/index.html` 直接存在（多一层 web 目录就是 tar 时没 cd 进去）；nginx location 是 `alias /var/www/qixiaoxia-mes/app/dist/` |
| app `/app/` 白屏 | HBuilder 输出后没 reload nginx，或浏览器缓存 | `ssh qxx 'nginx -s reload'`；强制刷新（Cmd+Shift+R） |
| app 接口跨域 | app 走相对路径 `/prod-api/`，但访问路径带了 `/app/` 前缀 | 检查 `app/config.js` 的 `baseUrl` 配置是否相对路径，nginx 是否正确代理 `/app/prod-api/` |
| app 里图片裂开（HTML 而非图片）| uni-app H5 把 `<image src="/prod-api/xxx">` 自动前缀化为 `/app/prod-api/xxx`，nginx 若无对应 location 就走 SPA fallback 返回 `app/dist/index.html`（Content-Type: text/html，几百字节）| 服务器 `/etc/nginx/conf.d/qixiaoxia-mes.conf` 必须有 `location /app/prod-api/ { proxy_pass http://127.0.0.1:8081/; ... }`；对比 `./nginx.conf` 权威副本，缺就用本 skill "nginx 配置"章节同步 |
| 步骤 5 找不到本机 web 包 | HBuilder 未发行 | 用户先在 HBuilder 点"发行 → 网站"，看到 `项目 app 导出Web成功` 再跑步骤 5 |
