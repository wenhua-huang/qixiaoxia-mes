# MES 质检模块（qc）设计文档

- 日期：2026-08-16
- 状态：待用户审阅
- 范围：后端 + PC 前端（一期）；移动端 app、样品级结果、质量报表为二期
- 前置文档：`docs/设计文档/数据库设计/mes-qc.sql`（17 表设计稿，本设计在其基础上优化）

## 1. 背景与目标

qc 模块目前 0 实现：无 Flyway 表、无后端包、无前端页面。已有资产：

- 下游单据挂点列已预留：`qxx_wm_item_recpt.iqc_id/iqc_code`、`qxx_wm_product_recpt.ipqc_id/ipqc_code`、`qxx_pro_card_process.ipqc_id/ipqc_code`、`qxx_wm_product_sales.oqc_id/oqc_code`、`qxx_wm_rt_issue/rt_vendor/rt_sales.rqc_id/rqc_code`
- `qxx_pro_route_process.is_check`（是否检验工序）已存在，前端工序编辑页已有该开关
- `SysTodoList` 已有 `QC_CHECK`（质检）待办类型
- 报工已有 `quantityQualified/quantityUnqualified/quantityUncheck` 数量字段

目标：可配置的检查项目与检验模板 + 四类检验业务（IQC 来料检 / IPQC 工序检 / OQC 出厂检 / RQC 退料检）与现有业务单据打通，配置驱动的拦截放行。

### 1.1 关键产品决策（用户已确认 / 待复核标注）

| 决策 | 结论 | 状态 |
|------|------|------|
| 本期范围 | 检测项 + 检验模板 + IQC + IPQC + OQC + RQC | ✅ 用户已确认 |
| 数据模型 | 方案 C：4 张单头表 + 1 张统一行表 | ✅ 用户已确认 |
| 拦截强度 | **配置驱动拦截**：物料配了模板才强制检，未配置免检直通；CONCESSION（让步）可例外放行 | ⚠️ 推荐方案（提问未获回复），审阅时可推翻 |
| 行业硬编码 | 删除纸袋子表与 OQC 固定核对列，全部配置化为检测项 + 预置模板种子 | ⚠️ 产品优化决策，审阅时可推翻 |
| 样品级结果 | `qxx_qc_result/_detail` 后置二期 | ⚠️ 同上 |

## 2. 架构决策：方案 C

4 张检验单头表（各自特有字段，与下游挂点一一对应，代码生成器友好）+ 1 张统一行表 `qxx_qc_order_line`（`qc_type + qc_id` 多态关联，与缺陷记录表同模式）。表数 17 → 11。

否决的备选：
- 方案 A（照搬设计稿 8 张单据表）：行表/行编辑页/判定逻辑写 4 遍，违反项目"重复 ≥2 次必须抽公共"约束。
- 方案 B（1 张统一大单表）：4 类单据特有字段挤一张宽表，与挂点语义错位。

### 2.1 对设计稿的其他优化

1. **来源单据适配现实**：设计稿 IQC/OQC 来源（到货通知单、发货通知单）不存在。改为 IQC ← 采购入库单、OQC ← 销售出库单、IPQC ← 流转卡工序/产品入库单、RQC ← 生产退料单等。不改下游表结构。
2. **IPQC 补 `card_id/card_code`**：实际挂点在流转卡工序，设计稿缺失。
3. **`workstation_id` 改可空**：巡检不一定绑定工位。
4. **`template_product` 增加可空 `process_id/process_code/process_name`**：IPQC 按工序配模板（印刷检色差、制袋检尺寸，同一产品不同工序不同检测项）。
5. **检测项补 `dict_type`**：字典型检测值需指定字典来源。

## 3. 数据模型（11 张表，Flyway V137）

通用约定：所有表含 `factory_id`（业务 INSERT 不写，拦截器注入；Flyway 种子 INSERT 显式写）；审计四字段（create_by/create_time/update_by/update_time）；`utf8mb4_unicode_ci`。

### 3.1 配置层（5 张）

**qxx_qc_index 检测项**：`index_code`(唯一)、`index_name`、`index_type`(IQC/IPQC/OQC/RQC)、`qc_tool`、`qc_result_type`(NUMBER/TEXT/DICT/FILE/COUNT)、`dict_type`(DICT 型字典类型，新增)、`qc_result_spc`(值属性，如长度mm/色差ΔE)、`enable_flag`。

**qxx_qc_template 检验模板**：`template_code`(唯一)、`template_name`、`qc_types`(适用检验种类，多选逗号分隔)、`enable_flag`。

**qxx_qc_template_index 模板-检测项**：`template_id`、`index_id` + 冗余编码名称、`check_method`、`stander_val`(标准值)、`unit_of_measure`、`threshold_max/threshold_min`(上下限偏差)、`order_num`。

**qxx_qc_template_product 模板-物料**：`template_id`、`item_id` + 冗余信息、`process_id/process_code/process_name`(可空，IPQC 工序级绑定，新增)、`quantity_check`(抽检样本量)、`quantity_unqualified`(Ac 值)、`cr_rate/maj_rate/min_rate`(缺陷率阈值)。

> 模板查找规则：检验类型 + 物料 (+ IPQC 时工序) → 启用的模板。查不到 = 免检。
> 唯一性约束：同一 (检验类型, item_id, process_id) 只允许一条启用绑定，保存时后端校验，避免查找歧义。

**qxx_qc_defect 缺陷字典**：`defect_code`(唯一)、`defect_name`、`index_type`、`defect_level`(CRITICAL/MAJOR/MINOR)、`process_method`(返工/让步接收/退货/报废)、`enable_flag`。

### 3.2 单据层（6 张）

四张单头表统一字段：`{type}_code`(唯一，自动编码)、`{type}_name`、`template_id`、`source_doc_id/source_doc_type/source_doc_code/source_line_id`(来源溯源，拦截反查依据)、物料四件套（`item_id/item_code/item_name/specification`）、`unit_of_measure`、数量组（`quantity_check/quantity_qualified/quantity_unqualified`）、缺陷组（`cr/maj/min_quantity + cr/maj/min_rate`）、`check_result`(PASS/FAIL/CONCESSION)、`concession_reason`(让步理由，CONCESSION 必填)、`inspect_date/inspector`、`status`(PENDING/INSPECTING/COMPLETED/CLOSED)。

| 表 | 特有字段 | 来源单据类型 |
|----|---------|-------------|
| `qxx_qc_iqc` | `vendor_id/code/name/batch`、`quantity_received`、`quantity_min_check/quantity_max_unqualified`(取自模板物料配置快照) | `item_recpt` + `item_recpt_line` |
| `qxx_qc_ipqc` | `ipqc_type`(FIRST_CHECK 首检/TOUR_CHECK 巡检/LAST_CHECK 尾检/SPOT_CHECK 抽检)、`workorder_id/code/name`、`card_id/card_code`(流转卡，新增)、`task_id/code/name`、`process_id/code/name`、`workstation_id/code/name`(可空) | `card_process`(工序检)、`product_recpt`(完工检) |
| `qxx_qc_oqc` | `client_id/code/name`、`batch_code`、`quantity_out`、`quantity_min_check/quantity_max_unqualified` | `product_sales` + line |
| `qxx_qc_rqc` | `rqc_type`(PROD_RETURN/PURCHASE_RETURN/QC_REJECT)、`workorder_*`、`vendor_*`、`batch_id/batch_code`、`return_reason`、`responsibility`(SUPPLIER/PRODUCTION/STORAGE/OTHER) | `rt_issue` 等 |

> IPQC 检验结果枚举含 REWORK（返工）时统一映射：整单结果仍为 PASS/FAIL/CONCESSION 三值，返工信息走缺陷记录的 `process_method`。

**qxx_qc_order_line 统一检验单行**：`qc_type`(IQC/IPQC/OQC/RQC)、`qc_id`(多态主键)、`index_id` + 冗余编码/名称/工具/方法/标准值/单位/上下限（模板快照）、`qc_result_type`、`check_val_text`(实测文本值，NUMBER 型存数字字符串)、`cr/maj/min_quantity`(该检测项缺陷数)、`line_result`(PASS/FAIL)、`remark`。索引 `(qc_type, qc_id)`。

**qxx_qc_defect_record 缺陷记录**：`qc_type/qc_id/line_id`(多态)、`defect_id` + 冗余、`defect_level`、`defect_quantity`、`process_method`、`defect_image`(拍照留证，走已有文件上传)、`remark`。

### 3.3 删除/后置的设计稿表

| 设计稿表 | 处置 |
|---------|------|
| `qxx_qc_iqc_line/_ipqc_line/_oqc_line/_rqc_line`（4 张） | 合并为 `qxx_qc_order_line` |
| `qxx_qc_ipqc_attr_paper_bag` | 删除，检测项配置化 + 预置纸袋检测项种子 |
| `qxx_qc_result/_result_detail` | 二期（样品级逐个实测记录） |
| OQC 表 5 个固定核对列 | 删除，改为预置 OQC 默认模板检测项（装箱核对/外观/箱唛/封箱/托盘） |

## 4. 业务流程集成（配置驱动拦截）

核心原则：**拦截校验一律写在 Service 层**（`WmItemRecptServiceImpl` 等），因为同一动作有 PC 与 mobile 双端点（如产品入库 `/confirm` 与 `/mobile/confirm`），Controller 层拦截会漏。

### 4.1 IQC 来料检

库存动作时机（已核实源码）：`confirmItemRecpt`（DRAFT→CONFIRMED）即增加库存，`postItemRecpt` 仅回写 PO。因此拦截点在 **confirm**，检验单生成提前到**创建（DRAFT）阶段**——形成标准 IQC 流程：收货建单 → 检验 → 判合格 → 入库。

```
采购入库单创建(fromPurOrder / receive, DRAFT)
  → 创建时：按行物料查 IQC 模板，有模板 → 自动生成 PENDING 检验单
     (source=item_recpt/line，回填单头 iqc_id/iqc_code 存首张)
  → 质检员录入行结果+缺陷 → 判定(COMPLETED, PASS/FAIL/CONCESSION)
  → confirm 确认入库时（doConfirmItemRecpt 共享核心，天然覆盖 PC confirm
     与移动端一键收货 receiveWithLines）：按 source 反查全部关联检验单，
     需检物料均 COMPLETED 且 PASS/CONCESSION → 放行；否则拒绝并提示
  → FAIL 处置：引导走供应商退货 rt_vendor(现有功能，不自动生成)
```

已知一期限制：需检物料走移动端"一键收货"会被 confirm 校验拒绝（整事务回滚），须在 PC 端完成检验后再确认；移动端检验录入为二期。

### 4.2 IPQC 工序检 + 完工检

- **工序检生成**：报工确认（`ProFeedbackServiceImpl.confirm`，含 batchConfirm）时，若工序 `is_check='Y'` 且该物料(+工序)配了 IPQC 模板 → 生成 PENDING 检验单并回填 `card_process.ipqc_id/ipqc_code`。**弱拦截**：报工本身不阻断（报工已有 quantityUncheck 待检数量承接），流转卡流转下一工序时前端提示存在未完成检验。
- **首检/巡检**：质检员在 IPQC 列表手工创建（选工单/流转卡/工序/检验类型），不依赖报工。
- **完工检（LAST_CHECK）生成**：产品入库单创建（`fromWorkorder`，DRAFT）时按工单产品查 IPQC(LAST_CHECK) 模板生成，回填 `product_recpt.ipqc_id/ipqc_code`；**confirm 确认入库时拦截**（confirm 与 mobileConfirm 两个入口，库存动作在 confirm），规则同 IQC。

### 4.3 OQC 出厂检

库存动作时机（已核实源码）：`postOut`（出库确认）即扣减库存。

```
销售出库单创建(fromSaleOrder / 手动新增, DRAFT)
  → 创建时：按行物料查 OQC 模板，有模板 → 生成 PENDING 检验单(按物料去重，多物料多张)
  → postOut 出库确认(扣库存)前：需检物料均 COMPLETED 且 PASS/CONCESSION → 放行；否则拒绝
```

发运(shipment)不再重复拦截（出库确认已拦截，货已放行）。

### 4.4 RQC 退料检

生产退料单 `execute`（`WmRtIssueController`）前：按物料查 RQC 模板，有模板 → 生成 PENDING 检验单；判定合格 → 退回仓库，不合格 → 引导报废/供应商退货路径（记录 `responsibility` 责任归属）。一期不做独立返工单/报废单，处置方式记录在缺陷记录的 `process_method`。

### 4.5 多物料单据规则

检验单为单物料维度。多物料单据按行物料去重生成多张检验单；单头挂点列（`iqc_id` 等）回填首张检验单仅作展示，**拦截校验与状态查询一律按 `source_doc_type + source_doc_id` 反查**。

## 5. 判定引擎（公共服务，一套实现）

输入：行结果 + 缺陷记录 + 模板物料配置（样本量/Ac/缺陷率阈值）。输出：行判定 + 整单判定。

规则（全部后端实现，前端只展示）：

1. **数值型行判定**：`stander_val + threshold_min ≤ 实测值 ≤ stander_val + threshold_max` 为 PASS；`stander_val` 为空时 threshold 为绝对上下限。区间任一端为空则只校验另一端。
2. **非数值型行**（TEXT/DICT/COUNT/FILE）：质检员人工选择行结果 PASS/FAIL。
3. **整单判定**：不合格数 > Ac 值 → FAIL；任一 CRITICAL 缺陷 → FAIL；任一档缺陷率超阈值 → FAIL；否则 PASS。
4. **让步改判**：整单 FAIL 后质检员可改判 CONCESSION，必填 `concession_reason`。
5. 判定完成即 `status=COMPLETED`，行与缺陷记录不可再改；CLOSED 仅用于作废（来源单据作废时联动关闭）。

**并发**：判定提交、自动生成检验单均加 Redisson 分布式锁（锁粒度：检验单 ID / 来源单据 ID），先锁后事务（`TransactionTemplate`），遵守 backend/AGENTS.md 加锁规范（backend/CLAUDE.md 已将质检判定列为必须加锁场景）。

**待办**：自动生成检验单时创建 `QC_CHECK` 类型待办；判定完成后关闭待办（跟随 sys_todo_list 现有模式）。

## 6. 菜单、权限与页面

- 一级目录"质量管理"（首页 qc 占位卡片已存在）：检测项 / 检验模板 / 缺陷字典 / 来料检验单 / 过程检验单 / 出货检验单 / 退料检验单
- 权限：`mes:qc:{index|template|defect|iqc|ipqc|oqc|rqc|order_line}:{action}`
- 自动编码：4 套单据编码规则（`IQC-`/`IPQC-`/`OQC-`/`RQC-` + 日期 + 日序列，sys_auto_code_rule + 3 part，按日循环）
- 前端：`src/views/mes/qc/{index,template,defect,iqc,ipqc,oqc,rqc}/`、`src/api/mes/qc/*.ts`
- **共用组件**：检验行编辑 + 缺陷录入组件（4 个检验单页面共用），单页面 ≤300 行
- 检验单编辑交互：选物料带出模板 → 行表格（检测项快照）逐项录值/选结果 → 缺陷弹窗（选缺陷字典 + 等级 + 数量 + 拍照）→ 判定提交
- 下游单据页面：入库单/出库单列表增加"检验状态"展示与检验单跳转（按 source 反查，不依赖单头挂点列）

## 7. 错误处理

- 拦截拒绝统一返回业务错误码 + 明确提示（含检验单编码），前端 toast 展示
- 模板停用/删除保护：被引用的模板/检测项禁用 `enable_flag='0'`，不物理删除；生成检验单时校验模板启用状态
- 检验单生成后模板变更不影响已生成检验单（行数据是模板快照）
- 来源单据作废（cancel/close）时联动 `CLOSED` 其 PENDING/INSPECTING 检验单
- 自动生成幂等：confirm 重复调用、批量报工重复触发时，按 `source_doc_type + source_doc_id + item_id(+process_id)` 查重，已存在未关闭检验单则不重复生成

## 8. 测试策略

- 后端单测：判定引擎规则（边界值/Ac/缺陷率/让步）全覆盖
- Service 层集成测试：四类拦截点（item_recpt.confirm、product_recpt.confirm、product_sales.postOut、rt_issue.execute）放行与拒绝路径
- E2E：按 run-tests skill 红线——后端改动必须 `mvn -pl ruoyi-admin -am package -DskipTests` + 重启 + token 实测接口；前端页面浏览器实测
- 验收场景：①配模板来料 → 未完成检验不能确认入库 → 检验 PASS 后确认成功；②未配模板来料直接确认；③FAIL → 让步改判 CONCESSION → 确认成功；④isCheck 工序报工生成 IPQC 并回填 card_process；⑤多物料出库单生成多张 OQC

## 9. 实施波次

1. **波次 1**：Flyway V137（11 表 + 菜单 + 字典 + 自动编码 + 预置检测项/默认模板种子）+ 配置层 5 个 CRUD（生成器 + 定制）
2. **波次 2**：统一行表 + 判定引擎 + IQC 全流程（生成/录入/判定/待办/拦截）
3. **波次 3**：OQC
4. **波次 4**：IPQC（报工 hook + 完工检 + 手工首巡检）
5. **波次 5**：RQC + 缺陷记录查询 + 收尾自检（crt-review）

每波次完成后走后端验证红线（打包 → 重启 → 实测）；各波次同时包含对应下游单据页面的"检验状态"展示改造（见 §6）。

改动的现有业务代码点（供确认，均为 Service 层注入生成/校验逻辑）：
`WmItemRecptServiceImpl.insertWmItemRecpt+receiveWithLines(生成)/doConfirmItemRecpt(拦截)`、`WmProductRecptServiceImpl.insertWmProductRecpt(生成)/confirmProductRecpt+mobileConfirmProductRecpt(拦截)`、`WmProductSalesServiceImpl.insertWmProductSales(生成)/postOut(拦截)`、`WmRtIssueServiceImpl(生成/execute 拦截)`、`ProFeedbackServiceImpl.confirm/batchConfirm(IPQC 工序检生成)`。

## 10. 二期展望（不在本期）

移动端 app 检验分包（检验录入/拍照水印/离线暂存，app/AGENTS.md 已规划）、样品级检测结果（qxx_qc_result/_detail）、质量报表（合格率趋势/供应商质量排名/缺陷帕累托）、返工单/报废单联动、AQL 动态抽样方案。
