---
name: dev-start
description: Use to start the local development environment for this project. Triggers on "start dev", "启动开发", "启动前后端", "本地启动", "dev server", "跑起来". Checks Docker deps (MySQL/Redis) → builds backend JAR → starts backend :8081 → starts frontend Vite dev → verifies the login chain end-to-end. Also handles stopping services and diagnosing startup failures.
---

# 启动本地开发环境

启动前后端服务，用于本地开发验证。

## 架构

```
浏览器 → Vite Dev Server → /dev-api 代理 → localhost:8081 (Spring Boot)
                                                 ↓
                                          MySQL :3307 (qxx-mysql)
                                          Redis :6380 (qxx-redis)
```

> 项目根路径：`/Users/huangwenhua/Company/qixiaoxiao/qixiaoxia-mes`

## 前置条件

| 依赖 | 容器名 | 端口 | 密码 |
|------|--------|------|------|
| MySQL 8.0 | `qxx-mysql` | 3307 | root / qxx123456 |
| Redis | `qxx-redis` | 6380 | qxx123456 |

## 启动步骤

### 1. 检查/启动依赖服务

```bash
# 确保 Docker 容器运行
docker start qxx-mysql qxx-redis 2>/dev/null
docker ps --filter name=qxx-mysql --filter name=qxx-redis --format "{{.Names}} {{.Status}}"
```

### 2. 构建并启动后端

```bash
cd /Users/huangwenhua/Company/qixiaoxiao/qixiaoxia-mes/backend

# 若已有旧后端运行，先停掉
lsof -ti :8081 | xargs kill 2>/dev/null

# 构建（跳过测试以加速）
mvn clean package -pl ruoyi-admin -am -DskipTests -q

# 启动
nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/qxx-backend.log 2>&1 &

# 等待就绪（最多 60s）
for i in $(seq 1 30); do
  sleep 2
  curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/ | grep -q 200 && echo "✅ 后端就绪" && break
done
```

### 3. 启动前端

```bash
cd /Users/huangwenhua/Company/qixiaoxiao/qixiaoxia-mes/frontend

# 若已有旧前端运行，先停掉（Vite dev 端口见 vite.config.ts，默认 :80）
lsof -ti :80 | xargs kill 2>/dev/null

# 启动 Vite 开发服务器（代理 /dev-api → localhost:8081）
npm run dev &

# 等待就绪
for i in $(seq 1 15); do
  sleep 1
  curl -s -o /dev/null -w "%{http_code}" http://localhost:80/ | grep -q 200 && echo "✅ 前端就绪" && break
done
```

### 4. 验证全链路

```bash
# 验证前端代理 → 后端 API 链路
curl -s http://localhost:80/dev-api/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('登录API:', d.get('code'), d.get('msg',''))"
```

期望输出：`登录API: 200 操作成功`

## 快速启动（一键）

依赖服务已就绪且代码未改动时，可跳过构建直接拉起重启：

```bash
# 后端：仅重启 JAR（不重新构建）
lsof -ti :8081 | xargs kill 2>/dev/null
cd /Users/huangwenhua/Company/qixiaoxiao/qixiaoxia-mes/backend && \
  nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/qxx-backend.log 2>&1 &

# 前端：仅重启 Vite
lsof -ti :80 | xargs kill 2>/dev/null
cd /Users/huangwenhua/Company/qixiaoxiao/qixiaoxia-mes/frontend && npm run dev &
```

## 停止服务

```bash
lsof -ti :8081 | xargs kill  # 停止后端
lsof -ti :80 | xargs kill    # 停止前端
```

## 故障排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 后端启动后立即退出 | Docker 容器未启动 | `docker start qxx-mysql qxx-redis` |
| 前端 404 | Vite 路由未命中 | 确认访问路径正确，刷新页面 |
| 登录接口 500 | 数据库连接失败 | 检查 `docker ps`，确认 `qxx-mysql` 状态 healthy |
| 端口 8081 被占 | 旧后端未完全退出 | `lsof -ti :8081 \| xargs kill -9` |
| 端口 80 被占 | 旧前端未完全退出 | `lsof -ti :80 \| xargs kill -9` |
| 构建失败 | 代码编译错误 | 先 `mvn compile -pl ruoyi-admin -am` 排查 |
| 前端 API 跨域 | Vite 代理未配置 | 检查 `frontend/vite.config.ts` 中 proxy 配置 |

## 查看日志

```bash
tail -f /tmp/qxx-backend.log           # 后端日志
# 前端日志直接看终端输出（stdout/stderr）
```
