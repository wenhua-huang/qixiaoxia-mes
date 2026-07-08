---
name: new-mes-feature
description: Use when adding a new MES module/entity end-to-end — e.g. "新增入库单/加个报工页面/做个质检模块/加 XX 管理". Orchestrates the full-stack 7-step workflow: DB check → Flyway migration → RuoYi code generator → auto-code rule+parts → backend menu → frontend page → frontend API. Bundles the auto-code SQL template (FIXCHAR+NOWDATE+SERIALNO) and the 4 frontend auto-code integration points. Ensures nothing in the stack is missed.
---

# 新增 MES 功能（全栈 7 步）

新增任何 MES 实体/模块时按序执行，禁止跳步。每步完成才进下一步。

## 1. 检查 DB 字段

```bash
docker exec -i qxx-mysql mysql -uroot -pqxx123456 mes -e "DESCRIBE qxx_{domain}_{entity}"
```
缺字段 → 先建 Flyway 迁移（见 `db-migration` skill）。表不存在 → 同样先建表。

## 2. Flyway 迁移

`backend/ruoyi-admin/src/main/resources/db/migration/V{next}__{desc}.sql`。含建表 DDL + 菜单/字典种子数据（DML 幂等 + INSERT 显式 `factory_id`）。详见 `db-migration` skill。

## 3. 代码生成（标准 CRUD 优先）

RuoYi 代码生成器（`ruoyi-generator`）→ 导入表 → 一键生成 Controller/Service/Mapper/前端页面。**标准 CRUD 不手写**，生成后只补业务逻辑。

## 4. 自动编码（新建实体必做）

新建实体**必须**同步建 `sys_auto_code_rule` + 3 个 `sys_auto_code_part`（FIXCHAR + NOWDATE + SERIALNO），写进同一次 Flyway 迁移：

```sql
-- rule: 编码规则定义
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
VALUES (1, '{RULE_CODE}', '{规则名称}', '{描述}', 20, 'N', '0', 'L', '1');
-- 用 @rid 引用刚插入的 rule_id（SELECT LAST_INSERT_ID() 或子查询）
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = '{RULE_CODE}' AND factory_id = 1);

-- part 1: 固定前缀 (FIXCHAR)
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
VALUES (1, @rid, 1, 'FIXCHAR', '{前缀编码}', '{前缀名称}', {长度}, '{前缀}');

-- part 2: 日期 (NOWDATE)
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
VALUES (1, @rid, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd');

-- part 3: 流水号 (SERIALNO, cycle_flag=Y cycle_method=DAY)
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
VALUES (1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY');
```

> 注意：DML 必须幂等。rule 用 `WHERE NOT EXISTS` 或前置 `DELETE WHERE rule_code=...` 防重。

## 5. 后端菜单

`sys_menu` 表加菜单记录（DDL 幂等写法，见 `db-migration` skill）。权限串 `mes:{domain}:{entity}:{action}`。菜单 `visible='0'` 否则前端路由不注册。

## 6. 前端页面

`frontend/src/views/mes/{domain}/{entity}/index.vue`。遵循 RuoYi 页面排版规范（el-row/el-col 两列、label-width 100px、按钮位置、命名约定）—— 详见 [`frontend/AGENTS.md`](frontend/AGENTS.md)「RuoYi 页面排版规范」。

## 7. 前端 API

`frontend/src/api/mes/{domain}/{entity}.ts`。字段命名前后端一致（camelCase）。后端新增/改接口必须同步更新此文件。

## 自动编码前端接入（4 处修改）

`index.vue` 必须接入自动编码开关：

1. `import { genSerialCode } from '@/api/mes/sys/autocoderule'`
2. `const autoGenFlag = ref(false)`，`reset()` 中重置 `autoGenFlag.value = false`
3. 编码输入旁放开关（**不用** `active-text`，用独立 `<span>`）：
   ```html
   <el-switch size="small" @change="handleAutoGenChange" v-model="autoGenFlag" />
   <span>自动生成</span>
   ```
4. handler：
   ```typescript
   function handleAutoGenChange(flag: boolean) {
     flag ? genSerialCode('{RULE_CODE}').then(r => form.value.code = r.data) : form.value.code = ''
   }
   ```

## 完成后

跑 `crt-review` skill 做三轮自检（Code Review → Repair → Test）。全栈功能上线前还要过 6 层验证清单（见根 `AGENTS.md`「测试策略」）。
