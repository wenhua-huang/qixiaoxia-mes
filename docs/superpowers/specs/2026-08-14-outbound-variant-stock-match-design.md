# 出库兼容变体库存（工单反查精确制导）

- 日期：2026-08-14
- 模块：wm（仓储）→ 销售出库
- 状态：设计待审

## 1. 背景与问题

### 1.1 现有变体机制回顾

系统无独立 variant 表，SKU 变体通过 `qxx_md_item.parent_id` 自引用表达：

- `parent_id = 0`：顶层 SPU / 标准产品（如 `FIN-BENQU-001`）
- `parent_id > 0`：变体 SKU，指向父 SPU（如 `FIN-BENQU-001-V1`）

销售订单转工单时，若用户勾选"创建变体"，`ProWorkorderServiceImpl.createWorkorderWithBom` 在一个事务内：

1. 新建 SKU 物料（`parent_id = 原 SPU`）
2. 复制 SPU 的工艺路线 + 路线 BOM，回填工单调整值
3. 把**工单**的 `product_id` 改为新 SKU

**但销售订单行的 `product_id` 不动，仍指向 SPU A。**

### 1.2 问题

由此产生产品身份在"销售→生产→出库"链路上的断裂：

```
销售订单行:  product_id = A (SPU)
                 │  转工单 + 建变体
                 ▼
工单:        product_id = A-V1 (SKU)  ← 已改
                 │  产品入库
                 ▼
库存:        成品库存记录在 item_id = A-V1，A 名下无库存
                 │  从销售订单建出库草稿
                 ▼
出库草稿:    selectStockWarehouseSummary(A) → 查不到 A 库存 → 红标"无库存"
             ❌ 实际有 A-V1 库存，但系统按 A 查不到
```

**本质**：销售订单是商务视角（客户订 A），生产是执行视角（实际做 A-V1）。出库作为桥接层，应当"生产什么出什么"。

### 1.3 设计原则（PM 决策）

| 视角 | 归属 | 原则 |
|------|------|------|
| 销售订单 | 商务契约 | 不动。客户订 A、报价按 A、对账按 A |
| 生产工单 | 执行 | 不动。实际生产 A-V1 |
| 出库 | 物理执行 | **桥接**。按工单反查实际产出物，出对应的库存 |

把桥接放在出库层，职责单一，不动 sal / pro 两域。

## 2. 方案选型

### 2.1 候选方案

| 方案 | 做法 | 评价 |
|------|------|------|
| A. 回写销售行 | 转工单建变体后把销售行 product_id 改成 SKU | 动已审核订单，失商务语义；改动跨 sal+pro |
| B. 按 parent_id 展开所有变体 | 出库建草稿时查 SPU + 其全部变体库存，FIFO 混合 | 查太宽：会把**别的客户订单**的变体也拉进候选，串仓风险 |
| **C. 工单反查精确制导（本方案）** | **从销售行反查其工单的 product_id，只查这个实际产出物的库存** | **精确：只出本订单实际生产的，不碰别人的变体；无串仓、无混合** |

### 2.2 选定：方案 C — 工单反查

**核心洞察**：工单是"为这个订单生产了什么"的权威来源。数据链路已经存在：

```
销售订单行 (line_id)
   │  qxx_pro_workorder.sales_order_line_id = line_id   (V71 已建)
   ▼
工单 (product_id = A-V1)   ← 建变体时已被改成 SKU
   │  product_id 就是实际产出的变体
   ▼
出库查这个 product_id 的库存 → 精确命中 A-V1
```

- **无串仓风险**：客户 X 的工单产出 A-V1，客户 Y 的工单产出 A-V2。X 的出库只查 A-V1，根本看不到 A-V2。"客户归属"已隐含在工单关联里，不需要新字段。
- **无 SPU+变体混合**：工单明确产出 A-V1，就只查 A-V1，不查 A。
- **改动面极小**：只动出库草稿生成那一步；过账扣库存完全不动（过账本就按出库行 item_id 扣减）。

### 2.3 候选产品解析规则

对每个销售订单行，按以下顺序解析"该查哪些 product_id 的库存"：

```
1. 查该行关联的工单（sales_order_line_id = lineId, status != CANCEL）
   → 取 DISTINCT product_id（工单实际产出物）
2. 若有工单 → candidateProductIds = 这些 product_id
3. 若无工单 → candidateProductIds = { line.productId }（回退到 SPU，覆盖未转工单/MTS 场景）
4. 按 candidateProductIds 查库存汇总，FIFO 拆行
```

**回退规则（步骤 3）的必要性**：销售订单已确认但尚未转工单时，仓库可能有标准品库存（期初/MTS 生产），应允许直接出。此时没有工单可反查，回退到销售行自身的 product_id（SPU A），保持与现状一致。

**关键特性**：一旦工单建了变体（product_id = A-V1），candidateProductIds = {A-V1}，**绝不查 A**。变体生产后只出变体库存，这是"生产什么出什么"的精确实现。

## 3. 详细设计

### 3.1 改动清单

| 层 | 文件 | 改动 |
|----|------|------|
| Mapper | `ProWorkorderMapper.java` / `.xml` | 新增 `selectProductIdsBySalesOrderLineId(Long)` 返回 `List<Long>` |
| 常量 | `ProConstants.java` | 补 `WORKORDER_STATUS_CANCEL = "CANCEL"`（当前硬编码，无常量） |
| VO | `WmStockWarehouseSummary.java` | 加 `itemId / itemCode / itemName` 字段 |
| SQL | `WmMaterialStockMapper.xml` `selectStockWarehouseSummary` | 改为接收 `List<Long> itemIds`（IN 子句），`GROUP BY (item_id, warehouse_id)`，补 item 列 |
| Mapper | `WmMaterialStockMapper.java` | `selectStockWarehouseSummary` 签名改 `List<Long> itemIds` |
| Service | `WmProductSalesServiceImpl` | 注入 `ProWorkorderMapper`；`mapOrderLinesToSalesLines` 加工单反查步骤；`buildSalesLine` 加 itemId 参数 |
| 前端 | 出库草稿页 | 可选：变体行显示 `[变体]` 标签（复用物料管理页样式） |
| DB | 无 | **不需要 Flyway 迁移**（纯查询逻辑改动） |

### 3.2 新增 Mapper：工单反查 product_id

`ProWorkorderMapper.java`：
```java
/** 按销售订单行反查工单实际产出的 product_id 集合（排除已取消） */
List<Long> selectProductIdsBySalesOrderLineId(Long salesOrderLineId);
```

`ProWorkorderMapper.xml`：
```xml
<!-- 按销售订单行反查工单产出产品（排除 CANCEL）。factory_id 由拦截器注入 -->
<select id="selectProductIdsBySalesOrderLineId" parameterType="Long" resultType="java.lang.Long">
    select distinct product_id from qxx_pro_workorder
    where sales_order_line_id = #{salesOrderLineId}
      and status != 'CANCEL'
      and product_id is not null
</select>
```

> 这与已有 `SalOrderLineMapper.sumProducedQtyByLineId`（按 lineId 聚合工单数量）是同表同条件的不同投影，factory_id 拦截器注入方式一致。

### 3.3 VO 改动：WmStockWarehouseSummary

当前只有仓库维度，缺产品维度（因为原来只查单 itemId）。现改为支持多 itemId 候选，需返回每条库存归属的具体物料：

```java
public class WmStockWarehouseSummary {
    private Long itemId;          // 新增：库存归属物料（SPU 或变体 SKU）
    private String itemCode;      // 新增
    private String itemName;      // 新增
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private BigDecimal quantityAvailable;
}
```

### 3.4 SQL 改动：selectStockWarehouseSummary

当前（单 itemId，仅按仓库分组）：

```xml
<select id="selectStockWarehouseSummary" ...>
    select s.warehouse_id as warehouseId, ...
    from qxx_wm_material_stock s
    left join qxx_wm_warehouse w on w.warehouse_id = s.warehouse_id
    where s.item_id = #{itemId}
      and s.quantity_available > 0
    group by s.warehouse_id
    order by min(s.create_time) asc
</select>
```

改为（多 itemId，按物料×仓库分组）：

```xml
<select id="selectStockWarehouseSummary" ...>
    select s.item_id as itemId,
           max(i.item_code) as itemCode,
           max(i.item_name) as itemName,
           s.warehouse_id as warehouseId,
           max(s.warehouse_code) as warehouseCode,
           coalesce(max(s.warehouse_name), max(w.warehouse_name)) as warehouseName,
           sum(s.quantity_available) as quantityAvailable
    from qxx_wm_material_stock s
    left join qxx_wm_warehouse w on w.warehouse_id = s.warehouse_id
    left join qxx_md_item i on i.item_id = s.item_id
    where s.item_id in
      <foreach collection="itemIds" item="id" open="(" separator="," close=")">#{id}</foreach>
      and s.quantity_available > 0
    group by s.item_id, s.warehouse_id
    order by min(s.create_time) asc
</select>
```

Mapper 接口签名改为 `List<WmStockWarehouseSummary> selectStockWarehouseSummary(@Param("itemIds") List<Long> itemIds);`

> factory_id 由拦截器自动注入到 `qxx_wm_material_stock`。`qxx_md_item` JOIN 同样带 factory_id，拦截器注入需验证；若 JOIN 表未被注入则补显式 `<if>` factory_id 条件。

### 3.5 Service 改动：mapOrderLinesToSalesLines

注入依赖：
```java
@Autowired private ProWorkorderMapper proWorkorderMapper;
```

核心逻辑改为先反查工单产出物：

```java
private List<WmProductSalesLine> mapOrderLinesToSalesLines(List<SalOrderLine> orderLines) {
    List<WmProductSalesLine> result = new ArrayList<>();
    if (orderLines == null) return result;
    for (SalOrderLine ol : orderLines) {
        BigDecimal need = ol.getQuantity() != null ? ol.getQuantity() : BigDecimal.ZERO;

        // ★ 工单反查：解析该销售行实际产出的 product_id
        List<Long> candidateItemIds = resolveProducedItemIds(ol);

        List<WmStockWarehouseSummary> stocks =
                wmMaterialStockMapper.selectStockWarehouseSummary(candidateItemIds);
        if (stocks == null || stocks.isEmpty()) {
            result.add(buildSalesLine(ol, null, null, null, null, null, null, need, BigDecimal.ZERO));
            continue;
        }
        // FIFO 按可用量拆行（stocks 已含 itemId，多物料×多仓混合按 create_time 排序）
        BigDecimal remaining = need;
        for (WmStockWarehouseSummary s : stocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal take = remaining.min(s.getQuantityAvailable());
            result.add(buildSalesLine(ol, s.getItemId(), s.getItemCode(), s.getItemName(),
                    s.getWarehouseId(), s.getWarehouseCode(), s.getWarehouseName(), take, s.getQuantityAvailable()));
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            WmStockWarehouseSummary last = stocks.get(stocks.size() - 1);
            result.add(buildSalesLine(ol, last.getItemId(), last.getItemCode(), last.getItemName(),
                    last.getWarehouseId(), last.getWarehouseCode(), last.getWarehouseName(), remaining, last.getQuantityAvailable()));
        }
    }
    return result;
}

/** 解析销售行实际产出的 product_id：优先工单反查，无工单回退到行自身 SPU */
private List<Long> resolveProducedItemIds(SalOrderLine ol) {
    List<Long> workorderProductIds =
            proWorkorderMapper.selectProductIdsBySalesOrderLineId(ol.getLineId());
    if (workorderProductIds != null && !workorderProductIds.isEmpty()) {
        return workorderProductIds;   // 工单产出物（可能是变体 A-V1）
    }
    // 无工单：回退到销售行自身产品（SPU，覆盖未转工单/MTS 场景）
    List<Long> fallback = new ArrayList<>();
    if (ol.getProductId() != null) fallback.add(ol.getProductId());
    return fallback;
}
```

`buildSalesLine` 签名加 itemId/code/name（从库存汇总取，不再从销售行硬套 SPU）：
```java
private WmProductSalesLine buildSalesLine(SalOrderLine ol, Long itemId, String itemCode,
        String itemName, Long whId, String whCode, String whName, BigDecimal qty, BigDecimal avail)
```

### 3.6 过账与扣库存：确认无需改动

过账链路 `postOut` → `postOutSingleBatch`：

- `WmProductSalesDetail` 的 `itemId` 从出库行克隆（`cloneDetail`），草稿阶段出库行已是变体 itemId
- `loadStockForUpdate(itemId=A-V1, batchId, wh)` 精确扣减 A-V1 库存 ✓
- `selectAvailableStocksForFifo(itemId=A-V1, wh)` 未指定批次时 FIFO 扣 A-V1 ✓
- 写事务流水 `qxx_wm_transaction`（type=PRODUCT_SALES，item_id=A-V1）✓

**结论：过账路径零改动。**

### 3.7 审计与追溯

无需新增字段。现有链路天然完整：

```
qxx_sal_order_line (product_id = A)            ← 商务视角，不变
   │ sales_order_line_id
   ▼
qxx_wm_product_sales_line (item_id = A-V1)     ← 物理视角，出库行指变体
   │ sales_id
   ▼
qxx_wm_product_sales_detail (item_id = A-V1)   ← 扣减明细
   │
   ▼
qxx_wm_material_stock (item_id = A-V1, onhand -= qty)
qxx_wm_transaction (item_id = A-V1, PRODUCT_SALES)
```

"客户订单 A 实际发了什么"：按 `sales_order_line_id` 查出库行 → item_id 即实际发货 SKU。`parent_id` 可反查回 SPU 血缘。

## 4. 边界与约束

### 4.1 多客户变体串仓：已解决（无需额外字段）

方案 C 通过工单反查，candidateProductIds 只含本订单工单的产出物。客户 X 的工单产出 A-V1、客户 Y 的产出 A-V2，两者互不可见。**客户归属已隐含在 `sales_order_line_id → workorder.product_id` 链路中**，不需要给变体加 client_id 字段。

> 残留极端场景：同一变体 SKU（A-V1）被两个不同客户的工单各自生产（相同规格、不同订单）。此时 A-V1 库存是同质可互换的（同一物理产品），FIFO 出货不构成"串仓"——客户收到的是规格完全一致的产品。若未来需按工单隔离库存，可利用库存记录已有的 `workorder_id` 维度精确匹配，本期不做。

### 4.2 SPU 与变体共存：已解决

工单建了变体后 product_id = A-V1，candidateProductIds = {A-V1}，**只查 A-V1 不查 A**。不存在 SPU+变体混合排序问题。

仅当**无工单**（回退到 SPU）时查 A，此时也没有变体库存参与，无混合。

### 4.3 不在本期范围

| 项 | 原因 |
|----|------|
| 变体主 BOM（`qxx_md_product_bom`）回填工单调整值 | 独立问题，当前是原样复制父产品 |
| 按工单 workorder_id 精确隔离库存 | 同质变体互换不影响客户，YAGNI |
| 销售订单行回写变体 | 与本方案互斥，本方案刻意不动销售单 |
| 生产中途才建变体 | 当前变体只在工单创建时产生 |

### 4.4 行为不变的场景

| 场景 | 行为 |
|------|------|
| 工单未建变体（product_id=A） | 工单反查得 {A}，查 A 库存，与现状一致 |
| 工单选"否，沿用原物料编码" | 不建变体，product_id=A，同上 |
| 销售行未转工单（有标准品库存） | 无工单回退到 {A}，查 A 库存，与现状一致 |
| 手工创建出库单（非从销售订单生成） | 用户自选 item，不经过 mapOrderLinesToSalesLines，不受影响 |
| 一行多次部分转工单（同一变体） | 工单反查得 {A-V1}，多次部分出库正常 |

## 5. 前端改动（可选，最小）

出库草稿页（从销售订单生成后）：

- 出库行"物料编码/名称"列已显示实际 itemId（变体编码如 `FIN-BENQU-001-V1`），无需额外开发即可看到变体
- 可选增强：若出库行 item 是变体（`parent_id > 0`），显示 `[变体]` 标签（复用物料管理页已有样式）

## 6. 测试要点

| 测试 | 验证点 |
|------|--------|
| 单元：工单反查命中变体 | 销售行=A，工单 product_id=A-V1 → candidateProductIds={A-V1}，出库行 item_id=A-V1 |
| 单元：无工单回退 SPU | 销售行=A，无工单 → candidateProductIds={A}，查 A 库存 |
| 单元：多工单多变体 | 两张工单 product_id 分别 A-V1、A-V2 → candidates={A-V1,A-V2}，都进 FIFO |
| 单元：CANCEL 工单排除 | 一张 CANCEL 工单 product_id=A-V2 + 一张正常 A-V1 → 只 {A-V1} |
| 集成：过账扣减 | 草稿行 item_id=A-V1 → 过账扣 A-V1 库存、写 PRODUCT_SALES 事务 |
| 集成：可编辑兜底 | 草稿生成后手动改 item_id → 过账按改后的 item 扣 |
| SQL：factory_id 注入 | IN 子句 + md_item JOIN 是否被拦截器注入 factory_id |

## 7. 影响面总结

- **DB**：无 Flyway 迁移
- **后端 pro 域**：1 个 Mapper 方法新增（只读查询）
- **后端 wm 域**：1 个 VO 加字段 + 1 个 SQL 改签名 + 1 个 Service 方法重构（约 50 行）
- **后端 sal 域**：零改动
- **前端**：可选 1 处标签增强
- **风险**：低。改动集中在只读的草稿生成路径，不触碰过账扣库存的事务路径

## 8. 参考文件

- 出库草稿生成：`backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/impl/WmProductSalesServiceImpl.java:528-599`
- 库存汇总 SQL：`backend/ruoyi-system/src/main/resources/mapper/mes/wm/WmMaterialStockMapper.xml:290-302`
- 工单聚合（参考）：`backend/ruoyi-system/src/main/resources/mapper/mes/sal/SalOrderLineMapper.xml:144-148`
- 工单状态常量：`backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/ProConstants.java:33-36`
- 变体创建（product_id 改写点）：`backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProWorkorderServiceImpl.java:1635`
- 过账扣减：`WmProductSalesServiceImpl.java:164-350`
