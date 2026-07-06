---
description: 生产环境发布 — 本机构建前端 → 服务器拉代码/编译后端/重启 → 上传前端 → 重载 Nginx
---

# 生产环境发布

发布流程：本机构建前端 → 服务器拉代码 → 编译后端 → 重启 → 上传前端 → 重载 Nginx

## 前置条件

- 本机可 SSH 连接服务器（已配置 SSH 密钥免密登录：`ssh qxx`）
- 服务器 Docker 容器正常运行（MySQL:3307, Redis:6380, MinIO:9010）
- **服务器仅 1.8GB 内存，前端构建必须在本机完成，禁止在服务器跑 `vite build`**

## 发布步骤

### 1. 本机构建前端

```bash
cd /Users/huangwenhua/company/self/qixiaoxia-mes/frontend && npx vite build
```

### 2. 服务器端：拉代码 → SQL → 编译 → 重启后端

```bash
ssh qxx << 'EOF'
cd /var/www/qixiaoxia-mes && git pull origin main

# 编译（必须用 JDK 17）— Flyway 在应用启动时自动执行 db/migration/*.sql
export JAVA_HOME=/usr/lib/jvm/java-17-alibaba-dragonwell-17.0.9.0.10.9-1.al8.x86_64
export PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn clean package -pl ruoyi-admin -am -DskipTests -Dcheckstyle.skip=true -q

# 重启
kill $(lsof -ti :8081) 2>/dev/null; sleep 2
nohup java -jar /var/www/qixiaoxia-mes/backend/ruoyi-admin/target/ruoyi-admin.jar \
  --server.port=8081 --ruoyi.profile=/var/www/qixiaoxia-mes/uploadPath \
  > /tmp/qxx-backend.log 2>&1 &
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
TOKEN=$(curl -s -X POST http://localhost:8081/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","code":"","uuid":""}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 后端直连
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"
# Nginx 代理
curl -s -o /dev/null -w "%{http_code}" http://localhost/prod-api/mes/md/unitmeasure/list -H "Authorization: Bearer $TOKEN"
# 前端页面
curl -s -o /dev/null -w "%{http_code}" http://localhost/
```

期望全部返回 `200`。

## 故障排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 后端启动后立即退出 | Docker 容器未启动 | `docker start qxx-mysql qxx-redis qxx-minio` |
| Nginx 502 | 后端未就绪 | 等 `curl :8081` 返回 200 后再重载 |
| 前端 404 | dist/ 未上传或路径错误 | 确认 scp 目标路径 `/var/www/qixiaoxia-mes/frontend/dist/` |
| 登录验证码报错 | 服务器已关闭验证码 | `"uuid":""` 传空字符串 |
