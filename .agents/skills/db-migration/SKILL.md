---
name: db-migration
description: Use when changing database schema or seed data — adding/altering tables, columns, menus, dict data, or writing any SQL migration. Triggers on "加字段/建表/改表/migration/迁移/DDL/DML/加菜单/字典数据". Enforces Flyway rules: version numbering (next V42+), DML idempotency (WHERE NOT EXISTS), explicit factory_id in INSERT (Flyway bypasses MyBatis interceptor), never modify executed migrations. Failure to follow these corrupts production data.
---

# 数据库迁移（Flyway）

通过版本化 SQL 文件管理 Schema 变更，应用启动时自动比对 `flyway_schema_history`，按版本号执行未跑过的脚本。

| 配置项 | 值 |
|--------|-----|
| 迁移文件路径 | `backend/ruoyi-admin/src/main/resources/db/migration/` |
| `baseline-version` | 32（V1–V32 已存在于 DB，跳过）|
| **当前最新版本** | **V41**（新迁移从 V42 开始，动手前先 `ls ... \| sort -V \| tail -3` 确认）|

## 标准流程

```bash
# Step 1: 查最新版本号
ls backend/ruoyi-admin/src/main/resources/db/migration/ | sort -V | tail -3
# Step 2: 创建 V{next}__{desc}.sql（双下划线，英文描述）
# Step 3: 编写 SQL（DDL + DML 可混排，按逻辑顺序）
```

## 命名规范

```
V{版本号}__{描述}.sql
✅ V42__add_product_bom_table.sql
❌ V42_add_bom.sql          （单下划线）
❌ V42__添加BOM表.sql        （中文文件名）
❌ 42__add_bom.sql           （缺 V 前缀）
```

- 前缀 `V` = 版本化迁移，只执行一次
- 版本号严格递增整数，新需求取最大值 +1
- 双下划线 `__` 分隔（不是单下划线）
- 描述用下划线分隔的英文

## 核心约束（违反会出生产事故）

### 1. DML 必须幂等

Flyway checksum 只防文件篡改，不防数据重复。一条 `INSERT` 重跑会产生重复行。

```sql
-- ❌ 不幂等 — 每次启动都插入新行，迟早爆唯一约束
INSERT INTO sys_menu (menu_name, perms) VALUES ('物料管理', 'mes:md:item:list');

-- ✅ 幂等 — WHERE NOT EXISTS 防重
INSERT INTO sys_menu (factory_id, menu_name, parent_id, order_num, path, component, perms, status)
SELECT 1, '物料管理', 2000, 1, 'item', 'mes/md/item/index', 'mes:md:item:list', '0'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'mes:md:item:list'
);
```

**所有 DML 语句（INSERT/UPDATE/DELETE）都必须做幂等判断**，不只是 INSERT。

### 2. 已执行的迁移文件不可修改

V42 执行后 → `flyway_schema_history` 记录了 checksum → 改 V42 内容 → 启动时 checksum 不匹配 → **报错拒绝启动**。

**要调整 Schema？** 新建更高版本号的迁移，在里面 `ALTER TABLE`/`UPDATE`，**永远不修改已执行的旧文件**。

### 3. factory_id 处理（Flyway 不走 MyBatis 拦截器）

| SQL 类型 | factory_id 规则 |
|----------|----------------|
| `CREATE TABLE` | 必须包含 `factory_id` 字段（所有表）|
| `INSERT` | **必须显式写 `factory_id`**（拦截器不生效）|
| `UPDATE` / `DELETE` | WHERE 带 `factory_id` |

```sql
-- ✅ Flyway 迁移中的 INSERT 必须写 factory_id
INSERT INTO sys_menu (factory_id, menu_name, perms)
VALUES (1, '物料管理', 'mes:md:item:list');

-- ❌ 不要依赖拦截器自动注入（Flyway 裸 JDBC，不走拦截器）
INSERT INTO sys_menu (menu_name, perms) VALUES ('物料管理', 'mes:md:item:list');
```

## DDL + DML 混排示例

一个迁移文件可同时含 DDL 和 DML，按逻辑顺序编排：

```sql
-- V42__add_product_bom.sql
-- Part 1: DDL（建表）
CREATE TABLE qxx_md_bom (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL COMMENT '物料ID',
    factory_id BIGINT NOT NULL COMMENT '工厂ID',
    -- ...
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品BOM';

-- Part 2: DML（种子数据 — 注意幂等）
INSERT INTO sys_menu (...) SELECT ... WHERE NOT EXISTS (...);
INSERT INTO sys_dict_data (...) SELECT ... WHERE NOT EXISTS (...);
```

## 本地 vs 生产

| 场景 | 方式 |
|------|------|
| 本地开发 | 启动后端 → Flyway 自动扫描 `db/migration/` → 执行未跑版本 |
| 生产部署 | 同上（后端重启时自动执行）。兜底：`ssh qxx 'docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes < /var/www/qixiaoxia-mes/backend/.../xxx.sql'` |

> ⚠️ **只执行 SQL，不连带部署/重启操作**。

## 查看迁移历史

```sql
SELECT version, description, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY version DESC LIMIT 10;
```
