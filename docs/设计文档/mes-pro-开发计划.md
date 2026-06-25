# mes-pro 模块设计与开发计划

## Context

**目标**：在 `qixiaoxia-mes` 项目中从零构建 `mes-pro`（生产管理）模块 + 补充 wm 生产领料功能。

**现状**：
- SQL 表设计已完成：`mes-pro.sql`（21 张表），`mes-wm.sql`（含 issue 三表但 Java 代码未实现领料）
- 后端：RuoYi-Vue 3.9.2，Java 17，Spring Boot 4.0.3，MyBatis（非 MyBatis-Plus），已有 md/wm/qc/dv/cal/tm/pur/sys 模块
- 前端：Vue 3.5 + Element Plus 2.13 + TypeScript + Vite，已有对应模块页面
- `pro` 模块在 qixiaoxia-mes 后端和前端均不存在，需全量新建
- 参考实现：`ktg-mes`（后端）+ `ktg-mes-ui`（前端）

---

## 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1: 工序基础 | ✅ 完成 | 3 entity + 完整 CRUD + 前端页面 |
| Phase 2: 工艺路线 | ✅ 完成 | 5 entity + 工序组成 + 关联产品 + BOM/参数 |
| Phase 3: 生产工单 | ⚠️ 后端完成/前端骨架 | 4 entity + CRUD，向导流程待完善 |
| Phase 4: 生产执行 | ❌ 未开始 | 7 entity + wm 领料退料补充 |
| Phase 5: 看板+追溯 | ❌ 未开始 | Dashboard + MaterialTrace |
| 安灯系统 | ❌ 推迟 | `qxx_pro_andon_config` / `qxx_pro_andon_record` |
| 外协管理 | ❌ 推迟 | 发料→外协加工→回厂验收独立模块 |

---

## 一、外部模块依赖

| 模块 | 被引用的表 | 依赖强度 | 用途 |
|------|-----------|---------|------|
| **md** (主数据) | `qxx_md_factory` | ⭐⭐⭐ 强 | 每个 pro 表都有 `factory_id`，多工厂隔离 |
| | `qxx_md_item` (含 `parent_id` SKU/变体) | ⭐⭐⭐ 强 | 产品物料、BOM 物料。变体在建工单时触发创建（非物料页面手动建） |
| | `qxx_md_product_bom` | ⭐⭐ 中 | 标准产品 BOM（校验参考，非工单创建数据源） |
| | `qxx_md_item_batch_config` | ⭐⭐ 中 | 物料批次属性配置（控制批号自动生成） |
| | `qxx_md_client` | ⭐⭐ 中 | 工单客户 |
| | `qxx_md_vendor` | ⭐⭐ 中 | 外协供应商 |
| | `qxx_md_workstation` | ⭐⭐⭐ 强 | 排产/报工/流转卡的工作站 |
| | `qxx_md_item_type` | ⭐⭐ 中 | 物料分类（RAW/SEMI/FINISHED/AUXILIARY/PACK） |
| **wm** (仓储) | `qxx_wm_issue_header/line/detail` | ⭐⭐⭐ 强 | 生产领料（本次需实现） |
| | `qxx_wm_rt_issue` | ⭐⭐ 中 | 生产退料 |
| | `qxx_wm_outsource_issue/recpt` | ⭐ 轻 | 外协领料/入库 |
| | `qxx_wm_transaction` | ⭐ 轻 | 物料追溯-库存事务 |
| **qc** (质量) | `qxx_qc_ipqc` | ⭐ 轻 | 流转卡过程检验 |
| **sys** (系统) | `sys_auto_code_rule/part/result` | ⭐⭐ 中 | 工序/工单/批号自动编码 |

**前端组件复用**（qixiaoxia-mes 已有的 MES 选择器）：
- `src/components/itemSelect/single.vue` — 物料选择器
- `src/components/vendorSelect/single.vue` — 供应商选择器
- `src/components/warehouseSelect/single.vue` — 仓库选择器
- `src/components/UserSelect/single.vue` — 用户选择器

---

## 二、开发阶段划分（低依赖优先）

### DDL 变更（本次需执行）

1. `qxx_pro_process_content` + `is_check char(1) default 'N'`（已执行）
   ```
   工序步骤级质检标识，与 `qxx_pro_route_process.is_check`（路线工序级）形成两层 QC 控制。
   ```

2. `qxx_pro_workorder_change` — 工单变更记录表（已执行）：
   ```sql
   create table qxx_pro_workorder_change (
     change_id      bigint(20) not null auto_increment comment '变更ID',
     factory_id     bigint(20) not null comment '工厂ID',
     workorder_id   bigint(20) not null comment '工单ID',
     change_type    varchar(32) not null comment '变更类型:BOM/QTY/PARAM/STATUS',
     field_name     varchar(64) not null comment '变更字段',
     old_value      varchar(500) default null comment '原值',
     new_value      varchar(500) default null comment '新值',
     change_reason  varchar(500) default null comment '变更原因',
     approver       varchar(64) default null comment '审批人',
     approve_time   datetime default null comment '审批时间',
     status         varchar(32) default 'PENDING' comment 'PENDING/APPROVED/REJECTED',
     remark         varchar(500) default '' comment '备注',
     create_by      varchar(64) default '',
     create_time    datetime default current_timestamp,
     key idx_workorder_id (workorder_id),
     primary key (change_id)
   ) engine=innodb comment='工单变更记录表';
   ```

---

### Phase 1：工序基础（无 pro 内部依赖） ✅ 完成

纯基础数据 CRUD，不涉及事务和锁。

**后端**（3 个 entity）：
1. `ProProcess` — 生产工序 (`qxx_pro_process`)
2. `ProProcessContent` — 工序作业内容 (`qxx_pro_process_content`)，含 `is_check` 字段
3. `ProParamTemplate` — 工序参数模版 (`qxx_pro_param_template`)

**前端**：`views/mes/pro/process/index.vue`

**UI 交互**：

```
┌─ 搜索：工序编码/名称/类型/启用状态 [搜索][重置] ─────────────────┐
│  [新增] [修改] [删除]                              [导出]       │
├──┬─────────┬─────────┬──────────┬──────┬──────┬──────┬──────┤
│☐│ 工序编码 │ 工序名称 │ 工序类型  │ 启用 │ 备注  │创建时间│ 操作  │
├──┼─────────┼─────────┼──────────┼──────┼──────┼──────┼──────┤
│☐│ PRC-001 │ 印刷    │ INTERNAL │ 是   │ ...  │06-18 │ ✎ ✎ │
└──┴─────────┴─────────┴──────────┴──────┴──────┴──────┴──────┘
                    [< 1 2 3 > 共 N 条]

点击[新增] → 弹窗（含 3 区域）：
  ┌──────────────────────────────────────────────┐
  │  基本信息：                                   │
  │  编码[◎自动○手动] ████  名称 ████            │
  │  类型[▼INTERNAL]  注意事项(textarea)          │
  │  启用[●是 ○否]                               │
  │  ── 作业内容 ──                              │
  │  [新增步骤]                                  │
  │  ┌──┬────────┬──────┬──────┬────┬────┬────┐  │
  │  │序│ 作业内容│ 设备  │ 辅料  │ SOP│质检│操作│  │
  │  ├──┼────────┼──────┼──────┼────┼────┼────┤  │
  │  │1 │ 上料   │印刷机│ 油墨  │ 📎 │ 是 │ ✎✎│  │
  │  │2 │ 调色   │色差仪│  -   │ -  │ 否 │ ✎✎│  │
  │  └──┴────────┴──────┴──────┴────┴────┴────┘  │
  │  ── 参数模版 ──                              │
  │  [新增参数]                                  │
  │  ┌──┬────────┬──────┬────┬──────┬────┬────┐  │
  │  │编│ 参数名  │ 类型  │单位│ 默认值│必填│操作│  │
  │  ├──┼────────┼──────┼────┼──────┼────┼────┤  │
  │  │cl│ 印刷色数│ INT  │ 色 │  4   │ 是 │ ✎✎│  │
  │  └──┴────────┴──────┴────┴──────┴────┴────┘  │
  │                              [取消] [确定]    │
  └──────────────────────────────────────────────┘
```

**关键交互**：
- 编码切换：自动生成开关 → `genSerialCode('PROCESS_CODE')` 自动填充/清空输入框
- 作业内容子表：内嵌 `<el-table>` + 行内编辑弹窗（小 dialog），上传 SOP 文件
- 参数模版子表：内嵌表格 + 弹窗，`param_type=ENUM` 时显示 tag-input 输入枚举值
- 删除校验：工序已被路线引用 → 后端抛异常 → 前端 `ElMessage.error(res.msg)`

**单元测试**：
- Mapper 层：测试 `insert → selectById → update → delete` 完整 CRUD 链路
- Service 层：测试查询列表分页、编码唯一性校验、`is_check` 默认值

---

### Phase 2：工艺路线 ✅ 完成

**后端**（5 个 entity）：
4. `ProRoute` — 工艺路线 (`qxx_pro_route`)
5. `ProRouteProcess` — 路线工序组成 (`qxx_pro_route_process`)
6. `ProRouteProduct` — 产品路线关联 (`qxx_pro_route_product`)
7. `ProRouteProductBom` — 路线产品 BOM (`qxx_pro_route_product_bom`)
8. `ProRouteProcessParam` — 路线产品工序标准参数 (`qxx_pro_route_process_param`)

**关键逻辑**：
- 创建路线产品关联时，从 `qxx_pro_param_template` 复制默认参数值到 `qxx_pro_route_process_param`
- 路线产品 BOM 与 `qxx_md_item` 校验物料存在性
- 工序顺序号 `order_num` 自动递增编号

**前端**：`views/mes/pro/proroute/index.vue`

**UI 交互**：

```
┌─ 搜索：路线编码/名称/启用 [搜索][重置] [刷新] ───────────────────┐
│  [新增] [修改] [删除]                                           │
├──┬─────────┬─────────┬──────┬──────┬────────┬──────┤
│☐│ 路线编码 │ 路线名称 │ 启用 │ 备注  │ 创建时间 │ 操作  │
├──┼─────────┼─────────┼──────┼──────┼────────┼──────┤
│☐│ RT-001  │ 纸袋标准 │ 是   │ ...  │ 06-15  │ ✎ ✎ │
└──┴─────────┴─────────┴──────┴──────┴────────┴──────┘

点击[编辑] → 弹窗（3 Tab）：
  ┌─────────────────────────────────────────────────────┐
  │ [基本信息] [工序组成] [关联产品]                      │
  │                                                      │
  │ Tab2-工序组成：                                       │
  │ [添加工序]                                            │
  │ ┌──┬──────┬──────────┬──────┬──────┬──────┬────────┐ │
  │ │序│ 工序  │ 工序类型  │ 关系  │ 关键 │ 检验 │ 操作   │ │
  │ ├──┼──────┼──────────┼──────┼──────┼──────┼────────┤ │
  │ │1 │ 印刷  │ INTERNAL │ 串行  │ 是   │ 否   │✎BOM参数✎│ │
  │ │2 │ 制袋  │ INTERNAL │ 串行  │ 是   │ 是   │✎BOM参数✎│ │
  │ │3 │ 贴绳  │ OUTSOURCE│ 串行  │ 否   │ 是   │✎BOM参数✎│ │
  │ └──┴──────┴──────────┴──────┴──────┴──────┴────────┘ │
  │                                                      │
  │ 关系下拉：                                             │
  │   串行(SS)—必须完成本工序才能开始下一道                 │
  │   并行(FS)—可与下一道工序同时进行                       │
  │   协同(CC)—需与下一道协同作业                           │
  │  下一道工序按序号自动确定（orderNum+1）                  │
  │                                                      │
  │ Tab3-关联产品：                                       │
  │ [搜索编码/名称 ████] [关联产品]                         │
  │ ┌──┬────────┬──────┬──────┬────────┬──────┐          │
  │ │产品│产品名称│ 规格  │ 批量  │ 标准工时│ 操作 │          │
  │ ├──┼────────┼──────┼──────┼────────┼──────┤          │
  │ │ZD1│ 纸袋A  │25×12│ 1000 │ 120min │ ✎ ✎ │          │
  │ └──┴────────┴──────┴──────┴────────┴──────┘          │
  │ 关联产品：ItemSelect 物料弹窗搜索选择 → 回填编码+名称+规格 │
  │                                        [取消][确定]  │
  └─────────────────────────────────────────────────────┘

点击 [BOM] → 工序物料弹窗：
  ┌──────────────────────────────────────────┐
  │  工序物料 — 印刷                     [×]  │
  │  [新增物料]                              │
  │  → 弹出选择对话框：选工序 + ItemSelect + 用量│
  │  ┌────────┬────────┬────┬──────┬────┐   │
  │  │物料编码 │物料名称 │规格 │ 用量  │操作│   │
  │  ├────────┼────────┼────┼──────┼────┤   │
  │  │INK-01  │水性油墨 │..  │0.05kg│删除│   │
  │  └────────┴────────┴────┴──────┴────┘   │
  │                           [关闭]        │
  └──────────────────────────────────────────┘

点击 [参数] → 工序参数弹窗：
  ┌──────────────────────────────────────────┐
  │  工序参数 — 印刷                     [×]  │
  │  [从模版初始化] → 从 param_template 批量复制│
  │  ┌──────────┬──────────┐                 │
  │  │ 参数名称  │ 参数值    │                 │
  │  ├──────────┼──────────┤                 │
  │  │ 印刷色数  │ 4色      │                 │
  │  │ 印刷速度  │ 120 m/min│                 │
  │  └──────────┴──────────┘                 │
  │                [确定(批量保存)] [关闭]     │
  └──────────────────────────────────────────┘
```

**关键交互**：
- 工序组成：点击 [添加工序] → 弹出工序选择器（`itemSelect` 风格弹窗），选中后填充到表格
- 关系选择：不再手动选下一道工序，只选关系（SS/FS/CC），下一道工序按序号+1 自动计算
- 关联产品：点击 [关联产品] → ItemSelect 弹窗搜索物料 → 选中后自动填入
- 产品 BOM：按工序筛选物料，下拉选择工序 + ItemSelect 选物料 + 输入用量
- 参数标准值：从模版复制默认值，可微调

**单元测试**：
- 路线产品关联 → 自动生成 `route_product_bom` 和 `route_process_param` 完整性
- `order_num` 自动编号逻辑

---

### Phase 3：生产工单 ⚠️ 后端完成 / 前端骨架

**后端**（4 个 entity）：
9. `ProWorkorder` — 生产工单 (`qxx_pro_workorder`)
10. `ProWorkorderBom` — 工单 BOM (`qxx_pro_workorder_bom`)
11. `ProWorkorderParam` — 工单工序参数 (`qxx_pro_workorder_param`)
12. `ProWorkorderChange` — 工单变更记录 (`qxx_pro_workorder_change`)

#### 3.1 BOM 数据源 — 单一来源，不做合并

```
★ 工单 BOM 的唯一数据源 = qxx_pro_route_product_bom
  "路线已绑定 产品 + 工序 + 物料 + 用量" → 这就是工单的完整用料清单

qxx_md_product_bom 的定位：
  仅做「可选校验参考」—— 工单 BOM 调整完成后，前端自动对比 md_product_bom，
  差异项高亮提示（"该物料与标准BOM不一致"），但不阻止提交。

为什么不做双源合并？
  ① route_product_bom 已含所有工序物料需求，数据完整
  ② md_product_bom 无工序关联，粒度不匹配 → 合并会产出重复行
     （同一物料同时出现在两个源中，用户困惑"哪个用量是对的？"）
  ③ 两源用量可能不一致（路线 BOM 更精确），合并逻辑复杂且容易出 bug
```

#### 3.2 工单创建流程

```
① 选择产品 → ② 选择工艺路线
    │
    ▼
③ 从 qxx_pro_route_product_bom 加载标准 BOM
   用量 = 用料比例 quantity × 计划数量 × (1 + scrap_rate)
    │
    ▼
④ BOM 调整界面（可偏离标准 BOM）
   物料替换 / 用量调整 / 新增 / 删除
   ⚠ 调整后自动对比 md_product_bom → 差异项高亮提示（非强制）
    │
    ▼
⑤ ★ SKU/变体（使用 qxx_md_item.parent_id 自引用）
	   ★ 仅在工单向导 Step2 触发，物料页面不提供"新增变体"按钮。
	   ★ SPU 自身即标准，物料建档时不自动创建冗余"标准变体"。
	   当 BOM 调整导致产出物与标准产品有实质差异 → 前端弹窗询问：
	   "是否将此定制规格保存为新产品变体（SKU）？"
	   ├── 选"是" → 后端 createWorkorderWithBom 事务内：
	   │   ├── ① INSERT qxx_md_item（parent_id=原SPU, item_code=skuCode, 差异字段回填）
	   │   ├── ② 复制父产品行业属性子表（MdItemServiceImpl.insertMdItem 自动处理）
	   │   ├── ③ 复制父产品 qxx_md_item_batch_config（变体同享批次追踪规则）
	   │   ├── ④ 工单 product_id / product_code 替换为变体
	   │   ├── ⑤ 调用 copyRouteProductForSku 复制工艺路线关联
	   │   └── ⑥ INSERT qxx_pro_workorder_change 记录变体创建事件
	   ├── 选"否" → 沿用原物料编码（不创建变体）
	   └── 返单时：建工单 Step1 选 SPU → 系统列出该 SPU 已有变体供选择
    │
    ▼
⑥ 写入 qxx_pro_workorder_bom（工单 BOM 快照，独立存储）
⑦ 复制 route_process_param → workorder_param
⑧ 若 batch_config.lot_number_flag='1' → 调用 autoCodeRule 自动生成批号
```

#### 3.3 物料齐套检查（实时计算，不持久化）

```
开工校验(workorderId)：
  ① 读取 workorder_bom 物料清单
  ② 逐项实时查询 wm 物料库存
  ③ 生成齐套报告：
     ✅ 满足行  /  ⚠ 缺料行（差额明细）
  ④ 全部满足 → 允许开工（status → PRODUCING）
     有缺料   → [确认开工] 禁用，提示"物料不齐套，请先处理缺料"

不持久化的原因：
  ✅ 开工校验是「实时快照」— 10分钟后库存可能已变化（新到货/被其他工单占用）
  ✅ 持久化产生「过期数据」— 库存已补足但记录仍是OPEN，需要额外生命周期管理
  ✅ 审计可通过 workorder 状态变更时间戳推断缺料延迟天数
  ❌ 缺点：无法做缺料频率统计（但 MVP 阶段非硬需求）
```

#### 3.4 工单变更管理

已审核工单（状态 ≠ PREPARE）不可直接修改，需走变更流程：

```
工单变更流程：
  ① 操作人提交变更申请 → INSERT qxx_pro_workorder_change (status=PENDING)
  ② 审批人审核 → status=APPROVED/REJECTED
  ③ 审批通过 → 执行变更（更新 workorder / workorder_bom）
  ④ 变更记录永久留存（追溯审计）
```

#### 3.5 事务 & Redis 锁

```java
@Autowired private RedisLockTemplate lockTemplate;
@Autowired private PlatformTransactionManager transactionManager;
private TransactionTemplate txTemplate;

@PostConstruct void initTx() { this.txTemplate = new TransactionTemplate(transactionManager); this.txTemplate.setTimeout(30); }

// 工单创建：TransactionTemplate（多表写入，无并发冲突）
public ProWorkorder createWorkorderWithBom(...) {
    return txTemplate.execute(status -> { /* INSERT workorder + BOM + param */ return workorder; });
}

// 工单编辑/状态变更：RedisLockTemplate + TransactionTemplate
public void updateWorkorder(...) {
    lockTemplate.execute("pro:workorder:lock:" + id, 5, () ->
        txTemplate.executeWithoutResult(status -> { /* UPDATE */ }));
}
```

| 场景 | 锁 Key | 原理 |
|------|--------|------|
| 工单创建 | 无锁 | TransactionTemplate 多表写入 |
| 工单编辑/状态变更/取消 | `pro:workorder:lock:{id}` 5s | Redis 锁 防并发 |

#### 3.6 UI 交互（向导式 3 步）

**列表页**：
```
┌─ 搜索：工单编码/产品/状态/需求日期 [搜索][重置] [刷新] ──────────┐
│  [新增] [修改] [删除] [开工校验] [完工] [取消]        [导出]      │
├──┬─────────┬──────┬────────┬──────┬──────┬──────┬──────┤
│☐│ 工单编码 │ 产品  │ 计划数量│已生产│ 状态  │ 交期  │ 操作  │
├──┼─────────┼──────┼────────┼────────┼──────┼──────┼──────┤
│☐│ WO-001  │ ZD-01│ 10000  │ 2000  │生产中 │06-20 │ 🎬✎🗑👁│
│☐│ WO-002  │ ZD-02│ 5000   │ 0     │待生产 │06-25 │ 🎬 ✓ ✎🗑👁│
└──┴─────────┴──────┴────────┴────────┴──────┴──────┴──────┘

操作栏按钮条件渲染：
  v-if="row.status == 'PREPARE'"  → [开工] [齐套检查] [修改] [删除] [查看]
  v-if="row.status == 'PRODUCING'" → [变更申请] [完工(→COMPLETED)] [查看]
  v-if="row.status == 'COMPLETED'" → [查看]
  v-if="row.status == 'CANCEL' || row.status == 'CLOSED'" → [查看]
```

**新增工单 — 3 步向导**：
```
Step1-基本信息：
┌──────────────────────────────────────────────┐
│  编码[◎自动○手动] ████  名称 ████              │
│  类型[◎自制 ○外协]                             │
│  产品[🔍选择物料] → ItemSelect 弹窗搜索          │
│        ZD-01 / 纸袋A                           │
│  工艺路线[▼] ← 根据产品自动加载可选路线           │
│  计划数量 ████  需求日期 📅 ████               │
│  客户订单号 ████  订单类型[▼新单/返单]           │
│  产品尺寸 ████  绳料规格 ████                   │
│  印刷要求 ████  包装要求 ████                   │
│                         [下一步：BOM调整]       │
└──────────────────────────────────────────────┘

Step2-BOM调整：
┌──────────────────────────────────────────────┐
│  标准 BOM（从 route_product_bom 加载）         │
│  用量 = 单位用量 × 计划数量 × (1+损耗率)        │
│  [新增物料行] [删除选中行]                      │
│  ┌──┬────┬────────┬──────┬──────┬──────┬────┐ │
│  │☐│所属│ 物料   │物料名称│单位用量│预计总量│操作│ │
│  ├──┼────┼────────┼──────┼──────┼──────┼────┤ │
│  │☐│印刷│ INK-01 │水性油墨│0.05kg│ 500kg│ ✎ │ │
│  │☐│制袋│ GLU-01 │白乳胶 │0.10kg│1000kg│ ✎ │ │
│  └──┴────┴────────┴──────┴──────┴──────┴────┘ │
│                                                │
│  ★ 若BOM偏离标准 → 提示：                       │
│   ⚠ "BOM已偏离标准，是否创建SKU变体物料？"       │
│      [是，创建新SKU] [否，沿用原物料编码]         │
│      SKU 编码：███████ (如 ZD-01-V1)            │
│      "本工单将生产 SKU变体，原产品编码将被替换"    │
│                   [上一步] [下一步：参数确认]     │
└──────────────────────────────────────────────┘

Step3-参数确认：
┌──────────────────────────────────────────────┐
│  标准参数值（从路线复制，计划员可按本单调整）     │
│  ┌──────┬──────────┬──────────┬────────┬────┐ │
│  │ 工序  │ 参数名称  │ 标准值    │ 调整值  │原因│ │
│  ├──────┼──────────┼──────────┼────────┼────┤ │
│  │ 印刷  │ 印刷色数  │ 4色      │ 5色    │客户│ │
│  │ 印刷  │ 印刷速度  │ 120 m/min│ -      │ -  │ │
│  └──────┴──────────┴──────────┴────────┴────┘ │
│                   [上一步] [提交] [取消]        │
└──────────────────────────────────────────────┘
```

**开工校验交互**：
```
点击 [开工校验] →
  ① loading "正在检查物料库存…"
  ② 弹出齐套报告：
     ┌────────────────────────────────────┐
     │ 物料齐套检查 - WO-001          [×] │
     │ ┌────────┬──────┬──────┬──────┬──┐ │
     │ │ 物料    │ 需求  │ 库存  │ 状态  │  │ │
     │ ├────────┼──────┼──────┼──────┼──┤ │
     │ │ INK-01 │ 500kg│ 800kg│ ✅  │  │ │
     │ │ GLU-01 │1000kg│ 200kg│ ⚠缺料│  │ │
     │ └────────┴──────┴──────┴──────┴──┘ │
     │ 齐套率：50% (1/2)                  │
     │ ⚠ 缺料清单已生成，请通知采购       │
     │           [确认开工] [关闭]         │
     └────────────────────────────────────┘
  ③ 全部满足 → [确认开工] → status=PRODUCING
     不满足 → [确认开工] 按钮禁用 + toast 提示
```

#### 3.7 单元测试 vs 集成测试

**单元测试（Mockito，mock 所有 Mapper + RedisLockTemplate + TransactionTemplate）**：
```
ProWorkorderServiceUnitTest:
  ├── testBomQuantityScaling        ← 单位用量 × 计划数 × (1+损耗率)
  ├── testBomAdjustmentLogic        ← 替换/增删物料行逻辑
  ├── testSkuVariantCreation        ← parent_id 设置 + 行业属性继承 + batch_config 复制
  ├── testSkuRouteCopy              ← 变体创建时调用 copyRouteProductForSku
  ├── testStatusTransitionLegal     ← PREPARE→PRODUCING 合法
  ├── testStatusTransitionIllegal   ← COMPLETED→PRODUCING 抛异常
  ├── testMaterialAvailabilityCheck ← 库存不足 → 缺料清单
  ├── testBatchCodeAutoGen          ← batch_config.lot_number_flag='1' → 自动生成
  └── testChangeOrderWorkflow       ← 变更申请→审批→执行
```

**集成测试（@SpringBootTest，真实 Spring + DB + Redis）**：
```
ProWorkorderServiceIntegrationTest:
  ├── testCreateWorkorder_FullChain          ← 工单+BOM+参数全部持久化
  ├── testCreateWorkorder_Rollback           ← BOM物料不存在 → 全部回滚
  ├── testCreateWorkorder_SkuVariant         ← 变体物料写入 md_item + batch_config + 路线复制
  ├── testConcurrentEditWithRedisLock        ← 两个线程同时编辑 → 一个抛 ServiceException
  ├── testRedisLockAutoRelease               ← 编辑完成后锁释放
  └── testSkuRouteCopy                       ← SKU 路线复制完整性验证
```

#### 3.8 Phase 3 剩余工作

- [ ] 恢复工序管理完整页面（当前 placeholder → 含作业内容 + 参数模版子表）
- [ ] 工单创建后端支持 BOM+参数一起提交（当前 addWorkorder 只保存 header）
- [ ] 工单变更管理前端页面
- [ ] 物料齐套检查前端交互
- [ ] 单元测试（9 个）+ 集成测试（6 个）

---

### Phase 4：生产执行 + wm 领料退料 ❌ 未开始

#### 4.0 wm 生产领料 & 退料实现（本次需补充）

**wm 模块现状**：`WmMiscIssue`/`WmMiscIssueLine`（杂项领料）已实现，**生产领料/退料未实现**。

**需新增**（参照已有 wm CRUD 模式）：

| 功能 | Domain | 表 | 说明 |
|------|--------|---|------|
| 领料单头 | `WmIssueHeader` | `qxx_wm_issue_header` | issue_type=PRODUCE, 关联 workorder_id |
| 领料单行 | `WmIssueLine` | `qxx_wm_issue_line` | 每物料一行, quantity_issue=申请数 |
| 领料明细 | `WmIssueDetail` | `qxx_wm_issue_detail` | 按批次/库位拆分, 实际出库数 |
| 退料单头 | `WmRtIssue` | `qxx_wm_rt_issue` | 关联原领料单 + workorder_id + 退料质检单 |
| 退料单行 | `WmRtIssueLine` | `qxx_wm_rt_issue_line` | 每物料一行, quantity_rt=退料数 |
| 退料明细 | `WmRtIssueDetail` | `qxx_wm_rt_issue_detail` | 按批次/库位拆分, 实际退库数 |

**领料业务流程**：

```
工单开工 → 创建领料单（根据 workorder_bom 生成领料行）
  ├── Header: issue_type=PRODUCE, workorder_id, 领料日期
  ├── Line:  每个物料一行, quantity_issue=申请领料数量
  └── Detail: 按批次/库位拆分, quantity=实际出库数量

领料过账 → WmTransactionServiceImpl.processTransaction()
  ├── 扣减库存 (transaction_type=ISSUE, 负数量)
  ├── 写入 material_trace (投料追溯: BATCH→CARD)
  └── 更新 card_process.issue_detail_id
```

**退料业务流程**：

```
生产退料（余料/不合格料退回仓库）
  ├── Header: 关联原 issue_id + workorder_id
  ├── Line:  quantity_rt ≤ 原领料数量 - 已退数量
  └── 退料过账 → processTransaction()
       ├── 增加库存 (transaction_type=RETURN, 正数量)
       ├── 质检判定（不合格→退回供应商 / 合格→回库）
       └── 写入 trace (退料追溯)
```

#### 4.1 生产执行实体

| 实体 | 表 | 说明 |
|------|---|------|
| **ProTask** | `qxx_pro_task` | 生产排产（工作站/机台/甘特图） |
| **ProFeedback** | `qxx_pro_feedback` | 报工记录（含 feedback_type 区分自制/外发代填/外发直填） |
| **ProFeedbackParam** | `qxx_pro_feedback_param` | 报工实际参数值（偏差判定 is_deviation） |
| **ProCard** | `qxx_pro_card` | 流转卡（工单→批次拆分，赋码打印） |
| **ProCardProcess** | `qxx_pro_card_process` | 流转卡工序流转跟踪 |
| **ProWorkrecord** | `qxx_pro_workrecord` | 上下工记录（工时统计） |
| **ProUserWorkstation** | `qxx_pro_user_workstation` | 用户工作站绑定 |

#### 4.2 流转卡 = 工单批次拆分

```
工单 WO-001 (计划数量 10000)
  ├── 流转卡 CRD-001 (quantity=3000, batch_code=B-001)  → 第1批次
  ├── 流转卡 CRD-002 (quantity=3000, batch_code=B-002)  → 第2批次
  └── 流转卡 CRD-003 (quantity=4000, batch_code=B-003)  → 第3批次

每张流转卡独立跟踪工序流转、独立报工，最后汇总到工单。
流转卡本身就是工单→批次的拆分载体，不再额外拆分。
```

#### 4.3 UI 交互

**排产** (`schedule/index.vue`)：
```
┌─ 左侧：待排产工单 ───────────┬─ 右侧：甘特图/任务列表 ────┐
│ 搜索 ████ [搜索]              │ 工作站[▼全部]              │
│ ☐ WO-001 纸袋A 10000         │ ┌─ 6/16 ─┬─ 6/17 ─┐       │
│ ☐ WO-002 纸袋B 5000          │ │████印刷 │         │       │
│ [查看选中工单工序]             │ │(绿)    │████裱纸 │       │
└──────────────────────────────┴─┴─────────┴─────────┴──────┘

① 勾选工单 → ② [查看工序] → ③ 弹出工序列表 → ④ 点击[→排产] →
   ┌──────────────────────────────┐
   │ 排产设置 - 印刷工序      [×]  │
   │ 工作站[🔍]  机台号 ████       │
   │ 排产数量 ████ (默认=工序剩余)  │
   │ 调机时长 ███ 分钟             │
   │ 单位耗时 ██.█ 分钟/个         │
   │ 开始时间 📅 ████              │
   │ 预计完成 (自动计算)            │
   │                 [取消][确定]   │
   └──────────────────────────────┘
```

**报工** (`feedback/index.vue`)：
```
┌─ 搜索：报工编码/工单/工作站/状态/日期 [搜索][重置] [刷新] ───┐
│  [新增报工] [修改] [删除] [确认] [审核] [撤回]      [导出]    │
├──┬─────────┬──────┬──────┬──────┬──────┬──────┬────┬────┤
│☐│ 报工编码 │ 工单  │ 工序  │ 工作站 │合格  │不合格 │状态│操作│
└──┴─────────┴──────┴──────┴──────┴──────┴──────┴────┴────┘

新增报工弹窗（3 Tab）：
  [报工信息] [工序参数] [物料消耗]

  Tab1-报工信息：
    任务[🔍选择任务] → 自动填充：工单、产品、工序、工作站、排产数量
    排产数量：5000  已报：2000  剩余可报：3000
    本次数量 ████ (不可超剩余可报)
    合格 ████  不合格 ████  工废 ████  料废 ████  待检测 ████
    ★ 生产批号：[◎自动生成(if batch_config.lot_number_flag='1') ○手动]

  Tab2-工序参数（偏差自动判定）：
    ┌──────┬────────┬───────┬───────┬──────┬────┐
    │ 工序  │ 参数名  │ 标准值  │ 实际值  │ 偏差  │说明│
    ├──────┼────────┼───────┼───────┼──────┼────┤
    │ 印刷  │ 印刷色数│ 4色   │ 4色   │ ✅正常│ -  │
    │ 印刷  │ 印刷速度│120m/min│135m/min│⚠偏差│赶工│
    └──────┴────────┴───────┴───────┴──────┴────┘
    ⚠ 偏差行 → 自动通知质检(sys_message) + 生成待办(sys_todo_list)
```

**流转卡** (`procard/index.vue`)：
```
┌─ 搜索：流转卡编码/工单/状态 [搜索][重置] [刷新] ────────────┐
│  [新增] [修改] [删除] [打印标签] [赋码]           [导出]     │
├──┬─────────┬──────┬──────┬────────┬──────┬──────┬────┬────┤
│☐│ 流转卡编码│ 工单  │ 产品  │当前工序 │流转数│ 状态 │赋码│操作│
└──┴─────────┴──────┴──────┴────────┴──────┴──────┴────┴────┘

点击[查看] → 工序流转详情（只读）：
  ┌──────────────────────────────────────────┐
  │ 工单WO-001 产品ZD-01  当前工序：印刷       │
  │ 赋码 [@BarcodeImage]                      │
  │ ────────────────────────                  │
  │ ┌──┬──────┬──────┬──────┬──────┬────┬──┐  │
  │ │序│ 工序  │ 投入  │ 产出  │不合格 │工作站│人│  │
  │ │1 │ 印刷  │ 3000 │ 2980 │ 20   │WS01│张│  │
  │ │2 │ 裱纸  │  -   │  -   │  -   │ -  │ -│  │
  │ └──┴──────┴──────┴──────┴──────┴────┴──┘  │
  │                         [打印标签][关闭]   │
  └──────────────────────────────────────────┘
```

#### 4.4 报工提交流程 & Redis 锁 + TransactionTemplate

这是整个 pro 模块**最复杂的事务场景**，采用与 `WmTransactionServiceImpl` 完全相同的模式：

```java
@Autowired private RedisLockTemplate lockTemplate;
@Autowired private PlatformTransactionManager transactionManager;
private TransactionTemplate txTemplate;

@PostConstruct
void initTx() {
    this.txTemplate = new TransactionTemplate(transactionManager);
    this.txTemplate.setTimeout(30);
}

// ★ 正确模式：lockTemplate.execute() → txTemplate.execute()
public ProFeedback submitFeedback(ProFeedback feedback, List<ProFeedbackParam> params) {
    String lockKey = "pro:task:lock:" + feedback.getTaskId();
    return lockTemplate.execute(lockKey, 10, () ->
        txTemplate.execute(status -> doSubmitFeedback(feedback, params))
    );
}
// 执行顺序：获取 Redis 锁 → 开启 DB 事务 → 读 task → 校验 → 写反馈 → 更新数量 → trace → 判完工 → 提交事务 → 释放锁

private ProFeedback doSubmitFeedback(ProFeedback feedback, List<ProFeedbackParam> params) {
    // 1. 锁内读取当前数量（保证读一致性）
    ProTask task = taskMapper.selectProTaskByTaskId(feedback.getTaskId());

    // 2. 校验：feedback.quantity ≤ task.quantity - task.quantityProduced
    if (feedback.getQuantityFeedback().compareTo(
            task.getQuantity().subtract(task.getQuantityProduced())) > 0) {
        throw new ServiceException("报工数量超出排产剩余数量");
    }

    // 3. INSERT qxx_pro_feedback
    // 4. BATCH INSERT qxx_pro_feedback_param（实际值 vs 标准值 → is_deviation 判定）
    // 5. UPDATE qxx_pro_task（累加 quantity_produced / qualified / unqualified）
    // 6. UPDATE qxx_pro_workorder（累加 quantity_produced）
    // 7. INSERT qxx_pro_material_trace（产出追溯）
    // 8. 判完工：task满量→COMPLETED；所有task完成→workorder COMPLETED

    return feedback;
    // 任何一步抛异常 → TransactionTemplate 回滚 → finally 释放 Redis 锁
}
```

#### 4.5 报工撤回 & 偏差预警 & 审批流

- **撤回**：`status=REVOKED` → 反向冲销 task/workorder 数量 → 原 feedback 记录保留（不物理删除）
- **偏差预警**：`is_deviation='Y'` → 自动写入 `sys_message`（通知质检/工艺）+ `sys_todo_list`（生成待办）
- **审批流**：`CONFIRMED→AUDITED` 需审批人确认，支持超时自动确认

#### 4.6 锁策略

| 场景 | 锁 Key | 等待 | 说明 |
|------|--------|------|------|
| 报工提交 | `pro:task:lock:{taskId}` | **10s** | 防并发超报，等待时间放宽（含参数校验） |
| 排产调整 | `pro:task:lock:{taskId}` | 5s | 防与报工并发冲突 |
| 流转卡工序完成 | `pro:card:lock:{cardId}` | 5s | 防重复完成同一工序 |
| 工单编辑 | `pro:workorder:lock:{workorderId}` | 5s | 防并发修改覆盖 |

#### 4.7 单元测试 vs 集成测试

**单元测试（Mockito）**：
```
ProFeedbackServiceUnitTest:
  ├── testValidateOverProduction      ← Mock task剩余100, fbQty=200 → ServiceException
  ├── testValidateExactMatch          ← Mock task剩余100, fbQty=100 → 通过
  ├── testFeedbackParamDeviation      ← Mock template.max=10, actual=15 → is_deviation='Y'
  ├── testFeedbackParamNormal         ← Mock template.max=10, actual=8 → is_deviation='N'
  ├── testStatusTransitionLegal       ← 合法/非法状态流转
  ├── testRevokeRollback              ← 撤回→数量冲销→workorder状态回退
  └── testBatchCodeAutoGen            ← batch_config.lot_number_flag='1' → 自动生成

ProCardServiceUnitTest:
  ├── testCardProcessComplete         ← 工序完成→current_process_id 更新
  ├── testCardBatchRelation           ← 多流转卡数量汇总 ≤ workorder.quantity
  └── testMaterialTraceWrite          ← trace 记录正确写入
```

**集成测试（@SpringBootTest）**：
```
ProFeedbackServiceIntegrationTest:
  ├── testSubmitFeedback_FullChain          ← 真实写入：feedback+params+task更新+workorder更新+trace
  ├── testSubmitFeedback_Rollback           ← 中途失败→全部回滚
  ├── testSubmitFeedback_Concurrent         ← 两个线程同时报工→一个ServiceException
  ├── testRevoke_QuantityRestored           ← 撤回→数量恢复→workorder状态回退
  ├── testRedisLockReleaseAfterException    ← 异常后锁正常释放
  └── testWorkorderAutoComplete             ← 最后一个task完成→workorder COMPLETED

ProCardServiceIntegrationTest:
  ├── testCardProcessComplete               ← 工序完成→状态更新
  └── testCardProcessDuplicateComplete      ← 重复完成→Redis锁阻塞/抛异常

WmIssueServiceIntegrationTest:
  ├── testIssueCreate                       ← 领料单创建+行+明细完整流程
  ├── testIssuePost                         ← 领料过账→库存扣减+trace写入
  ├── testRtIssueCreate                     ← 退料单创建+关联原领料单
  └── testRtIssuePost                       ← 退料过账→库存增加
```

---

### Phase 5：生产看板 + 物料追溯 ❌ 未开始

#### 生产看板 (`dashboard/index.vue`)

```
┌────────────────────────────────────────────────────┐
│  生产进度看板                      刷新 🔄 自动刷新 │
├──────────┬──────────┬──────────┬──────────┬────────┤
│ 在制工单 │ 今日报工  │ 今日合格  │ 今日不合格│ 齐套率  │
│   23    │  15,200  │  14,800  │   400    │  87%   │
├──────────┴──────────┴──────────┴──────────┴────────┤
│  ┌─ 工单进度(TOP10) ────┐  ┌─ 工序产出趋势 ──────┐ │
│  │ WO-001 ████████ 80%  │  │ 📈 ECharts 折线图    │ │
│  │ WO-002 ██████ 60%    │  │ (近7天每日产出量)    │ │
│  │ WO-003 ████ 35%      │  └────────────────────┘ │
│  └──────────────────────┘                         │
│  ┌─ ⚠ 延迟预警 ──────────────────────────────┐    │
│  │ WO-005 │ 纸袋C │ 需求6/15 │ 超期2天 │ 生产中│    │
│  │ WO-008 │ 礼盒A │ 需求6/16 │ 超期1天 │ 待生产│    │
│  └──────────────────────────────────────────┘    │
└────────────────────────────────────────────────────┘
```

#### 物料追溯 (`materialtrace/index.vue`)

```
┌─ 追溯方向[◎正向 ○反向]  起点[🔍选择流转卡/批次/卷料] [搜索] ─┐
│                                                               │
│ ┌──────┐   ┌────────┐   ┌──────┐   ┌──────┐                  │
│ │BATCH-A│──→│CRD-001 │──→│BATCH-B│──→│BATCH-C│                 │
│ │500kg │   │工序:印刷│   │450kg │   │400kg │                  │
│ └──────┘   └────────┘   └──────┘   └──────┘                  │
│     │                                       │                  │
│     │         ┌────────┐                    │                  │
│     └────────→│CRD-002 │←───────────────────┘                  │
│              │工序:裱纸│                                       │
│              └────────┘                                       │
│  节点可点击 → 展示详情（数量/时间/操作人/关联单据号）            │
└───────────────────────────────────────────────────────────────┘
```

---

### 已推迟

- **安灯系统** (`qxx_pro_andon_config` / `qxx_pro_andon_record`)
- **外协管理独立模块**（发料→外协加工→回厂验收的独立页面）
- **排产优化引擎**（有限产能排程、倒排/顺排策略）
- **OEE/效率统计**
- **设备数据采集**

---

## 三、页面间联动关系

```
                  ┌──────────────────────────────┐
                  │       工艺路线 (proroute)       │
                  │ 定义 产品→工序流程+BOM+参数    │
                  └───────────┬──────────────────┘
                              │ 加载路线 & BOM
                              ▼
┌──────────┐      ┌─────────────────┐      ┌──────────────┐
│ 工序管理  │─────→│   生产工单       │─────→│   生产排产    │
│(process) │ 引用  │  (workorder)    │ 拆分  │  (schedule)   │
└──────────┘      └──┬──────┬───────┘      └──────┬───────┘
                     │      │                     │
                     │      │ ★流转卡(拆批)       │ 排到工作站
                     │      ▼                     ▼
                     │  ┌──────────────┐   ┌──────────────┐
                     │  │   流转卡      │   │   报工记录    │
                     │  │  (procard)   │──→│  (feedback)  │
                     │  └──────────────┘   └──────┬───────┘
                     │   工序完成更新              │
                     │                            ▼
                     │                     ┌──────────────┐
                     │                     │  物料追溯     │
                     └────────────────────→│(materialtrace)│
                             写入追溯      └──────────────┘
```

---

## 四、Redis 分布式锁 + TransactionTemplate 总结

### 为什么不用 `@Transactional` 注解？

Spring AOP 代理无法拦截同类自调用（`this.doXxx()`），`@Transactional` 放在 private 方法上 **100% 失效**。正确做法：`TransactionTemplate`（编程式事务，直接操作 `PlatformTransactionManager`），与 `WmTransactionServiceImpl` 一致。

### 基础设施

- **Redisson 3.27.2** → `RedissonClient` → `RedisLockTemplate`（项目已集成，wm 模块生产验证）
- **TransactionTemplate** → `new TransactionTemplate(transactionManager)` → `txTemplate.execute(status → ...)`（`@PostConstruct` 创建，timeout=30s）

### 标准调用模式

```java
@Service
public class ProFeedbackServiceImpl {
    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    // ★ 唯一正确模式：lockTemplate.execute() → txTemplate.execute()
    public ProFeedback submitFeedback(ProFeedback feedback, List<ProFeedbackParam> params) {
        return lockTemplate.execute("pro:task:lock:" + feedback.getTaskId(), 10, () ->
            txTemplate.execute(status -> doSubmitFeedback(feedback, params))
        );
    }
    // 执行顺序：①获取Redis锁 → ②开启DB事务 → ③执行业务 → ④提交DB事务 → ⑤释放Redis锁
}
```

### 锁命名 & 适用矩阵

```
pro:task:lock:{taskId}           — 报工提交(10s)、排产调整(5s)
pro:workorder:lock:{workorderId} — 工单编辑、状态变更、取消
pro:card:lock:{cardId}           — 流转卡工序完成

Redis锁+TransactionTemplate:        仅 TransactionTemplate:
  ✅ 报工提交（并发超报）             ✅ 工单创建（多表写入，无并发）
  ✅ 工单编辑（并发覆盖）             ✅ 工单变更（变更记录+数据更新）
  ✅ 流转卡工序完成（防重复）
                                     无需锁/事务（单表自动提交）：
                                       ❌ 工序/路线/参数模版 CRUD
```

---

## 五、技术规范

### 后端规范（遵循 qixiaoxia-mes 已有模式）

**命名约定**：
```
表名:         qxx_pro_{entity}
Domain:       Pro{Entity} extends BaseEntity               → com.ruoyi.system.domain.mes.pro
Mapper:       Pro{Entity}Mapper                            → com.ruoyi.system.mapper.mes.pro
Mapper XML:   resources/mapper/mes/pro/Pro{Entity}Mapper.xml
Service:      IPro{Entity}Service / Pro{Entity}ServiceImpl → com.ruoyi.system.service.mes.pro
Controller:   Pro{Entity}Controller                        → com.ruoyi.web.controller.mes.pro
URL前缀:      /mes/pro/{entity}
权限前缀:     mes:pro:{entity}:
```

**Controller 端点**（每个 entity 6 个）：
```
@GetMapping("/list") → TableDataInfo (分页)    @PostMapping → AjaxResult (新增)
@PostMapping("/export") → void (导出)          @PutMapping → AjaxResult (修改)
@GetMapping("/{id}") → AjaxResult (详情)       @DeleteMapping("/{ids}") → AjaxResult (删除)
```

**关键点**：
- Factory ID 由 `FactoryIdInterceptor` MyBatis 插件自动注入，代码中不手动设置
- 审计字段（createBy/createTime/updateBy/updateTime）在 ServiceImpl 中手动设置
- `@PreAuthorize` 注解在每个 Controller 端点

### 前端规范（遵循 qixiaoxia-mes 已有模式）

- `<script setup lang="ts">` Composition API
- Element Plus 组件，按钮统一 `size="small"` + `link`（非 `type="text"`）
- `useDict()` 加载字典，`<dict-tag>` 渲染，禁止硬编码枚举中文映射
- `v-hasPermi="['mes:pro:xxx:action']"` 权限控制
- 对话框主从表：`<el-tabs>` + 子组件 `v-if="form.id != null"`
- 自动编码：`genSerialCode('CODE_TYPE')` + toggle 开关
- SCSS scoped 样式，日期格式 `YYYY-MM-DD HH:mm:ss`

---

## 六、验证步骤

### 每阶段开发完成后执行：

**后端验证**：
1. 编译检查：`cd backend && mvn compile -pl ruoyi-admin -am`
2. 单元测试：`mvn test -pl ruoyi-system -Dtest="*Pro*Test"`
3. 集成测试：`mvn test -pl ruoyi-system -Dtest="*Pro*IntegrationTest"` (需 Redis/DB)
4. 启动后端：确认新 Controller 注册成功（Swagger UI 可见 `/mes/pro/*`）
5. 接口测试：用 curl 逐个测试 CRUD 端点

**前端验证**：
1. TypeScript 类型检查：`cd frontend && npx vue-tsc --noEmit`
2. ESLint 检查：`npm run lint`
3. Dev Server 启动：确认页面加载无 console error
4. 页面交互验证：搜索表单、新增/编辑弹窗、删除确认、分页、字典渲染、权限按钮

**端到端集成验证**：
```
完整流程：
  创建工序 → 创建路线+关联产品+标准BOM → 创建工单+BOM调整 
  → 开工校验(齐套检查) → 排产到工作站 → 领料出库 → 打印流转卡 
  → 报工(填入实际参数+偏差判定) → 物料追溯查询 → 生产看板查看

每个环节验证：
  - 数据库数据正确性
  - 事务回滚正确性（模拟异常场景）
  - 并发场景正确性（Redis 锁验证）
```

---

## 七、文件清单估算

| 层次 | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Phase 5 | 合计 |
|------|---------|---------|---------|---------|---------|------|
| 后端 Entity | 3 | 5 | 4 (+变更) | 7 | 1 | **20** |
| 后端 Mapper | 3 | 5 | 4 | 7 | 1 | **20** |
| 后端 Mapper XML | 3 | 5 | 4 | 7 | 1 | **20** |
| 后端 Service 接口 | 3 | 5 | 4 | 7 | 1 | **20** |
| 后端 ServiceImpl | 3 | 5 | 4 | 7 | 1 | **20** |
| 后端 Controller | 3 | 5 | 4 | 7 | 1 | **20** |
| **wm 领料 Entity** | — | — | — | **3** | — | **3** |
| **wm 退料 Entity** | — | — | — | **3** | — | **3** |
| **wm 领料+退料 Mapper/Service/Controller** | — | — | — | **~18** | — | **18** |
| 后端 单元测试 | 2 | 2 | **9** | **8** | — | **21** |
| 后端 集成测试 | 2 | 2 | **6** | **10** | — | **20** |
| 前端页面 (.vue) | 1 | 2 | **3** | **6** | **2** | **14** |
| 前端 API (.ts) | 3 | 5 | **4** | **6** | 1 | **19** |
| 前端类型定义 | 1 | 1 | 1 | 1 | 1 | **5** |
| DDL 变更脚本 | 1 (is_check) | 0 | **1** (变更表) | 0 | 0 | **2** |

总计约 **225 个文件**，按 Phase 分批实现，每阶段独立可验证。

安灯系统（2 entity + 2 页面 + 2 API + ~14 文件）推迟至后续迭代。
外协管理独立模块推迟至后续迭代。
排产优化/OEE/设备数据采集推迟至后续迭代。
