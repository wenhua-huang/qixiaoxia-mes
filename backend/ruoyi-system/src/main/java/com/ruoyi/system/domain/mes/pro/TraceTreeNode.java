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
 * <p>防环/汇聚：
 * <ul>
 *   <li>cycle=true：真环（当前 DFS 路径回到祖先，数据异常），标红，不再展开</li>
 *   <li>reference=true：菱形汇聚（节点已在其他分支展开过，同一物料的不同业务视角），
 *       显示为灰色引用卡，不展开 children，refToBranch 指向首次出现的业务单据分支</li>
 * </ul>
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

    /** 是否循环节点（真环：当前路径回到祖先，数据异常） */
    private boolean cycle;

    /** 是否同源引用节点（菱形汇聚：已在其他分支展开，此处仅作引用，不递归） */
    private boolean reference;

    /** 首次出现所在分支的业务单据描述，如「流转卡 000CRD20260811011」 */
    private String refToBranch;

    /** 被其他分支引用的次数（仅首次出现节点有值，用于角标提示） */
    private Integer refCount;

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
    public boolean isReference() { return reference; }
    public void setReference(boolean reference) { this.reference = reference; }
    public String getRefToBranch() { return refToBranch; }
    public void setRefToBranch(String refToBranch) { this.refToBranch = refToBranch; }
    public Integer getRefCount() { return refCount; }
    public void setRefCount(Integer refCount) { this.refCount = refCount; }
    public List<TraceTreeNode> getChildren() { return children; }
    public void setChildren(List<TraceTreeNode> children) { this.children = children; }
}
