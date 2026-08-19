# MES 质检模块（qc）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-08-16-mes-qc-quality-inspection-design.md` 落地可配置的质检模块：检测项/模板配置 + IQC/IPQC/OQC/RQC 四类检验单与业务拦截。

**Architecture:** 方案 C——4 张检验单头表 + 1 张统一行表（`qc_type+qc_id` 多态）+ 公共判定引擎/生成工厂/拦截门（各一套）；拦截校验全部注入 Service 层（库存动作时机：item_recpt 与 product_recpt 在 confirm，product_sales 在 postOut）。

**Tech Stack:** Spring Boot 4.0.3 + MyBatis（RuoYi 架构，包 `com.ruoyi.*`）· Vue 3.5 + Element Plus · Flyway（下一版本 **V137**，V136 已被兄弟 worktree qr-code-p1 占用于共享库）· RedisLockTemplate · AutoCodeGenerator · JUnit5+Mockito（单测）/ BaseIntegrationTest+Testcontainers（IT）。

## Global Constraints（每个任务隐含遵守）

- 所有业务表 SQL 的 WHERE 必须带 `factory_id` 条件（`<if>` 写法）；业务代码 INSERT **不写** `factory_id`（拦截器注入）；Flyway 种子 INSERT **显式写** `factory_id`（裸 JDBC）。
- 后端函数 ≤50 行；前端组件 ≤300 行（超限拆分）；重复逻辑 ≥2 次必须抽公共；业务状态用常量类（本项目惯例是 `XxxConstants` 字符串常量，非 enum）。
- 判定提交、检验单生成必须用 `RedisLockTemplate.execute(lockKey, action)`（先锁后事务，禁止在加锁方法上叠 `@Transactional`）。
- 后端改动验证红线：`cd backend && mvn -pl ruoyi-admin -am package -DskipTests` → kill 旧进程 → `nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &` → `curl -s http://localhost:8081/captchaImage` 200 → 用 `python3 backend/scripts/get_token.py` 取 token 实测接口。未走完不得声称完成。
- 单测命名 `should_xxx_when_xxx` + `@DisplayName` 中文；单测禁止连库（Mock 所有 Mapper）；IT 继承 `BaseIntegrationTest`（Testcontainers，库名 `mes_integration_test`）。
- 提交信息用中文 conventional commits（`feat(qc): ...`）；每任务一提交。

## 关键参考文件（实现时照抄模式，不凭记忆写）

| 模式 | 参考文件 |
|------|---------|
| 单表 CRUD 全套 | `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/md/MdItemType.java` + 同名 Mapper/ServiceImpl/Controller（含 `ruoyi-system/src/main/resources/mapper/mes/md/MdItemTypeMapper.xml`） |
| 头+子表级联保存 | `com/ruoyi/system/service/mes/wm/impl/WmProductSalesServiceImpl.java`（lines 级联） |
| 分布式锁 | `ruoyi-common/src/main/java/com/ruoyi/common/core/redis/RedisLockTemplate.java`（`execute("key", () -> ...)`） |
| 自动编码 | `autoCodeGenerator.genSerialCode(常量, "")`（WmProductSalesServiceImpl L105） |
| 幂等迁移 SQL | `backend/ruoyi-admin/src/main/resources/db/migration/V92__slitting_feature.sql`（字典/编码/菜单 WHERE NOT EXISTS 模式） |
| 集成测试基类 | `backend/ruoyi-admin/src/test/java/com/ruoyi/BaseIntegrationTest.java`；示例 `.../mes/sal/SalOrderIT.java` |
| 前端页面 | `frontend/src/views/mes/wm/rt_sales/index.vue`（含质检字段展示）等现有 mes 页面 |
| 前端 API 模块 | `frontend/src/api/mes/wm/product_sales.ts` |

## 文件清单（Create/Modify 全景）

**Backend Create**（包前缀 `com.ruoyi.system` / `com.ruoyi.web`）：
- `domain/mes/qc/`：QcIndex、QcTemplate、QcTemplateIndex、QcTemplateProduct、QcDefect、QcIqc、QcIpqc、QcOqc、QcRqc、QcOrderLine、QcDefectRecord、QcJudgeConfig、QcJudgeResult
- `mapper/mes/qc/`：上述 11 个 Mapper 接口 + `resources/mapper/mes/qc/` 11 个 XML
- `service/mes/qc/`：IQcIndexService、IQcTemplateService、IQcDefectService、IQcIqcService、IQcIpqcService、IQcOqcService、IQcRqcService、IQcOrderLineService、IQcJudgeService、IQcFactoryService、IQcGateService、QcConstants + `impl/` 对应实现
- `controller/mes/qc/`：QcIndexController、QcTemplateController、QcDefectController、QcIqcController、QcIpqcController、QcOqcController、QcRqcController

**Backend Modify**（Service 层 hook，见 Task 9/12/14/16）：
`WmItemRecptServiceImpl`、`WmProductRecptServiceImpl`、`WmProductSalesServiceImpl`、`WmRtIssueServiceImpl`、`ProFeedbackServiceImpl`

**Frontend Create**：`src/api/mes/qc/{index,template,defect,iqc,ipqc,oqc,rqc}.ts`；`src/views/mes/qc/{index,template,defect,iqc,ipqc,oqc,rqc}/index.vue`；`src/views/mes/qc/components/{QcLineEditor,QcDefectDialog,QcJudgeDialog}.vue`

**Frontend Modify**：`src/views/mes/wm/item_recpt/index.vue`（检验状态列+跳转）、`product_sales/index.vue`（同）、`product_recpt/index.vue`（同）、`src/views/mes/pro/feedback/index.vue`（确认后提示检验单）

**Test Create**：`ruoyi-system/src/test/java/com/ruoyi/system/service/mes/qc/impl/{QcJudgeServiceImplTest,QcFactoryServiceImplTest,QcGateServiceImplTest}.java`；`ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/qc/{QcIqcIT,QcOqcIT,QcIpqcIT,QcRqcIT}.java`

---

## 波次一：配置层（Task 1-5）

### Task 1: Flyway V137 — 11 表 + 字典 + 自动编码 + 菜单 + 种子

**Files:**
- Create: `backend/ruoyi-admin/src/main/resources/db/migration/V137__qc_module_core.sql`

**Interfaces:**
- Produces: 11 张 `qxx_qc_*` 表、8 个 `mes_qc_*` 字典、4 条 `sys_auto_code_rule`（QC_IQC_CODE/QC_IPQC_CODE/QC_OQC_CODE/QC_RQC_CODE）、菜单 28900 目录 + 28901-28907 页面 + 按钮、检测项/缺陷/默认模板种子（**不预置 template_product**——绑定物料后才拦截，保证上线零影响）

- [ ] **Step 1: 写迁移文件**（完整 SQL 如下）

```sql
-- ============================================================
-- V136：质量管理模块(qc)
-- 表：检测项/模板(3表)/缺陷字典 + IQC/IPQC/OQC/RQC 单头 + 统一行表 + 缺陷记录
-- 幂等：新表 create（重跑由 Flyway version 保证）；系统表 INSERT 用 WHERE NOT EXISTS
-- 种子不预置 template_product：物料未绑定模板=免检，上线零影响
-- ============================================================
SET NAMES utf8mb4;

-- ══════════ 1. 检测项 ══════════
create table qxx_qc_index (
  index_id        bigint(20)   not null auto_increment comment '检测项ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  index_code      varchar(64)  not null comment '检测项编码',
  index_name      varchar(255) not null comment '检测项名称',
  index_type      varchar(16)  not null comment '检测项类型(IQC/IPQC/OQC/RQC)',
  qc_tool         varchar(255) default null comment '检测工具',
  qc_result_type  varchar(16)  not null comment '值类型:NUMBER/TEXT/DICT/FILE/COUNT',
  dict_type       varchar(64)  default null comment '字典型关联的sys_dict_type(DICT型必填)',
  qc_result_spc   varchar(255) default null comment '值属性(如长度mm/色差ΔE)',
  enable_flag     char(1)      default '1' not null comment '是否启用',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  primary key (index_id),
  unique key uk_index_code (index_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检测项表';

-- ══════════ 2. 检验模板 ══════════
create table qxx_qc_template (
  template_id     bigint(20)   not null auto_increment comment '模板ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  template_code   varchar(64)  not null comment '模板编码',
  template_name   varchar(255) not null comment '模板名称',
  qc_types        varchar(64)  not null comment '适用检验种类(多选逗号分隔,如IQC,RQC)',
  enable_flag     char(1)      default '1' not null comment '是否启用',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  primary key (template_id),
  unique key uk_template_code (template_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检验模板表';

create table qxx_qc_template_index (
  record_id       bigint(20)   not null auto_increment comment '记录ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  template_id     bigint(20)   not null comment '模板ID',
  index_id        bigint(20)   not null comment '检测项ID',
  index_code      varchar(64)  not null comment '检测项编码',
  index_name      varchar(255) not null comment '检测项名称',
  index_type      varchar(16)  not null comment '检测项类型',
  qc_tool         varchar(255) default null comment '检测工具',
  qc_result_type  varchar(16)  default null comment '值类型',
  check_method    varchar(500) default null comment '检测方法/要求',
  stander_val     double(12,4) default null comment '标准值(数值型)',
  unit_of_measure varchar(64)  default null comment '单位',
  threshold_min   double(12,4) default null comment '允许下偏差(实测<标准+下偏差=不合格)',
  threshold_max   double(12,4) default null comment '允许上偏差(实测>标准+上偏差=不合格)',
  order_num       int(11)      default 1 comment '排序',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_template (template_id),
  primary key (record_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='模板-检测项表';

create table qxx_qc_template_product (
  record_id            bigint(20)   not null auto_increment comment '记录ID',
  factory_id           bigint(20)   not null comment '工厂ID',
  template_id          bigint(20)   not null comment '模板ID',
  item_id              bigint(20)   not null comment '物料ID',
  item_code            varchar(64)  default null comment '物料编码',
  item_name            varchar(255) default null comment '物料名称',
  specification        varchar(500) default null comment '规格型号',
  unit_of_measure      varchar(64)  default null comment '单位',
  process_id           bigint(20)   default null comment '工序ID(仅IPQC工序级绑定,可空)',
  process_code         varchar(64)  default null comment '工序编码',
  process_name         varchar(255) default null comment '工序名称',
  quantity_check       int(11)      default 1 comment '抽检样本量',
  quantity_unqualified int(11)      default 0 comment '最大不合格数(Ac值,超过整批拒收)',
  cr_rate              double(12,2) default 0.00 comment '致命缺陷率阈值(%)',
  maj_rate              double(12,2) default 0.00 comment '严重缺陷率阈值(%)',
  min_rate              double(12,2) default 0.00 comment '轻微缺陷率阈值(%)',
  remark               varchar(500) default '' comment '备注',
  create_by            varchar(64)  default '',
  create_time          datetime     default current_timestamp,
  update_by            varchar(64)  default '',
  update_time          datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_template (template_id),
  key idx_item (item_id),
  primary key (record_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='模板-物料表';

-- ══════════ 3. 缺陷字典 ══════════
create table qxx_qc_defect (
  defect_id       bigint(20)   not null auto_increment comment '缺陷ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  defect_code     varchar(64)  not null comment '缺陷编码',
  defect_name     varchar(500) not null comment '缺陷描述',
  index_type      varchar(16)  not null comment '适用检验类型(IQC/IPQC/OQC/RQC)',
  defect_level    varchar(16)  not null comment '等级:CRITICAL/MAJOR/MINOR',
  process_method  varchar(500) default null comment '处置方法(返工/让步接收/退货/报废)',
  enable_flag     char(1)      default '1' not null comment '是否启用',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  primary key (defect_id),
  unique key uk_defect_code (defect_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='缺陷字典表';

-- ══════════ 4. 四张检验单头（统一含：来源溯源/物料/数量/缺陷/判定/状态） ══════════
create table qxx_qc_iqc (
  iqc_id                   bigint(20)   not null auto_increment comment '来料检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  iqc_code                 varchar(64)  not null comment '检验单编码',
  iqc_name                 varchar(255) default null comment '检验单名称',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(qxx_wm_item_recpt.recpt_id)',
  source_doc_type          varchar(32)  default 'wm_item_recpt' comment '来源类型',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  vendor_id                bigint(20)   default null comment '供应商ID',
  vendor_code              varchar(64)  default null comment '供应商编码',
  vendor_name              varchar(255) default null comment '供应商名称',
  vendor_batch             varchar(64)  default null comment '供应商批次号',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  quantity_received        double(12,2) default 0 comment '本次接收数量',
  quantity_check           int(11)      default null comment '本次实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       int(11)      default 0 comment '合格数',
  quantity_unqualified     int(11)      default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数(判定汇总)',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(判定汇总,%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由(CONCESSION必填)',
  receive_date             datetime     default null comment '来料日期',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  primary key (iqc_id),
  unique key uk_iqc_code (iqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='来料检验单';

create table qxx_qc_ipqc (
  ipqc_id                  bigint(20)   not null auto_increment comment '过程检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  ipqc_code                varchar(64)  not null comment '检验单编码',
  ipqc_name                varchar(255) default null comment '检验单名称',
  ipqc_type                varchar(16)  not null comment 'FIRST_CHECK/TOUR_CHECK/LAST_CHECK/SPOT_CHECK',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(pro_card_process.record_id 或 qxx_wm_product_recpt.recpt_id)',
  source_doc_type          varchar(32)  default null comment '来源类型:pro_card_process/wm_product_recpt',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  workorder_id             bigint(20)   default null comment '工单ID',
  workorder_code           varchar(64)  default null comment '工单编码',
  workorder_name           varchar(255) default null comment '工单名称',
  card_id                  bigint(20)   default null comment '流转卡ID',
  card_code                varchar(64)  default null comment '流转卡编码',
  task_id                  bigint(20)   default null comment '任务ID',
  task_code                varchar(64)  default null comment '任务编码',
  process_id               bigint(20)   default null comment '工序ID',
  process_code             varchar(64)  default null comment '工序编码',
  process_name             varchar(255) default null comment '工序名称',
  workstation_id           bigint(20)   default null comment '工位ID(可空)',
  workstation_code         varchar(64)  default null comment '工位编码',
  workstation_name         varchar(255) default null comment '工位名称',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  quantity_check           double(12,4) default null comment '实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       double(12,4) default 0 comment '合格数',
  quantity_unqualified     double(12,4) default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  key idx_workorder (workorder_id),
  primary key (ipqc_id),
  unique key uk_ipqc_code (ipqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='过程检验单';

create table qxx_qc_oqc (
  oqc_id                   bigint(20)   not null auto_increment comment '出货检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  oqc_code                 varchar(64)  not null comment '检验单编码',
  oqc_name                 varchar(255) default null comment '检验单名称',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(qxx_wm_product_sales.sales_id)',
  source_doc_type          varchar(32)  default 'wm_product_sales' comment '来源类型',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  client_id                bigint(20)   default null comment '客户ID',
  client_code              varchar(64)  default null comment '客户编码',
  client_name              varchar(255) default null comment '客户名称',
  batch_code               varchar(64)  default null comment '批次号',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  quantity_out             double(12,2) default 0 comment '发货数量',
  quantity_check           int(11)      default null comment '实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       int(11)      default 0 comment '合格数',
  quantity_unqualified     int(11)      default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由',
  out_date                 datetime     default null comment '出货日期',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  primary key (oqc_id),
  unique key uk_oqc_code (oqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='出货检验单';

create table qxx_qc_rqc (
  rqc_id                   bigint(20)   not null auto_increment comment '退料检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  rqc_code                 varchar(64)  not null comment '检验单编码',
  rqc_name                 varchar(255) default null comment '检验单名称',
  rqc_type                 varchar(16)  default 'PROD_RETURN' comment 'PROD_RETURN/PURCHASE_RETURN/QC_REJECT',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(qxx_wm_rt_issue.rt_id)',
  source_doc_type          varchar(32)  default 'wm_rt_issue' comment '来源类型',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  workorder_id             bigint(20)   default null comment '工单ID(生产退料)',
  workorder_code           varchar(64)  default null comment '工单编码',
  vendor_id                bigint(20)   default null comment '供应商ID(采购退货)',
  vendor_code              varchar(64)  default null comment '供应商编码',
  vendor_name              varchar(255) default null comment '供应商名称',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  batch_code               varchar(128) default null comment '批次号',
  quantity_check           double(12,4) default null comment '实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       double(12,4) default 0 comment '合格数',
  quantity_unqualified     double(12,4) default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由',
  return_reason            varchar(500) default null comment '退料原因',
  responsibility           varchar(16)  default null comment '责任归属:SUPPLIER/PRODUCTION/STORAGE/OTHER',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  primary key (rqc_id),
  unique key uk_rqc_code (rqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='退料检验单';

-- ══════════ 5. 统一检验单行（多态 qc_type+qc_id） ══════════
create table qxx_qc_order_line (
  line_id         bigint(20)   not null auto_increment comment '行ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  qc_type         varchar(16)  not null comment '检验单类型:IQC/IPQC/OQC/RQC',
  qc_id           bigint(20)   not null comment '检验单ID(多态)',
  index_id        bigint(20)   not null comment '检测项ID',
  index_code      varchar(64)  default null comment '检测项编码',
  index_name      varchar(255) default null comment '检测项名称',
  index_type      varchar(16)  default null comment '检测项类型',
  qc_tool         varchar(255) default null comment '检测工具',
  qc_result_type  varchar(16)  default null comment '值类型',
  check_method    varchar(500) default null comment '检测方法',
  stander_val     double(12,4) default null comment '标准值(快照)',
  unit_of_measure varchar(64)  default null comment '单位',
  threshold_min   double(12,4) default null comment '允许下偏差(快照)',
  threshold_max   double(12,4) default null comment '允许上偏差(快照)',
  check_val_text  varchar(500) default null comment '实测值(数值型存数字文本/文本型存内容/字典型存dict_value)',
  cr_quantity     int(11)      default 0 comment '致命缺陷数(该检测项)',
  maj_quantity     int(11)      default 0 comment '严重缺陷数',
  min_quantity     int(11)      default 0 comment '轻微缺陷数',
  line_result     varchar(16)  default null comment '行结果:PASS/FAIL',
  order_num       int(11)      default 1 comment '排序',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_qc_order (qc_type, qc_id),
  primary key (line_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检验单行(四类共用)';

-- ══════════ 6. 缺陷记录 ══════════
create table qxx_qc_defect_record (
  record_id        bigint(20)   not null auto_increment comment '记录ID',
  factory_id       bigint(20)   not null comment '工厂ID',
  qc_type          varchar(16)  not null comment 'IQC/IPQC/OQC/RQC',
  qc_id            bigint(20)   not null comment '检验单ID',
  line_id          bigint(20)   default null comment '检验单行ID',
  defect_id        bigint(20)   default null comment '缺陷字典ID',
  defect_code      varchar(64)  default null comment '缺陷编码',
  defect_name      varchar(500) not null comment '缺陷描述',
  defect_level     varchar(16)  not null comment 'CRITICAL/MAJOR/MINOR',
  defect_quantity  int(11)      default 1 comment '缺陷数量(不合格样品数)',
  process_method   varchar(500) default null comment '处置方法',
  defect_image     varchar(500) default null comment '缺陷图片URL',
  remark           varchar(500) default '' comment '备注',
  create_by        varchar(64)  default '',
  create_time      datetime     default current_timestamp,
  update_by        varchar(64)  default '',
  update_time      datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_qc_order (qc_type, qc_id),
  primary key (record_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检验缺陷记录';

-- ══════════ 7. 字典（系统表无 factory_id） ══════════
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检检验类型', 'mes_qc_type', '0', 'admin', NOW(), '质检四类检验业务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '来料检验', 'IQC', 'mes_qc_type', '', 'primary', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_type' AND dict_value='IQC');
-- （IPQC-过程检验/warning、OQC-出货检验/success、RQC-退料检验/info 同上模式，dict_sort 2-4）

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检单状态', 'mes_qc_status', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_status');
-- PENDING-待检验(warning)/INSPECTING-检验中(primary)/COMPLETED-已完成(success)/CLOSED-已关闭(info)

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检判定结果', 'mes_qc_check_result', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_check_result');
-- PASS-合格(success)/FAIL-不合格(danger)/CONCESSION-让步接收(warning)

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检值类型', 'mes_qc_result_type', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_result_type');
-- NUMBER-数值型/TEXT-文本型/DICT-字典型/FILE-文件型/COUNT-计数型

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '缺陷等级', 'mes_qc_defect_level', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_defect_level');
-- CRITICAL-致命缺陷(danger)/MAJOR-严重缺陷(warning)/MINOR-轻微缺陷(info)

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '过程检验类型', 'mes_qc_ipqc_type', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_ipqc_type');
-- FIRST_CHECK-首检/TOUR_CHECK-巡检/LAST_CHECK-尾检(完工检)/SPOT_CHECK-抽检

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '退料检验类型', 'mes_qc_rqc_type', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_rqc_type');
-- PROD_RETURN-生产退料/PURCHASE_RETURN-采购退货/QC_REJECT-质检退货

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '退料责任归属', 'mes_qc_responsibility', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_responsibility');
-- SUPPLIER-供应商/PRODUCTION-生产部门/STORAGE-仓储部门/OTHER-其他
```

> ⚠️ 实现注意：上面字典段为节省篇幅用注释概括了部分条目，**落盘时必须把每个 dict_value 的完整 INSERT...SELECT...WHERE NOT EXISTS 全部写出**（参照 V92 模式，共 8 个字典类型 27 条字典数据）。

```sql
-- ══════════ 8. 自动编码规则（4 套，模式照抄 V92） ══════════
-- IQC: 'IQC'+yyyyMMdd+3位流水(每日重置)；IPQC/OQC/RQC 同构
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'QC_IQC_CODE', '来料检验单编码', '格式:IQC20260816001', 15, 'N', '0', 'L', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='QC_IQC_CODE');
SET @rid_iqc = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code='QC_IQC_CODE' AND factory_id=1 LIMIT 1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_iqc, 1, 'FIXCHAR', 'PREFIX_IQC', '前缀IQC', 3, 'IQC'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_iqc AND part_index=1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_iqc, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_iqc AND part_index=2);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_iqc, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_iqc AND part_index=3);
-- QC_IPQC_CODE(前缀IPQC,4位,max_length16)/QC_OQC_CODE(前缀OQC)/QC_RQC_CODE(前缀RQC) 同构重复上述 4 段

-- ══════════ 9. 菜单（28900 目录 + 7 页面 + 按钮） ══════════
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28900, '质量管理', 0, 8, 'qc', NULL, 1, 0, 'M', '0', '0', '', 'form', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28900);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28901, '检测项目', 28900, 1, 'qcindex', 'mes/qc/index/index', 1, 0, 'C', '0', '0', 'mes:qc:index:list', 'edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28901);
-- 28902 检验模板 mes/qc/template/index mes:qc:template:list
-- 28903 缺陷字典 mes/qc/defect/index  mes:qc:defect:list
-- 28904 来料检验单 mes/qc/iqc/index   mes:qc:iqc:list
-- 28905 过程检验单 mes/qc/ipqc/index   mes:qc:ipqc:list
-- 28906 出货检验单 mes/qc/oqc/index   mes:qc:oqc:list
-- 28907 退料检验单 mes/qc/rqc/index   mes:qc:rqc:list
-- （同上模式，order_num 2-7，icon 自选现有图标名）

-- 按钮权限（每页面 5 个：list/query/add/edit/remove，menu_type='F'，parent_id=页面ID）：
-- 检测项 28911-28915（perms: mes:qc:index:query/add/edit/remove）
-- 模板   28921-28925；缺陷 28931-28935；IQC 28941-28945 + 28946 mes:qc:iqc:judge（判定）
-- IPQC 28951-28956(+judge)；OQC 28961-28966(+judge)；RQC 28971-28976(+judge)

-- ══════════ 10. 种子：检测项（factory_id 显式；多工厂按需复制） ══════════
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IQC-PAPER-001', '原纸克重', 'IQC', '电子秤', 'NUMBER', 'g/m²', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-PAPER-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IQC-PAPER-002', '原纸幅宽', 'IQC', '卷尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-PAPER-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'IQC-GEN-001', '来料外观检查', 'IQC', '目视', 'TEXT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-GEN-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'IQC-GEN-002', '来料数量核对', 'IQC', '点数', 'COUNT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-GEN-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-PRINT-001', '印刷色差ΔE', 'IPQC', '色差仪', 'NUMBER', 'ΔE', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-PRINT-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-001', '制袋长度', 'IPQC', '钢尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-002', '制袋宽度', 'IPQC', '钢尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-003', '提绳长度', 'IPQC', '钢尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-003');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-004', '胶合强度', 'IPQC', '拉力计', 'NUMBER', 'N', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-004');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'IPQC-GEN-001', '过程外观目视', 'IPQC', '目视', 'TEXT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-GEN-001');
-- OQC-GEN-001~005：装箱核对/外观检查/箱唛核对/封箱检查/托盘检查（DICT 型，dict_type 用 mes_qc_check_result）
-- RQC-GEN-001 退料原因确认(TEXT)/RQC-GEN-002 退料数量清点(COUNT)
-- （全部按上述 WHERE NOT EXISTS 模式写全，共 19 条检测项）

-- ══════════ 11. 种子：缺陷字典（7 条） ══════════
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-001', '印刷色差超标', 'IPQC', 'MINOR', '返工', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-001');
-- DEF-002 尺寸超差(IPQC,MAJOR,返工)/DEF-003 袋底爆裂(IPQC,CRITICAL,报废)/DEF-004 绳长偏差(IPQC,MINOR,让步接收)
-- DEF-005 胶合不牢(IPQC,MAJOR,返工)/DEF-006 破损污渍(IQC,MINOR,退货)/DEF-007 数量短缺(IQC,MAJOR,补货)
-- （同模式写全 7 条；index_type 按实际填 IQC/IPQC/OQC/RQC）

-- ══════════ 12. 种子：默认模板（4 个头 + 行） ══════════
INSERT INTO qxx_qc_template (factory_id, template_code, template_name, qc_types, remark, create_by)
SELECT 1, 'TPL-IQC-STD', '标准来料检验模板', 'IQC', '含克重/幅宽/外观/数量', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_template WHERE template_code='TPL-IQC-STD');
-- TPL-IPQC-STD(qc_types='IPQC')/TPL-OQC-STD('OQC')/TPL-RQC-STD('RQC') 同模式

SET @tid_iqc = (SELECT template_id FROM qxx_qc_template WHERE template_code='TPL-IQC-STD' AND factory_id=1 LIMIT 1);
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_iqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 1, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IQC-GEN-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_iqc AND x.index_code='IQC-GEN-001');
-- （同模式把 TPL-IQC-STD 配 IQC-PAPER-001/002、IQC-GEN-002；TPL-IPQC-STD 配 6 个 IPQC 检测项；
--   TPL-OQC-STD 配 5 个 OQC 核对项；TPL-RQC-STD 配 2 个 RQC 项。克重项可示范 stander_val/threshold：
--   如原纸克重 stander_val 由物料决定留空，由用户在页面上按模板行编辑）
```

> ⚠️ 实现注意：上述带注释概括的段（字典 30 条、编码 4 套、菜单按钮 39 条、检测项 17 条、缺陷 7 条、模板行 17 条，以简报明细枚举为准）**必须全部展开成完整 SQL 后落盘**，禁止保留注释占位。

- [ ] **Step 2: 本地跑迁移验证**

```bash
cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q
# 杀旧进程重启（红线流程）
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s http://localhost:8081/captchaImage -o /dev/null -w "%{http_code}\n"   # 期望 200
docker exec qxx-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "show tables like 'qxx_qc_%'" mes   # 期望 11 行
docker exec qxx-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "select count(*) from qxx_qc_index" mes   # 期望 17
```

预期：Flyway 应用 V136 成功，启动日志无 `Migration checksum mismatch`。

- [ ] **Step 3: Commit**

```bash
git add backend/ruoyi-admin/src/main/resources/db/migration/V137__qc_module_core.sql
git commit -m "feat(qc): V136 质检模块 11 表+字典+编码+菜单+种子"
```

### Task 2: QcConstants + 检测项 CRUD（全套样板）

**Files:**
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/qc/QcConstants.java`
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/qc/QcIndex.java`
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/qc/QcIndexMapper.java`
- Create: `backend/ruoyi-system/src/main/resources/mapper/mes/qc/QcIndexMapper.xml`
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/qc/IQcIndexService.java` + `impl/QcIndexServiceImpl.java`
- Create: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/qc/QcIndexController.java`

**Interfaces:**
- Produces: `IQcIndexService`（标准 CRUD）；`QcConstants`（后续所有任务引用的常量，字段如下，**不得改名**）

```java
package com.ruoyi.system.service.mes.qc;

/** 质检模块公共常量 */
public class QcConstants {
    // 自动编码规则 key（与 V136 一致）
    public static final String CODE_RULE_IQC = "QC_IQC_CODE";
    public static final String CODE_RULE_IPQC = "QC_IPQC_CODE";
    public static final String CODE_RULE_OQC = "QC_OQC_CODE";
    public static final String CODE_RULE_RQC = "QC_RQC_CODE";
    // 检验业务类型
    public static final String TYPE_IQC = "IQC";
    public static final String TYPE_IPQC = "IPQC";
    public static final String TYPE_OQC = "OQC";
    public static final String TYPE_RQC = "RQC";
    // 单据状态
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_INSPECTING = "INSPECTING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CLOSED = "CLOSED";
    // 判定结果
    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_FAIL = "FAIL";
    public static final String RESULT_CONCESSION = "CONCESSION";
    // 行结果
    public static final String LINE_PASS = "PASS";
    public static final String LINE_FAIL = "FAIL";
    // 检测值类型
    public static final String RESULT_TYPE_NUMBER = "NUMBER";
    public static final String RESULT_TYPE_TEXT = "TEXT";
    public static final String RESULT_TYPE_DICT = "DICT";
    public static final String RESULT_TYPE_FILE = "FILE";
    public static final String RESULT_TYPE_COUNT = "COUNT";
    // 缺陷等级
    public static final String DEFECT_CRITICAL = "CRITICAL";
    public static final String DEFECT_MAJOR = "MAJOR";
    public static final String DEFECT_MINOR = "MINOR";
    // IPQC 检验类型
    public static final String IPQC_FIRST = "FIRST_CHECK";
    public static final String IPQC_TOUR = "TOUR_CHECK";
    public static final String IPQC_LAST = "LAST_CHECK";
    public static final String IPQC_SPOT = "SPOT_CHECK";
    // 来源单据类型（gate/factory 反查用）
    public static final String SOURCE_ITEM_RECPT = "wm_item_recpt";
    public static final String SOURCE_PRODUCT_RECPT = "wm_product_recpt";
    public static final String SOURCE_PRODUCT_SALES = "wm_product_sales";
    public static final String SOURCE_CARD_PROCESS = "pro_card_process";
    public static final String SOURCE_RT_ISSUE = "wm_rt_issue";
    // 判定锁 key 前缀
    public static final String LOCK_JUDGE = "qc:judge:";
    public static final String LOCK_GENERATE = "qc:generate:";
}
```

- [ ] **Step 1: 写 QcIndex 实体**（字段=V136 `qxx_qc_index` 全列转 camelCase：indexId/factoryId/indexCode/indexName/indexType/qcTool/qcResultType/dictType/qcResultSpc/enableFlag/remark + createBy/createTime/updateBy/updateTime。照抄 `MdItemType.java` 的类注解/`@Excel` 风格，`Serializable`）
- [ ] **Step 2: 写 Mapper 接口**（`selectQcIndexList(QcIndex)`/`selectQcIndexByIndexId(Long)`/`insertQcIndex`/`updateQcIndex`/`deleteQcIndexByIndexId`/`deleteQcIndexByIndexIds(Long[])`）
- [ ] **Step 3: 写 Mapper XML**（照抄 `MdItemTypeMapper.xml` 结构；list 查询 WHERE 含 `<if test="indexCode != null and indexCode != ''"> and index_code like concat('%',#{indexCode},'%')</if>`、indexName/indexType/qcResultType/enableFlag 同构 + `and factory_id = #{factoryId}` <if>；**这是唯一与参考不同的硬性点**）

> 本项目所有 list SQL 必须带 `factory_id` 条件（拦截器注入参数值）：`<if test="factoryId != null and factoryId != ''"> and factory_id = #{factoryId}</if>`

- [ ] **Step 4: 写 Service 接口 + 实现**（标准模式；`insertQcIndex` 加编码唯一校验：Mapper 加 `checkIndexCodeUnique` 精确匹配语句（`where index_code = #{indexCode} limit 1`，照抄 `MdItemTypeMapper.xml` 的 checkItemTypeCodeUnique 模式），非空即抛 `ServiceException("检测项编码已存在")`。**禁止**用 LIKE 列表查询做唯一校验（子串误判））
- [ ] **Step 5: 写 Controller**（`@RequestMapping("/mes/qc/index")`；list/getInfo/add(`@PreAuthorize("@ss.hasPermi('mes:qc:index:add')')`)/edit/remove/export 标准五端点）
- [ ] **Step 6: 编译 + 红线验证**

```bash
cd backend && mvn -pl ruoyi-system -am compile -q && mvn -pl ruoyi-admin -am package -DskipTests -q
# 重启后实测：
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
curl -s "http://localhost:8081/mes/qc/index/list?pageNum=1&pageSize=10" -H "Authorization: Bearer $TOKEN"
# 期望: {"code":200,...,"rows":[19 条种子...],"total":19}
curl -s -X POST "http://localhost:8081/mes/qc/index" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"indexCode":"IQC-TEST-001","indexName":"临时测试项","indexType":"IQC","qcResultType":"TEXT"}'
# 期望 code:200；再查列表 total=20；删除该条后 total=19
```

- [ ] **Step 7: Commit** `git commit -m "feat(qc): 检测项CRUD+质检常量类"`

### Task 3: 检验模板（头+双子表级联）+ 缺陷字典

**Files:**
- Create: `domain/mes/qc/QcTemplate.java`（字段=template 头 + `private List<QcTemplateIndex> indexRows;` + `private List<QcTemplateProduct> productRows;`）
- Create: `domain/mes/qc/QcTemplateIndex.java`、`QcTemplateProduct.java`（字段=各自 DDL 全列）
- Create: `domain/mes/qc/QcDefect.java`
- Create: 对应 4 个 Mapper 接口 + 4 个 XML（`QcTemplateIndexMapper.selectByTemplateId(Long)`/`deleteByTemplateId(Long)`；`QcTemplateProductMapper` 同 + `selectEnabledByItem(qcType,itemId,processId)` 供波次 2 用）
- Create: `IQcTemplateService` + `impl/QcTemplateServiceImpl.java`、`IQcDefectService` + `impl/QcDefectServiceImpl.java`
- Create: `controller/mes/qc/QcTemplateController.java`、`QcDefectController.java`

**Interfaces:**
- Consumes: Task 2 的模式与 QcConstants
- Produces（后续任务依赖，签名不得改）:
  - `IQcTemplateService.selectQcTemplateWithRows(Long templateId)` → QcTemplate（含 indexRows/productRows）
  - `QcTemplateProductMapper.selectEnabledBindExact(String qcType, Long itemId, Long processId)` / `selectEnabledBindCommon(String qcType, Long itemId)`（均 JOIN qxx_qc_template 过滤 `enable_flag='1'` 且 `FIND_IN_SET(#{qcType}, t.qc_types)`；Exact 匹配 `tp.process_id = #{processId}`，Common 匹配 `tp.process_id IS NULL`；Java 侧先 Exact 后 Common）
  - `IQcTemplateService` 删除保护：`deleteQcTemplateByTemplateIds` 前校验该模板无 `qxx_qc_*` 单据引用（4 个单头 mapper 各 count template_id），有则抛"模板已被检验单引用，请停用而非删除"
  - `QcDefectService` 标准 CRUD

- [ ] **Step 1: QcDefect 全套**（纯照抄 Task 2 模式换字段：defectCode 唯一校验用 `checkDefectCodeUnique` 精确匹配，禁 LIKE）
- [ ] **Step 2: QcTemplate 级联保存**（核心新逻辑，完整代码）：

```java
@Override
@Transactional
public int updateQcTemplate(QcTemplate entity) {
    // 头
    int rows = qcTemplateMapper.updateQcTemplate(entity);
    // 检测项行：全删全插（行数少、编辑频度低，换替换策略最简单且无孤儿行）
    qcTemplateIndexMapper.deleteByTemplateId(entity.getTemplateId());
    if (entity.getIndexRows() != null) {
        for (QcTemplateIndex r : entity.getIndexRows()) {
            r.setTemplateId(entity.getTemplateId());
            r.setFactoryId(entity.getFactoryId());
            qcTemplateIndexMapper.insertQcTemplateIndex(r);
        }
    }
    // 物料绑定行：全删全插 + 启用唯一性校验
    qcTemplateProductMapper.deleteByTemplateId(entity.getTemplateId());
    if (entity.getProductRows() != null) {
        checkBindUnique(entity.getProductRows());
        for (QcTemplateProduct p : entity.getProductRows()) {
            p.setTemplateId(entity.getTemplateId());
            p.setFactoryId(entity.getFactoryId());
            qcTemplateProductMapper.insertQcTemplateProduct(p);
        }
    }
    return rows;
}

/** 同一(检验类型,物料,工序)只允许一条启用绑定 — 防查找歧义 */
private void checkBindUnique(List<QcTemplateProduct> rows) {
    Set<String> seen = new HashSet<>();
    for (QcTemplateProduct p : rows) {
        if (!"1".equals(String.valueOf(p.getEnableFlag() == null ? "1" : p.getEnableFlag()))) continue;
        String key = p.getIndexType() == null ? "" : p.getIndexType(); // 见下方说明
        // 绑定行的检验类型取头模板 qc_types 首值或行上冗余，见 Step 3 数据模型说明
    }
    // 全库校验：对本模板外已存在的启用绑定做 (item_id, ifnull(process_id,0)) 查重
    for (QcTemplateProduct p : rows) {
        int cnt = qcTemplateProductMapper.countEnabledBindExclude(p.getTemplateId(), p.getItemId(), p.getProcessId());
        if (cnt > 0) {
            throw new ServiceException("物料[" + p.getItemName() + "]在该检验维度已存在启用的模板绑定，请先停用原有绑定");
        }
    }
}
```

> **数据模型补充**：`qxx_qc_template_product` 行没有 index_type 列，"同维度唯一"按 `(item_id, process_id)` 对**同检验类型模板**唯一。`countEnabledBindExclude(templateId, itemId, processId)` SQL：`SELECT count(1) FROM qxx_qc_template_product tp JOIN qxx_qc_template t ON tp.template_id=t.template_id WHERE t.enable_flag='1' AND tp.item_id=#{itemId} AND (tp.process_id<=>#{processId}) AND FIND_IN_SET(#{qcType}, t.qc_types) AND tp.template_id != #{templateId} AND tp.factory_id=#{factoryId}`——需同时在 Mapper 方法加 `@Param("qcType")`（从头模板 qc_types 传入）。

- [ ] **Step 3: Controller**（`/mes/qc/template`；getInfo 返回 `selectQcTemplateWithRows`；`/mes/qc/defect` 标准五端点）
- [ ] **Step 4: 编译 + 红线验证**：POST 建模板（带 2 行 indexRows + 1 行 productRows 绑定已有物料）→ GET getInfo 验证行回读 → 再对同物料同类型建第二个启用绑定 → 期望报"已存在启用的模板绑定"
- [ ] **Step 5: Commit** `git commit -m "feat(qc): 检验模板头+行级联保存与绑定唯一校验, 缺陷字典CRUD"`

### Task 4: 配置层前端（检测项/模板/缺陷 3 页）

**Files:**
- Create: `frontend/src/api/mes/qc/index.ts`、`template.ts`、`defect.ts`
- Create: `frontend/src/views/mes/qc/index/index.vue`（检测项）
- Create: `frontend/src/views/mes/qc/template/index.vue`（模板，含两个子表 Tab）
- Create: `frontend/src/views/mes/qc/defect/index.vue`（缺陷）

**Interfaces:**
- Consumes: Task 2/3 的后端端点（路径 `/mes/qc/index|template|defect`）

- [ ] **Step 1: API 模块**（照抄 `src/api/mes/wm/product_sales.ts` 模式；`template.ts` 的 `getTemplate(id)` 返回含 `indexRows/productRows`）

```typescript
// frontend/src/api/mes/qc/template.ts
import request from '@/utils/request'
export interface QcTemplateIndexRow { recordId?: number; templateId?: number; indexId: number | undefined; indexCode?: string; indexName?: string; indexType?: string; qcTool?: string; qcResultType?: string; checkMethod?: string; standerVal?: number; unitOfMeasure?: string; thresholdMin?: number; thresholdMax?: number; orderNum?: number }
export interface QcTemplateProductRow { recordId?: number; templateId?: number; itemId: number | undefined; itemCode?: string; itemName?: string; specification?: string; processId?: number; processCode?: string; processName?: string; quantityCheck?: number; quantityUnqualified?: number; crRate?: number; majRate?: number; minRate?: number }
export interface QcTemplate { templateId?: number; templateCode?: string; templateName?: string; qcTypes?: string; enableFlag?: string; remark?: string; indexRows?: QcTemplateIndexRow[]; productRows?: QcTemplateProductRow[] }
export const listTemplate = (query?: any) => request({ url: '/mes/qc/template/list', method: 'get', params: query })
export const getTemplate = (templateId: number) => request({ url: '/mes/qc/template/' + templateId, method: 'get' })
export const addTemplate = (data: QcTemplate) => request({ url: '/mes/qc/template', method: 'post', data })
export const updateTemplate = (data: QcTemplate) => request({ url: '/mes/qc/template', method: 'put', data })
export const delTemplate = (templateIds: number | number[]) => request({ url: '/mes/qc/template/' + templateIds, method: 'delete' })
```

（`index.ts`/`defect.ts` 同构：list/get/add/update/del 五函数，字段对齐实体）

- [ ] **Step 2: 检测项页面**（照抄任一现有简单 CRUD 页如 `views/mes/md/itemtype/index.vue` 结构：搜索区+工具栏+el-table+分页+新增/编辑 Dialog。表格列：编码/名称/检验类型(dict mes_qc_type tag)/值类型(dict mes_qc_result_type)/工具/值属性/启用/备注；表单含全部字段，DICT 型显示 dict_type 输入框）
- [ ] **Step 3: 模板页面**（编辑 Dialog 内 2 个 el-tab：
  - 「检测项」Tab：el-table 行内编辑（检测项下拉选 `listIndex({enableFlag:'1'})` 带出编码/名称/工具/值类型，可填标准值/单位/上下偏差/方法/排序）
  - 「适用物料」Tab：el-table 行内编辑（物料选择弹窗复用现有物料选择组件（搜 `views/mes/md` 下选择器用法），IPQC 类型时显示工序选择列；抽检样本量/Ac 值/三档缺陷率阈值输入）
  - 保存提交 `{...form, indexRows, productRows}` 整体 put）
- [ ] **Step 4: 缺陷页面**（同 Step 2 模式；列：编码/描述/检验类型/等级(mes_qc_defect_level tag)/处置方法/启用）
- [ ] **Step 5: 浏览器实测**：`npm run dev` 后登录 → 质量管理菜单 → 三页面增删改查 + 模板两个 Tab 保存回读；组件超 300 行必须拆子组件
- [ ] **Step 6: Commit** `git commit -m "feat(qc): 配置层前端三页面(检测项/模板/缺陷)"`

### Task 5: 波次一收尾验证

- [ ] **Step 1: 全链路手工验证**：建模板→绑物料（记下 itemId）→列表过滤；`mvn -pl ruoyi-system test -q` 确认无回归
- [ ] **Step 2: Commit（如有修补）** `git commit -m "fix(qc): 波次一收尾"`

## 波次二：统一行表 + 判定引擎 + IQC 全流程（Task 6-11）

### Task 6: 统一行表 + IQC 单据 CRUD

**Files:**
- Create: `domain/mes/qc/QcOrderLine.java`（字段=order_line DDL 全列）、`domain/mes/qc/QcIqc.java`（字段=iqc DDL 全列 + `private List<QcOrderLine> lines;` + `private List<QcDefectRecord> defectRecords;`）
- Create: `domain/mes/qc/QcDefectRecord.java`
- Create: `mapper/mes/qc/QcOrderLineMapper.java`+XML、`QcIqcMapper.java`+XML、`QcDefectRecordMapper.java`+XML
- Create: `service/mes/qc/IQcOrderLineService.java`+impl、`IQcIqcService.java`+`impl/QcIqcServiceImpl.java`（本任务仅 CRUD + 行/缺陷读写，判定逻辑在 Task 7）
- Create: `controller/mes/qc/QcIqcController.java`

**Interfaces（后续任务依赖，签名不得改）:**
- `IQcOrderLineService`: `List<QcOrderLine> selectByOrder(String qcType, Long qcId)` / `int replaceLines(String qcType, Long qcId, List<QcOrderLine> lines)`（全删全插，回填行结果后落库）/ `int deleteByOrder(String qcType, Long qcId)`
- `QcOrderLineMapper`: 上述对应方法 + `int batchInsert(List<QcOrderLine> lines)`（factory 生成行用）
- `QcIqcMapper`: `List<QcIqc> selectBySource(@Param("sourceDocType") String, @Param("sourceDocId") Long, @Param("itemId") Long)`（gate/factory 反查核心，itemId 可 null）
- `QcDefectRecordMapper`: `List<QcDefectRecord> selectByOrder(String qcType, Long qcId)`
- `IQcIqcService`: 标准 CRUD + `List<QcIqc> listBySource(String sourceDocType, Long sourceDocId)` + `void closeIqc(Long iqcId)` + `void judgeIqc(Long iqcId, String concessionReason)`（Task 9 Step 2 实现）

- [ ] **Step 1: 实体/Mapper/XML**（照抄 Task 2 模式；`QcOrderLineMapper` 的 selectByOrder：`where qc_type=#{qcType} and qc_id=#{qcId} and factory_id=#{factoryId} order by order_num`）
- [ ] **Step 2: Controller**（`/mes/qc/iqc`：list/getInfo(含 lines+defectRecords)/add/edit/remove/**`GET listBySource`**（query 参数 sourceDocType/sourceDocId，供下游单据页面查检验状态，`@Anonymous` 不需要）/`PUT close/{iqcId}`）
- [ ] **Step 3: getDetail 返回组装**（`selectQcIqcByIqcId` 后 set lines/defectRecords）
- [ ] **Step 4: 编译 + 红线验证**：curl 建一条 IQC（手填 templateId/物料）→ getInfo 验证空行数组 → add 3 行 → getDetail 验证行
- [ ] **Step 5: Commit** `git commit -m "feat(qc): 统一检验行表+IQC单据CRUD"`

### Task 7: 判定引擎 QcJudgeService（TDD）

**Files:**
- Create: `domain/mes/qc/QcJudgeConfig.java`（POJO：`Integer quantityCheck; Integer acQuantity; double crRateLimit; double majRateLimit; double minRateLimit;`）
- Create: `domain/mes/qc/QcJudgeResult.java`（POJO：`int crQuantity; int majQuantity; int minQuantity; double crRate; double majRate; double minRate; int quantityUnqualified; String result;`）
- Create: `service/mes/qc/IQcJudgeService.java` + `impl/QcJudgeServiceImpl.java`（**纯逻辑，无 Mapper 依赖**）
- Test: `ruoyi-system/src/test/java/com/ruoyi/system/service/mes/qc/impl/QcJudgeServiceImplTest.java`

**Interfaces:**
- Produces: `String judgeLine(QcOrderLine line)`（数值型自动判定回填 lineResult；非数值型原样返回；未录值返回 null）；`QcJudgeResult judge(List<QcOrderLine> lines, List<QcDefectRecord> defects, QcJudgeConfig cfg)`（汇总+整单判定，会回填每行 lineResult）

- [ ] **Step 1: 先写失败测试**（完整测试代码）

```java
package com.ruoyi.system.service.mes.qc.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.qc.*;
import com.ruoyi.system.service.mes.qc.QcConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("质检判定引擎")
class QcJudgeServiceImplTest {

    private final QcJudgeServiceImpl service = new QcJudgeServiceImpl();

    private QcOrderLine numberLine(Double std, Double min, Double max, String val) {
        QcOrderLine l = new QcOrderLine();
        l.setIndexName("测试数值项");
        l.setQcResultType(QcConstants.RESULT_TYPE_NUMBER);
        l.setStanderVal(std); l.setThresholdMin(min); l.setThresholdMax(max);
        l.setCheckValText(val);
        return l;
    }
    private QcDefectRecord defect(String level, int qty) {
        QcDefectRecord d = new QcDefectRecord();
        d.setDefectLevel(level); d.setDefectQuantity(qty);
        return d;
    }
    private QcJudgeConfig cfg(int checkQty, int ac) {
        QcJudgeConfig c = new QcJudgeConfig();
        c.setQuantityCheck(checkQty); c.setAcQuantity(ac);
        c.setCrRateLimit(0); c.setMajRateLimit(0); c.setMinRateLimit(0);
        return c;
    }

    @Test
    @DisplayName("数值行判定_标准值加偏差区间内合格")
    void should_pass_when_val_within_stander_plus_threshold() {
        QcOrderLine l = numberLine(100.0, -2.0, 2.0, "101.5");
        assertEquals(QcConstants.LINE_PASS, service.judgeLine(l));
        l.setCheckValText("102.01");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
        l.setCheckValText("97.99");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_无标准值时上下限为绝对值")
    void should_use_absolute_bounds_when_no_stander() {
        QcOrderLine l = numberLine(null, 90.0, 110.0, "95");
        assertEquals(QcConstants.LINE_PASS, service.judgeLine(l));
        l.setCheckValText("110.5");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_区间端点为空只校验另一端")
    void should_check_only_bound_when_other_null() {
        QcOrderLine l = numberLine(null, null, 50.0, "60");
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
        l.setCheckValText("50");
        assertEquals(QcConstants.LINE_PASS, service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_非数字实测值抛业务异常")
    void should_throw_when_number_val_not_parseable() {
        QcOrderLine l = numberLine(100.0, -1.0, 1.0, "abc");
        assertThrows(ServiceException.class, () -> service.judgeLine(l));
    }

    @Test
    @DisplayName("数值行判定_未录实测值返回null")
    void should_return_null_when_val_blank() {
        QcOrderLine l = numberLine(100.0, -1.0, 1.0, null);
        assertNull(service.judgeLine(l));
    }

    @Test
    @DisplayName("非数值行_保留人工判定结果")
    void should_keep_manual_result_for_non_number() {
        QcOrderLine l = new QcOrderLine();
        l.setQcResultType(QcConstants.RESULT_TYPE_TEXT);
        l.setLineResult(QcConstants.LINE_FAIL);
        assertEquals(QcConstants.LINE_FAIL, service.judgeLine(l));
    }

    @Test
    @DisplayName("整单判定_未录完抛异常")
    void should_throw_when_any_line_unjudged() {
        QcOrderLine l = numberLine(100.0, -1.0, 1.0, null);
        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.judge(Collections.singletonList(l), Collections.emptyList(), cfg(10, 0)));
        assertTrue(ex.getMessage().contains("未录入"));
    }

    @Test
    @DisplayName("整单判定_不合格数超Ac值判FAIL")
    void should_fail_when_unqualified_over_ac() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeResult r = service.judge(Collections.singletonList(ok),
            Arrays.asList(defect(QcConstants.DEFECT_MINOR, 3), defect(QcConstants.DEFECT_MAJOR, 2)), cfg(20, 4));
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(5, r.getQuantityUnqualified());
    }

    @Test
    @DisplayName("整单判定_任一致命缺陷判FAIL")
    void should_fail_when_any_critical() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeResult r = service.judge(Collections.singletonList(ok),
            Collections.singletonList(defect(QcConstants.DEFECT_CRITICAL, 1)), cfg(100, 10));
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(1, r.getCrQuantity());
    }

    @Test
    @DisplayName("整单判定_缺陷率超阈值判FAIL")
    void should_fail_when_rate_over_limit() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeConfig c = cfg(100, 50);
        c.setMinRateLimit(2.0);
        QcJudgeResult r = service.judge(Collections.singletonList(ok),
            Collections.singletonList(defect(QcConstants.DEFECT_MINOR, 3)), c);
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(3.0, r.getMinRate());
    }

    @Test
    @DisplayName("整单判定_行FAIL但无缺陷记录时不合格数至少为FAIL行数")
    void should_count_fail_lines_when_no_defect_records() {
        QcOrderLine bad = numberLine(100.0, -1.0, 1.0, "200");
        QcJudgeResult r = service.judge(Collections.singletonList(bad), Collections.emptyList(), cfg(10, 0));
        assertEquals(QcConstants.RESULT_FAIL, r.getResult());
        assertEquals(1, r.getQuantityUnqualified());
    }

    @Test
    @DisplayName("整单判定_全部合格判PASS")
    void should_pass_when_all_good() {
        QcOrderLine ok = numberLine(100.0, -1.0, 1.0, "100");
        QcJudgeResult r = service.judge(Collections.singletonList(ok), Collections.emptyList(), cfg(10, 0));
        assertEquals(QcConstants.RESULT_PASS, r.getResult());
        assertEquals(QcConstants.LINE_PASS, ok.getLineResult());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd backend && mvn -pl ruoyi-system test -Dtest=QcJudgeServiceImplTest -q
```
预期：编译失败（类不存在）。

- [ ] **Step 3: 写实现**（完整代码）

```java
package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.qc.QcDefectRecord;
import com.ruoyi.system.domain.mes.qc.QcJudgeConfig;
import com.ruoyi.system.domain.mes.qc.QcJudgeResult;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.service.mes.qc.IQcJudgeService;
import com.ruoyi.system.service.mes.qc.QcConstants;
import org.springframework.stereotype.Service;

/** 质检判定引擎 — 纯逻辑：行判定(数值区间) + 整单判定(Ac/致命/缺陷率) */
@Service
public class QcJudgeServiceImpl implements IQcJudgeService {

    @Override
    public String judgeLine(QcOrderLine line) {
        if (!QcConstants.RESULT_TYPE_NUMBER.equals(line.getQcResultType())) {
            return line.getLineResult();
        }
        if (StringUtils.isBlank(line.getCheckValText())) {
            return null;
        }
        double val;
        try {
            val = Double.parseDouble(line.getCheckValText().trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("检测项[" + line.getIndexName() + "]实测值不是数字：" + line.getCheckValText());
        }
        Double std = line.getStanderVal();
        Double lo = line.getThresholdMin();
        Double hi = line.getThresholdMax();
        Double lower = (std != null) ? (lo != null ? std + lo : null) : lo;
        Double upper = (std != null) ? (hi != null ? std + hi : null) : hi;
        boolean fail = (lower != null && val < lower) || (upper != null && val > upper);
        return fail ? QcConstants.LINE_FAIL : QcConstants.LINE_PASS;
    }

    @Override
    public QcJudgeResult judge(List<QcOrderLine> lines, List<QcDefectRecord> defects, QcJudgeConfig cfg) {
        int failLines = 0;
        for (QcOrderLine line : lines) {
            String r = judgeLine(line);
            if (r == null) {
                throw new ServiceException("检测项[" + line.getIndexName() + "]未录入实测值，无法判定");
            }
            line.setLineResult(r);
            if (QcConstants.LINE_FAIL.equals(r)) {
                failLines++;
            }
        }
        int cr = 0, maj = 0, min = 0;
        for (QcDefectRecord d : defects) {
            int q = d.getDefectQuantity() == null ? 1 : d.getDefectQuantity();
            if (QcConstants.DEFECT_CRITICAL.equals(d.getDefectLevel())) { cr += q; }
            else if (QcConstants.DEFECT_MAJOR.equals(d.getDefectLevel())) { maj += q; }
            else if (QcConstants.DEFECT_MINOR.equals(d.getDefectLevel())) { min += q; }
            else { throw new ServiceException("未知缺陷等级：" + d.getDefectLevel()); }
        }
        int defectQty = cr + maj + min;
        int unqualified = Math.max(defectQty, failLines);
        double checkQty = cfg.getQuantityCheck() == null || cfg.getQuantityCheck() <= 0 ? 0 : cfg.getQuantityCheck();
        double crRate = pct(cr, checkQty), majRate = pct(maj, checkQty), minRate = pct(min, checkQty);
        String result = QcConstants.RESULT_PASS;
        if (cfg.getAcQuantity() != null && unqualified > cfg.getAcQuantity()) { result = QcConstants.RESULT_FAIL; }
        if (cr > 0) { result = QcConstants.RESULT_FAIL; }
        if (checkQty > 0 && (crRate > cfg.getCrRateLimit() || majRate > cfg.getMajRateLimit() || minRate > cfg.getMinRateLimit())) {
            result = QcConstants.RESULT_FAIL;
        }
        QcJudgeResult r = new QcJudgeResult();
        r.setCrQuantity(cr); r.setMajQuantity(maj); r.setMinQuantity(min);
        r.setCrRate(crRate); r.setMajRate(majRate); r.setMinRate(minRate);
        r.setQuantityUnqualified(unqualified); r.setResult(result);
        return r;
    }

    private double pct(int qty, double base) {
        return base <= 0 ? 0 : Math.round(qty * 10000.0 / base) / 100.0;
    }
}
```

- [ ] **Step 4: 跑测试确认全绿**

```bash
cd backend && mvn -pl ruoyi-system test -Dtest=QcJudgeServiceImplTest -q
```
预期：13 个测试全部 PASS。

- [ ] **Step 5: Commit** `git commit -m "feat(qc): 判定引擎TDD完成(行区间判定+Ac/致命/缺陷率整单判定)"`

### Task 8: 生成工厂 QcFactoryService + 拦截门 QcGateService（IQC 部分，TDD）

**Files:**
- Create: `service/mes/qc/IQcFactoryService.java` + `impl/QcFactoryServiceImpl.java`
- Create: `service/mes/qc/IQcGateService.java` + `impl/QcGateServiceImpl.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/system/service/mes/qc/impl/QcFactoryServiceImplTest.java`、`QcGateServiceImplTest.java`

**Interfaces（波次 3-5 直接扩展这两个类，签名不得改）:**

```java
public interface IQcFactoryService {
    /** 查启用模板绑定：先精确工序，后通用(process_id IS NULL)；无绑定返回 null=免检 */
    QcTemplateProduct resolveTemplate(String qcType, Long itemId, Long processId);
    /** 采购入库单创建后生成 IQC 待检单（幂等：同来源+物料存在未关闭单则跳过；Redis 锁） */
    void generateIqcForItemRecpt(WmItemRecpt header, List<WmItemRecptLine> lines);
    /** 按模板检测项快照生成行 */
    List<QcOrderLine> buildLinesFromTemplate(Long templateId, String qcType, Long qcId);
    void generateOqcForProductSales(WmProductSales header, List<WmProductSalesLine> lines);        // Task 12 实现
    void generateIpqcForProductRecpt(WmProductRecpt header);                                        // Task 14 实现
    void generateIpqcForFeedback(ProFeedback feedback);                                             // Task 14 实现
    void generateRqcForRtIssue(WmRtIssue header, List<WmRtIssueLine> lines);                        // Task 16 实现
    /** 来源单据作废时联动关闭其 PENDING/INSPECTING 检验单（按 source 反查置 CLOSED） */
    void closeBySource(String sourceDocType, Long sourceDocId);
}

public interface IQcGateService {
    /** 确认入库前校验：需检物料必须有 COMPLETED 且 PASS/CONCESSION 的检验单，否则抛 ServiceException */
    void assertItemRecptConfirmable(WmItemRecpt header, List<WmItemRecptLine> lines);
    void assertProductRecptConfirmable(WmProductRecpt header);                                      // Task 14 实现
    void assertProductSalesPostable(WmProductSales header);                                          // Task 12 实现
    void assertRtIssueExecutable(WmRtIssue header, List<WmRtIssueLine> lines);                      // Task 16 实现
}
```

- [ ] **Step 1: 先写失败测试**（QcFactoryServiceImplTest，Mockito 全 Mock）

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("IQC 待检单生成工厂")
class QcFactoryServiceImplTest {
    @Mock QcTemplateProductMapper bindMapper;
    @Mock QcIqcMapper iqcMapper;
    @Mock QcOrderLineMapper lineMapper;
    @Mock QcTemplateIndexMapper templateIndexMapper;
    @Mock AutoCodeGenerator autoCodeGenerator;
    @Mock RedisLockTemplate lockTemplate;
    @InjectMocks QcFactoryServiceImpl service;
    // lockTemplate.execute 需 stub 成直接执行: when(lockTemplate.executeWithResult(any(), anyLong(), any()))
    //   .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());

    @Test
    @DisplayName("物料无模板绑定时跳过生成")
    void should_skip_when_no_template_bind() {
        when(bindMapper.selectEnabledBindExact("IQC", 1L, null)).thenReturn(null);
        when(bindMapper.selectEnabledBindCommon("IQC", 1L)).thenReturn(null);
        service.generateIqcForItemRecpt(header(1L), Collections.singletonList(line(1L)));
        verify(iqcMapper, never()).insertQcIqc(any());
    }

    @Test
    @DisplayName("同来源同物料已有未关闭检验单时不重复生成")
    void should_skip_when_active_order_exists() {
        when(bindMapper.selectEnabledBindExact("IQC", 1L, null)).thenReturn(bind(10L));
        when(iqcMapper.selectBySource("wm_item_recpt", 1L, 1L)).thenReturn(
            Collections.singletonList(orderWithStatus("PENDING")));
        service.generateIqcForItemRecpt(header(1L), Collections.singletonList(line(1L)));
        verify(iqcMapper, never()).insertQcIqc(any());
    }

    @Test
    @DisplayName("生成时快照模板阈值并回填入库单头挂点")
    void should_snapshot_thresholds_and_backfill_header() {
        // bind 返回 quantityCheck=5/Ac=1/三率 0,2,3
        // templateIndexMapper 返回 2 行检测项
        // 断言: insertQcIqc 捕获的实体 quantityMinCheck=5, quantityMaxUnqualified=1, majRateLimit=2.0...
        //        header.getIqcId()/getIqcCode() 被回填, 状态 PENDING, 行插入 2 次
    }

    @Test
    @DisplayName("多行同物料合并为一张检验单")
    void should_merge_same_item_lines_into_one_order() { /* 两行同 itemId → insertQcIqc 仅 1 次, quantityReceived=行数量之和 */ }
}
```

（QcGateServiceImplTest 同构：①需检物料无检验单 → 抛异常且消息含物料编码与提示；②存在 COMPLETED+PASS → 放行；③仅 FAIL 单 → 抛异常；④CONCESSION → 放行；⑤未绑定模板物料 → 放行。写全 5 个用例。）

- [ ] **Step 2: 跑测试确认失败** `mvn -pl ruoyi-system test -Dtest='QcFactoryServiceImplTest,QcGateServiceImplTest' -q`
- [ ] **Step 3: 写实现**（核心代码）

```java
// QcFactoryServiceImpl 关键段（generateIqcForItemRecpt）
@Override
public void generateIqcForItemRecpt(WmItemRecpt header, List<WmItemRecptLine> lines) {
    Map<Long, List<WmItemRecptLine>> byItem = lines.stream()
        .collect(Collectors.groupingBy(WmItemRecptLine::getItemId, LinkedHashMap::new, Collectors.toList()));
    for (Map.Entry<Long, List<WmItemRecptLine>> e : byItem.entrySet()) {
        QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_IQC, e.getKey(), null);
        if (bind == null) { continue; }  // 免检
        lockTemplate.execute(QcConstants.LOCK_GENERATE + "iqc:" + header.getRecptId() + ":" + e.getKey(), () -> {
            List<QcIqc> exist = iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, header.getRecptId(), e.getKey());
            boolean hasActive = exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus()));
            if (hasActive) { return; }
            WmItemRecptLine first = e.getValue().get(0);
            double received = e.getValue().stream().mapToDouble(l -> nvl(l.getQuantityRecpt())).sum();
            QcIqc iqc = new QcIqc();
            iqc.setIqcCode(autoCodeGenerator.genSerialCode(QcConstants.CODE_RULE_IQC, ""));
            iqc.setIqcName("来料检验-" + StringUtils.defaultString(first.getItemName()));
            iqc.setTemplateId(bind.getTemplateId());
            iqc.setSourceDocId(header.getRecptId()); iqc.setSourceDocType(QcConstants.SOURCE_ITEM_RECPT);
            iqc.setSourceDocCode(header.getRecptCode());
            iqc.setVendorId(header.getVendorId()); iqc.setVendorCode(header.getVendorCode()); iqc.setVendorName(header.getVendorName());
            iqc.setItemId(first.getItemId()); iqc.setItemCode(first.getItemCode()); iqc.setItemName(first.getItemName());
            iqc.setSpecification(first.getSpecification()); iqc.setUnitOfMeasure(first.getUnitOfMeasure());
            iqc.setQuantityReceived(received);
            iqc.setQuantityMinCheck(bind.getQuantityCheck());
            iqc.setQuantityMaxUnqualified(bind.getQuantityUnqualified());
            iqc.setCrRateLimit(bind.getCrRate()); iqc.setMajRateLimit(bind.getMajRate()); iqc.setMinRateLimit(bind.getMinRate());
            iqc.setReceiveDate(new Date()); iqc.setStatus(QcConstants.STATUS_PENDING);
            iqcMapper.insertQcIqc(iqc);
            lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_IQC, iqc.getIqcId()));
            if (header.getIqcId() == null) { header.setIqcId(iqc.getIqcId()); header.setIqcCode(iqc.getIqcCode()); }
            // 回写挂点列（仅首张）
            wmItemRecptMapper.updateWmItemRecptHeaderRefs(header.getRecptId(), iqc.getIqcId(), iqc.getIqcCode());
        });
    }
}

// QcGateServiceImpl 核心（IQC）
@Override
public void assertItemRecptConfirmable(WmItemRecpt header, List<WmItemRecptLine> lines) {
    Set<Long> items = lines.stream().map(WmItemRecptLine::getItemId).collect(Collectors.toSet());
    for (Long itemId : items) {
        if (factoryService.resolveTemplate(QcConstants.TYPE_IQC, itemId, null) == null) { continue; }
        List<QcIqc> orders = iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, header.getRecptId(), itemId);
        String itemCode = lines.stream().filter(l -> itemId.equals(l.getItemId())).findFirst().map(WmItemRecptLine::getItemCode).orElse(String.valueOf(itemId));
        boolean passed = orders.stream().anyMatch(o -> QcConstants.STATUS_COMPLETED.equals(o.getStatus())
            && (QcConstants.RESULT_PASS.equals(o.getCheckResult()) || QcConstants.RESULT_CONCESSION.equals(o.getCheckResult())));
        if (!passed) {
            String hint = orders.isEmpty() ? "未生成检验单" : "检验单[" + orders.get(0).getIqcCode() + "]状态:" + orders.get(0).getStatus() + "/" + orders.get(0).getCheckResult();
            throw new ServiceException("物料[" + itemCode + "]需来料检验合格后方可确认入库（" + hint + "）");
        }
    }
}
```

> 实现补充：`buildLinesFromTemplate` 把 `qxx_qc_template_index` 各行快照为 `QcOrderLine`（qcType/qcId/检测项五件套/标准值/单位/上下偏差/order_num，checkValText 留空）；`updateWmItemRecptHeaderRefs` 是 WmItemRecptMapper 新增的仅更新 iqc_id/iqc_code 两列的方法（避免并发覆盖整头）。`nvl` 私有方法处理 null 数量取 0。

- [ ] **Step 4: 跑测试全绿** → **Step 5: Commit** `git commit -m "feat(qc): 检验单生成工厂+拦截门(IQC) TDD"`

### Task 9: IQC hook 注入采购入库 + 集成测试

**Files:**
- Modify: `ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/impl/WmItemRecptServiceImpl.java`
- Modify: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/wm/WmItemRecptMapper.java` + XML（新增 `updateWmItemRecptHeaderRefs`）
- Test: `ruoyi-admin/src/test/java/com/ruoyi/web/controller/mes/qc/QcIqcIT.java`

- [ ] **Step 1: 注入依赖**：`WmItemRecptServiceImpl` 加 `@Autowired private IQcFactoryService qcFactoryService;` 与 `@Autowired private IQcGateService qcGateService;`（同包 import）
- [ ] **Step 2: 生成 hook（3 个创建路径的尾部，头+行已落库处）**：
  1. `insertWmItemRecpt(entity)`：若 entity 带 lines 则生成；不带行则跳过
  2. `fromPurOrder` 流程生成头+行的方法尾部（实现时定位保存行后的最后一行代码）
  3. `receiveWithLines(body)`：`saveRecptLines(header, lines);` 之后、`doConfirmItemRecpt(...)` 之前插入 `qcFactoryService.generateIqcForItemRecpt(header, lines);`
- [ ] **Step 3: 拦截 hook**：`doConfirmItemRecpt(header, lines)` 方法体第一行插入

```java
// IQC 拦截：需检物料必须有合格/让步检验单（confirm 即增库存，检验必须前置）
qcGateService.assertItemRecptConfirmable(header, lines);
```

（`confirmItemRecpt` 与 `receiveWithLines` 都经 `doConfirmItemRecpt`，一处注入双入口覆盖。）

- [ ] **Step 4: judgeIqc 实现 + 判定端点**（`QcIqcServiceImpl.judgeIqc(Long iqcId, String concessionReason)` 完整代码与 Task 12 的 `judgeOqc` 同构——锁 key `qc:judge:IQC:{id}`、cfg 取 iqc 头快照、行/缺陷读写、CONCESSION 必填理由、COMPLETED 后写 inspectDate/inspector；Controller 加 `@PutMapping("/judge/{iqcId}")` + `@PreAuthorize("@ss.hasPermi('mes:qc:iqc:judge')")`，body 传 `{"concessionReason": "..."}`）
- [ ] **Step 5: item_recpt 列表 qcStatus**：`WmItemRecptMapper.selectWmItemRecptList` SQL 增加子查询列 `qc_status`（汇总逻辑见 Task 12 Step 5），`WmItemRecpt` 实体加 `qcStatus` 字段（供 Task 10 前端渲染）
- [ ] **Step 6: 写集成测试**（完整骨架，断言写全）

```java
@DisplayName("IQC 来料检验全流程集成测试")
class QcIqcIT extends BaseIntegrationTest {
    // 参照 SalOrderIT 的建数据方式：RestTemplate + token
    // 前置: 建检测项→建模板(带 indexRows)→建 template_product 绑定某物料 M→建供应商→建 PO→fromPurOrder 生成入库单
    @Test should_reject_confirm_when_iqc_pending()      // 入库单 confirm → 期望 code!=200，msg 含"需来料检验"
    @Test should_pass_confirm_after_iqc_judged_pass()   // 打开检验单→录入行值(区间内)→judge→confirm 成功，库存增加
    @Test should_reject_confirm_when_iqc_fail()         // 行录超差值+缺陷数>Ac → judge=FAIL → confirm 拒绝
    @Test should_pass_confirm_when_concession()         // FAIL 后带 concessionReason judge → CONCESSION → confirm 成功
    @Test should_skip_inspection_when_no_bind()         // 未绑定物料建入库单 → 无检验单生成 → confirm 直接成功
}
```

- [ ] **Step 7: 跑 IT**（需 Docker）：`cd backend && mvn -pl ruoyi-admin verify -Dit.test=QcIqcIT -q`，预期 5 用例全绿
- [ ] **Step 8: 红线验证**：打包重启后用 token 实测 `POST /mes/wm/item_recpt/fromPurOrder/{orderId}` + `PUT /mes/wm/item_recpt/confirm/{recptId}`，看到拒绝消息即拦截生效
- [ ] **Step 9: Commit** `git commit -m "feat(qc): IQC接入采购入库(创建生成/确认拦截/判定)+集成测试"`

### Task 10: IQC 前端 + 3 个共用组件 + 入库单检验状态列

**Files:**
- Create: `frontend/src/api/mes/qc/iqc.ts`、`orderline.ts`（行/缺陷 CRUD API：`listLines(qcType,qcId)`/`saveLines(qcType,qcId,data)`/`listDefects(qcType,qcId)`/`saveDefect`/`delDefect`）
- Create: `frontend/src/views/mes/qc/components/QcLineEditor.vue`（**4 个检验页面共用**）
- Create: `frontend/src/views/mes/qc/components/QcDefectDialog.vue`（共用）
- Create: `frontend/src/views/mes/qc/components/QcJudgeDialog.vue`（共用）
- Create: `frontend/src/views/mes/qc/iqc/index.vue`（+ 同目录 `IqcEditDrawer.vue` 拆分控行数）
- Modify: `frontend/src/views/mes/wm/item_recpt/index.vue`（检验状态列）

**Interfaces:**
- Consumes: Task 6/7 后端端点：`PUT /mes/qc/iqc/judge/{iqcId}`（body `{concessionReason}`）、`GET /mes/qc/iqc/listBySource?sourceDocType&sourceDocId`

- [ ] **Step 1: QcLineEditor.vue**（props：`qcType:string, qcId:number, readonly:boolean`；el-table 展示行：检测项/工具/方法/标准值+区间提示 `[std+min, std+max]`/实测值输入（NUMBER 用 el-input-number→存字符串；DICT 用 dict 下拉；TEXT 用文本框；FILE 用上传组件回填 URL）/行结果（NUMBER 显示"判定时自动"；其余 PASS/FAIL 下拉）；失焦时 NUMBER 行前端按同公式即时预览 行结果tag（仅展示，判定以服务端为准））
- [ ] **Step 2: QcDefectDialog.vue**（表格列：缺陷（下拉 listDefect({indexType: qcType, enableFlag:'1'}) 带出等级）/等级 tag/数量/处置方法/图片上传/备注；增删行保存到 `defect_record` API）
- [ ] **Step 3: QcJudgeDialog.vue`**（实际检测数量 quantity_check 输入（默认=quantity_min_check）；"执行判定"按钮 → `PUT judge` → 展示判定结果 tag 与三档缺陷率；若返回 FAIL 显示"让步接收"展开 concessionReason 必填文本域再次提交）
- [ ] **Step 4: iqc/index.vue**：列表（编码/来源单据/供应商/物料/接收数/检测数/状态 tag/结果 tag/检验员/检验日期）+ 筛选（编码/物料/供应商/状态/结果）+ 行操作：`录入检验`（打开 IqcEditDrawer：QcLineEditor + QcDefectDialog + QcJudgeDialog，仅 PENDING/INSPECTING 可用）、`详情`（readonly drawer，含缺陷记录与判定汇总）、`作废`（close，仅 PENDING/INSPECTING）
- [ ] **Step 5: item_recpt/index.vue 增加检验状态列**：渲染 Task 9 Step 5 已加的 `qcStatus` 出参字段（tag 映射 PASSED/CONCESSION/PENDING/FAILED/NONE，点击跳 IQC 列表带 sourceDocId 过滤；零额外请求）
- [ ] **Step 6: 浏览器实测**：完整走 验收场景①③（未检不能确认→录入→PASS 确认成功；FAIL→让步→成功）
- [ ] **Step 7: Commit** `git commit -m "feat(qc): IQC前端+共用行/缺陷/判定组件+入库单检验状态列"`

### Task 11: 波次二收尾

- [ ] **Step 1: `mvn -pl ruoyi-system test -q` + `mvn -pl ruoyi-admin verify -Dit.test='QcIqcIT' -q` 全绿；红线重启实测汇总截图/记录**
- [ ] **Step 2: Commit（如有修补）** `git commit -m "fix(qc): 波次二收尾"`

## 波次三：OQC 出厂检（Task 12-13）

### Task 12: OQC 后端（单据 CRUD + 判定 + 出库拦截）

**Files:**
- Create: `domain/mes/qc/QcOqc.java`（字段=oqc DDL 全列 + lines/defectRecords）+ `QcOqcMapper.java`+XML
- Create: `service/mes/qc/IQcOqcService.java`+`impl/QcOqcServiceImpl.java` + `controller/mes/qc/QcOqcController.java`
- Modify: `QcFactoryServiceImpl`（实现 `generateOqcForProductSales`）、`QcGateServiceImpl`（实现 `assertProductSalesPostable`）
- Modify: `WmProductSalesServiceImpl`、`WmProductSalesMapper`（+`updateSalesHeaderRefs`）、出库 list SQL（+qcStatus 汇总列）
- Test: `QcFactoryServiceImplTest`/`QcGateServiceImplTest` 补 OQC 用例；`QcOqcIT.java`

- [ ] **Step 1: QcOqc CRUD**（照 Task 6 IQC 模式：list/getInfo(含行+缺陷)/add/edit/remove/listBySource/close/judge）
- [ ] **Step 2: judgeOqc**（完整代码，IQC 的 judge 在 Task 9 已实现于 `QcIqcServiceImpl.judgeIqc`，此处同构镜像——判定核心复用 `IQcJudgeService`，仅单据读写不同）：

```java
@Override
public void judgeOqc(Long oqcId, String concessionReason) {
    lockTemplate.execute(QcConstants.LOCK_JUDGE + "OQC:" + oqcId, () -> {
        QcOqc oqc = qcOqcMapper.selectQcOqcByOqcId(oqcId);
        if (oqc == null) { throw new ServiceException("检验单不存在"); }
        if (QcConstants.STATUS_COMPLETED.equals(oqc.getStatus()) || QcConstants.STATUS_CLOSED.equals(oqc.getStatus())) {
            throw new ServiceException("已完成或已关闭的检验单不可判定");
        }
        List<QcOrderLine> lines = orderLineService.selectByOrder(QcConstants.TYPE_OQC, oqcId);
        if (lines.isEmpty()) { throw new ServiceException("检验单无检测项"); }
        List<QcDefectRecord> defects = defectRecordMapper.selectByOrder(QcConstants.TYPE_OQC, oqcId);
        QcJudgeConfig cfg = new QcJudgeConfig();
        cfg.setQuantityCheck(oqc.getQuantityCheck());
        cfg.setAcQuantity(oqc.getQuantityMaxUnqualified());
        cfg.setCrRateLimit(oqc.getCrRateLimit()); cfg.setMajRateLimit(oqc.getMajRateLimit()); cfg.setMinRateLimit(oqc.getMinRateLimit());
        QcJudgeResult r = judgeService.judge(lines, defects, cfg);
        String finalResult = r.getResult();
        if (QcConstants.RESULT_FAIL.equals(finalResult) && StringUtils.isNotBlank(concessionReason)) {
            finalResult = QcConstants.RESULT_CONCESSION;
        }
        if (QcConstants.RESULT_CONCESSION.equals(finalResult) && StringUtils.isBlank(concessionReason)) {
            throw new ServiceException("让步接收必须填写让步理由");
        }
        orderLineService.replaceLines(QcConstants.TYPE_OQC, oqcId, lines);   // 回填行结果
        oqc.setCheckResult(finalResult); oqc.setConcessionReason(QcConstants.RESULT_CONCESSION.equals(finalResult) ? concessionReason : null);
        oqc.setQuantityUnqualified(r.getQuantityUnqualified());
        oqc.setQuantityQualified(Math.max(nvl(oqc.getQuantityCheck()) - r.getQuantityUnqualified(), 0));
        oqc.setCrQuantity(r.getCrQuantity()); oqc.setMajQuantity(r.getMajQuantity()); oqc.setMinQuantity(r.getMinQuantity());
        oqc.setCrRate(r.getCrRate()); oqc.setMajRate(r.getMajRate()); oqc.setMinRate(r.getMinRate());
        oqc.setStatus(QcConstants.STATUS_COMPLETED);
        oqc.setInspectDate(DateUtils.getNowDate()); oqc.setInspector(SecurityUtils.getUsername());
        qcOqcMapper.updateQcOqc(oqc);
    });
}
```

> `QcIqcServiceImpl.judgeIqc`（Task 9 补实现）与此同构；判定编排在各类型 Service 各 ~45 行（类型异构不强行抽象），判定核心逻辑只在 `QcJudgeService` 一处——这是 DRY 的边界决策，crt-review 时不得再抽。

- [ ] **Step 3: factory/gate OQC 实现**：`generateOqcForProductSales(header, lines)` 与 IQC 同构（分组物料→resolveTemplate(OQC,item,null)→锁内幂等→建单快照（quantity_out=行数量和，client 三件套从 header）→建行→`updateSalesHeaderRefs` 回填首张）；`assertProductSalesPostable(header)` 内部自行加载出库行后按 IQC gate 同构校验，异常消息文案改"需出货检验合格后方可出库确认"
- [ ] **Step 4: WmProductSalesServiceImpl hook**：①创建路径（`insertWmProductSales` 若保存行、`fromSaleOrder` 流程行落库后）→ `qcFactoryService.generateOqcForProductSales(header, lines)`；②`postOut(salesId, details)` 方法体头部（加载 header 之后）→ `qcGateService.assertProductSalesPostable(header)`；③`close(salesId)`/`cancel(salesId)` 作废路径 → 调 factory 新增的 `closeBySource("wm_product_sales", salesId)`（联动把 PENDING/INSPECTING 检验单置 CLOSED，spec §7 联动作废）
- [ ] **Step 5: 出库列表 qcStatus**：`selectWmProductSalesList` SQL 加子查询列 `qc_status`（对 source_doc_type='wm_product_sales' AND source_doc_id=sales_id 的检验单状态汇总：任一 COMPLETED+PASS→PASSED；COMPLETED+CONCESSION→CONCESSION；COMPLETED+FAIL→FAILED；否则 PENDING/NONE），实体加 `qcStatus` 字段
- [ ] **Step 6: 单测+IT**：factory/gate 补 OQC 用例（同 IQC 五场景）；`QcOqcIT`：should_generate_oqc_on_sales_create / should_reject_postOut_when_pending / should_pass_postOut_after_judged（含多物料两张单全过才放行场景）
- [ ] **Step 7: 红线验证 + Commit** `git commit -m "feat(qc): OQC出厂检(判定+出库确认拦截)+集成测试"`

### Task 13: OQC 前端

- [ ] **Step 1**: `api/mes/qc/oqc.ts`（同 iqc.ts 模式）；`views/mes/qc/oqc/index.vue` + `OqcEditDrawer.vue`（列表列/筛选把供应商换成客户与批次，其余复用 IQC 页结构与 3 个共用组件；出库数量 quantity_out 展示）
- [ ] **Step 2**: `product_sales/index.vue` 加 qcStatus 列（tag 映射 PASSED/CONCESSION/PENDING/FAILED/NONE，点击跳 OQC 列表带 sourceDocId）
- [ ] **Step 3**: 浏览器实测验收场景⑤（多物料出库单生成多张 OQC，全部合格才可出库确认）+ Commit `feat(qc): OQC前端+出库单检验状态列`

## 波次四：IPQC 工序检 + 完工检（Task 14-15）

### Task 14: IPQC 后端

**Files:**
- Create: `domain/mes/qc/QcIpqc.java`（+lines/defectRecords）、`QcIpqcMapper.java`+XML、`IQcIpqcService`+impl、`QcIpqcController`（多一个**手工创建**端点支持首检/巡检/抽检：add 校验 ipqc_type ∈ {FIRST_CHECK,TOUR_CHECK,SPOT_CHECK,LAST_CHECK}，workorderId/itemId 必填）
- Modify: `QcFactoryServiceImpl`（`generateIpqcForFeedback` + `generateIpqcForProductRecpt`）、`QcGateServiceImpl`（`assertProductRecptConfirmable`）
- Modify: `ProFeedbackServiceImpl.confirmFeedback`、`ProFeedbackController.confirm`、`ProCardProcessMapper`（+`selectByCardAndProcess`/`updateCardProcessRefs`）、`WmProductRecptServiceImpl`、`WmProductRecptMapper`（+refs）
- Test: factory/gate 单测补 IPQC 用例；`QcIpqcIT.java`

- [ ] **Step 1: `generateIpqcForFeedback(ProFeedback feedback)`**（完整核心）：

```java
@Override
public void generateIpqcForFeedback(ProFeedback feedback) {
    if (feedback.getRouteId() == null || feedback.getProcessId() == null || feedback.getItemId() == null) { return; }
    ProRouteProcess rp = proRouteProcessMapper.selectByRouteAndProcess(feedback.getRouteId(), feedback.getProcessId());
    if (rp == null || !"Y".equals(rp.getIsCheck())) { return; }   // 非检验工序
    QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_IPQC, feedback.getItemId(), feedback.getProcessId());
    if (bind == null) { return; }                                  // 未配置=免检
    ProCardProcess cp = proCardProcessMapper.selectByCardAndProcess(feedback.getCardId(), feedback.getProcessId());
    if (cp == null) { return; }
    lockTemplate.execute(QcConstants.LOCK_GENERATE + "ipqc:cp:" + cp.getRecordId(), () -> {
        List<QcIpqc> exist = qcIpqcMapper.selectBySource(QcConstants.SOURCE_CARD_PROCESS, cp.getRecordId(), null);
        if (exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus()))) { return; }
        QcIpqc ipqc = new QcIpqc();
        ipqc.setIpqcCode(autoCodeGenerator.genSerialCode(QcConstants.CODE_RULE_IPQC, ""));
        ipqc.setIpqcName("工序检-" + StringUtils.defaultString(feedback.getProcessName()));
        ipqc.setIpqcType(QcConstants.IPQC_LAST);                   // 报工触发的工序完成检
        ipqc.setTemplateId(bind.getTemplateId());
        ipqc.setSourceDocId(cp.getRecordId()); ipqc.setSourceDocType(QcConstants.SOURCE_CARD_PROCESS);
        ipqc.setSourceDocCode(feedback.getCardCode());
        ipqc.setWorkorderId(feedback.getWorkorderId()); ipqc.setWorkorderCode(feedback.getWorkorderCode()); ipqc.setWorkorderName(feedback.getWorkorderName());
        ipqc.setCardId(feedback.getCardId()); ipqc.setCardCode(feedback.getCardCode());
        ipqc.setProcessId(feedback.getProcessId()); ipqc.setProcessCode(feedback.getProcessCode()); ipqc.setProcessName(feedback.getProcessName());
        ipqc.setTaskId(feedback.getTaskId()); ipqc.setTaskCode(feedback.getTaskCode());
        ipqc.setItemId(feedback.getItemId()); ipqc.setItemCode(feedback.getItemCode()); ipqc.setItemName(feedback.getItemName());
        ipqc.setSpecification(feedback.getItemSpc()); ipqc.setUnitOfMeasure(feedback.getUnitOfMeasure());
        snapshotBind(ipqc, bind);                                  // 样本量/Ac/三率，同 IQC 私有方法复用
        ipqc.setStatus(QcConstants.STATUS_PENDING);
        qcIpqcMapper.insertQcIpqc(ipqc);
        lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_IPQC, ipqc.getIpqcId()));
        proCardProcessMapper.updateCardProcessRefs(cp.getRecordId(), ipqc.getIpqcId(), ipqc.getIpqcCode());
    });
}
```

> `selectByRouteAndProcess` / `selectByCardAndProcess` 若 Mapper 无现成方法则按现有 XML 模式新增（`where route_id=#{routeId} and process_id=#{processId} and factory_id=#{factoryId}`）。字段名以 ProFeedback/ProCardProcess 实体实际属性为准（如 itemSpc 对应 specification）。

- [ ] **Step 2: `generateIpqcForProductRecpt(WmProductRecpt header)`**（完工检：resolveTemplate(IPQC, header.produceId/itemId, null)——按实体实际产品字段；source=wm_product_recpt；ipqcType=LAST_CHECK；快照同上；回填 `product_recpt.ipqc_id/ipqc_code`）
- [ ] **Step 3: `assertProductRecptConfirmable(header)`**（gate：加载入库行物料→对配 IPQC(LAST_CHECK) 模板的物料按 source=wm_product_recpt 校验 COMPLETED+PASS/CONCESSION，文案"需完工检验合格后方可确认入库"）
- [ ] **Step 4: hooks**：①`WmProductRecptServiceImpl`：创建路径（`insertWmProductRecpt`/`fromWorkorder` 行落库后）→ generate；`confirmProductRecpt` 状态守卫后、库存更新前 → assert；`mobileConfirmProductRecpt` 若未复用 confirm 核心则在库存更新前同样 assert。②`ProFeedbackServiceImpl.confirmFeedback(recordId)` 确认成功后（状态已置 CONFIRMED 之后）追加：

```java
// IPQC 工序检：isCheck 工序报工确认后生成待检单（弱拦截：报工不阻断，检验失败走后续处置）
String ipqcCode = null;
try {
    ipqcCode = qcFactoryService.generateIpqcForFeedback(feedback); // 返回生成的编码，无则 null
} catch (Exception e) {
    log.warn("IPQC 待检单生成失败 feedbackId={}", recordId, e);
}
```

（`generateIpqcForFeedback` 签名调整为返回 `String`——Task 8 接口定义处同步改。生产报工是高频操作，IPQC 生成失败不阻断，仅告警。）

- [ ] **Step 5: `ProFeedbackController.confirm`** 改为返回 `AjaxResult.success(ipqcCode != null ? Map.of("ipqcCode", ipqcCode) : null)`（响应结构向后兼容，原前端忽略 body）
- [ ] **Step 6: judgeIpqc 实现**（同 judgeOqc 镜像，cfg 取 ipqc 头快照；判定通过后**不**自动流转卡，只完成单据）
- [ ] **Step 7: 单测+IT**：`QcIpqcIT`：should_generate_ipqc_when_ischeck_process_confirmed（断言 card_process.ipqc_id 回填）/ should_not_generate_when_not_ischeck / should_not_generate_twice（重复确认幂等）/ should_generate_last_check_on_product_recpt / should_reject_product_recpt_confirm_when_pending / should_pass_after_judged / should_support_manual_tour_check（手工巡检单创建+判定）
- [ ] **Step 8: 红线验证 + Commit** `git commit -m "feat(qc): IPQC工序检+完工检(报工触发生成/入库拦截)+集成测试"`

### Task 15: IPQC 前端

- [ ] **Step 1**: `api/mes/qc/ipqc.ts`；`views/mes/qc/ipqc/index.vue`+`IpqcEditDrawer.vue`（列表：编码/检验类型 tag/工单/流转卡/工序/物料/检测数/状态/结果；筛选：类型/工单/工序/状态/结果；**手工创建**按钮：弹窗选检验类型(FIRST/TOUR/SPOT)+工单（带出产品物料）+流转卡（可空）+工序，提交后端 add；编辑抽屉复用 3 组件）
- [ ] **Step 2**: `views/mes/pro/feedback/index.vue`：confirm 成功回调若 `res.data?.ipqcCode` 则 `ElMessage.info("已生成过程检验单 " + res.data.ipqcCode)`（spec §4.2 弱拦截=提示的实现）
- [ ] **Step 3**: `product_recpt/index.vue` 加 qcStatus 列（同 Task 13 模式，后端 list SQL 补 qcStatus——Task 14 Step 3 一并做）
- [ ] **Step 4**: 浏览器实测验收场景④ + Commit `feat(qc): IPQC前端+报工提示+产品入库检验状态列`

## 波次五：RQC + 待办 + 收尾（Task 16-17）

### Task 16: RQC 后端 + 待办联动

**Files:**
- Create: `QcRqc.java`/`QcRqcMapper`+XML/`IQcRqcService`+impl/`QcRqcController`（judge 镜像 + return_reason/responsibility 编辑字段）
- Modify: `QcFactoryServiceImpl.generateRqcForRtIssue(header, lines)`、`QcGateServiceImpl.assertRtIssueExecutable`、`WmRtIssueServiceImpl`（`createFromIssue` 行落库后 generate；`executeReturn(rtId)` 顶部 assert，文案"需退料检验合格后方可执行退料"）
- Modify: `QcFactoryServiceImpl` 全部 5 个 generate 方法末尾 + 各类型 judge 完成处 —— 待办联动：

```java
// 生成待办（userId=null 全员可见；字段取值参照现有 insertSysTodoList 调用点，grep 确认 status/priority 惯例值）
SysTodoList todo = new SysTodoList();
todo.setTodoType(TodoTypeEnum.QC_CHECK.getCode());
todo.setTodoTitle("待检验：" + code + " " + itemName);
todo.setSourceDocId(qcId); todo.setSourceDocType(qcType); todo.setSourceDocCode(code);
todo.setPriority("M"); todo.setStatus("0");   // ← 实现时 grep SysTodoListServiceImpl 现有用法对齐取值
sysTodoListService.insertSysTodoList(todo);
// judge 完成：按 source 更新待办 status 已处理 + handleTime/handleResult（updateSysTodoList）
```

- [ ] **Step 1-4**: RQC CRUD → judgeRqc 镜像 → factory/gate + hook → 待办联动（5 个 generate + 4 个 judge）
- [ ] **Step 5**: `QcRqcIT`：should_generate_rqc_on_rt_issue_create / should_reject_execute_when_pending / should_pass_execute_after_judged / should_create_todo_on_generate_and_close_on_judge
- [ ] **Step 6**: 红线验证 + Commit `git commit -m "feat(qc): RQC退料检+QC_CHECK待办联动+集成测试"`

### Task 17: RQC 前端 + 全模块收尾

- [ ] **Step 1**: `api/mes/qc/rqc.ts`；`views/mes/qc/rqc/index.vue`+`RqcEditDrawer.vue`（编辑抽屉在共用组件之上加"退料原因/责任归属"两个表单项，责任归属 tag 色：SUPPLIER 危险/PRODUCTION 警告/其余信息）
- [ ] **Step 2**: **crt-review 三轮自检**（调用 `.agents/skills/crt-review` skill）：逐文件核对 factory_id `<if>`、函数 ≤50 行、组件 ≤300 行、常量引用、无魔法数字
- [ ] **Step 3**: 全模块回归：`mvn -pl ruoyi-system test -q` 全绿；`mvn -pl ruoyi-admin verify -Dit.test='QcIqcIT,QcOqcIT,QcIpqcIT,QcRqcIT' -q` 全绿；红线重启后浏览器过 spec §8 五个验收场景
- [ ] **Step 4**: Commit `git commit -m "feat(qc): RQC前端+质检模块收尾自检"`；更新 spec 状态行为"已实施"

---

## 偏差与决策记录（实现时不得随意扩大）

1. 工序检弱拦截 = 报工确认返回 ipqcCode 提示 + card_process 回填可见；流转卡流转阻塞提示为二期。
2. 判定编排在 4 个类型 Service 各 ~45 行（类型异构不抽象），核心规则只在 QcJudgeService。
3. 需检物料移动端一键收货会被拒（一期检验仅 PC），spec §4.1 已注明。
4. 样品级结果（qxx_qc_result/_detail）、质量报表、返工单/报废单联动、AQL 动态抽样：二期。

## 任务依赖图

T1 → T2 → T3 → T4/T5；T5 → T6 → T7 → T8 → T9 → T10/T11；T11 → T12 → T13；T13 → T14 → T15；T15 → T16 → T17
（波次内串行，波次间串行；T4 与 T5 可并行、T10 与 T11 可并行——前端与收尾验证除外，收尾必须在对应前端后）



