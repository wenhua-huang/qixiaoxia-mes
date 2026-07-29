package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 物料追溯树节点（DTO）
 *
 * <p>用于 traceChain 接口返回完整追溯树结构（横向 DAG 展示）。
 * 每个节点代表一个业务实体（采购单/库存/流转卡/工单/报工/销售单等），
 * 通过 children 递归表达分支关系。
 *
 * <p>防环：循环节点（已被 visited 标记的重复访问）设置 cycle=true，不再继续展开 children。
 *
 * @author qixiaoxia
 * @date 2026-07-28
 */
public class TraceTreeNode
{
    /** 节点类型（PUR_ORDER/MATERIAL_STOCK/CARD/WORKORDER/FEEDBACK/VENDOR/SALES_OUT 等） */
    private String nodeType;

    /** 节点 ID */
    private Long nodeId;

    /** 业务描述（如"采购单 PO20260727099 · 德欣纸业"），根节点为 "类型 #id" 回退 */
    private String nodeDesc;

    /** 物料名称（若有） */
    private String itemName;

    /** 批次号（若有） */
    private String batchCode;

    /** 进入此节点的事件类型（ISSUE/RECEIPT/PRODUCE_STOCKIN...），根节点为 null */
    private String traceType;

    /** 进入此节点的数量 */
    private BigDecimal quantity;

    /** 单位名称 */
    private String unitName;

    /** 深度（根节点=0） */
    private Integer depth;

    /** 是否循环节点（重复访问，不再展开） */
    private boolean cycle;

    /** 子分支 */
    private List<TraceTreeNode> children = new ArrayList<>();

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
    public String getNodeDesc() { return nodeDesc; }
    public void setNodeDesc(String nodeDesc) { this.nodeDesc = nodeDesc; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public String getTraceType() { return traceType; }
    public void setTraceType(String traceType) { this.traceType = traceType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public Integer getDepth() { return depth; }
    public void setDepth(Integer depth) { this.depth = depth; }
    public boolean isCycle() { return cycle; }
    public void setCycle(boolean cycle) { this.cycle = cycle; }
    public List<TraceTreeNode> getChildren() { return children; }
    public void setChildren(List<TraceTreeNode> children) { this.children = children; }
}
