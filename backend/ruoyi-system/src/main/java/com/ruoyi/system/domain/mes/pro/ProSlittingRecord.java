package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;

/**
 * 分切作业记录表对象 qxx_pro_slitting_record
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
public class ProSlittingRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long slitId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "分切批次号")
    private String slitBatchNo;

    /** 关联报工记录ID */
    private Long feedbackId;

    @Excel(name = "工单编码")
    private Long workorderId;
    private String workorderCode;

    private Long processId;
    private String processCode;
    @Excel(name = "工序名称")
    private String processName;

    private Long cardId;

    // ── 母卷信息 ──
    @Excel(name = "母卷号")
    private Long parentRollId;
    private String parentRollCode;

    // ── 领料来源（库存驱动）──
    private Long sourceItemId;
    private String sourceItemCode;
    @Excel(name = "领料物料")
    private String sourceItemName;
    private Long sourceWarehouseId;
    private String sourceWarehouseCode;
    private String sourceWarehouseName;
    @Excel(name = "领料数量(吨)")
    private BigDecimal pickQty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pickTime;
    private String pickBy;

    private Long parentItemId;
    private String parentItemCode;
    @Excel(name = "母卷物料")
    private String parentItemName;
    private String parentWidth;
    @Excel(name = "母卷重量(吨)")
    private BigDecimal parentWeight;

    // ── 子卷汇总 ──
    @Excel(name = "子卷数量")
    private Integer childCount;
    @Excel(name = "子卷总重量(吨)")
    private BigDecimal childTotalWeight;

    // ── 纸边/损耗 ──
    private Long edgeItemId;
    private String edgeItemCode;
    private String edgeItemName;
    @Excel(name = "纸边重量(kg)")
    private BigDecimal edgeWeight;

    // ── 重量校验 ──
    private BigDecimal lossWeight;
    private BigDecimal lossRate;

    // ── 操作 ──
    @Excel(name = "操作人")
    private String operator;
    private Long workstationId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date slitTime;
    @Excel(name = "状态")
    private String status;

    /** 分切模式:INTERNAL-厂内,OUTSOURCE-外协 */
    @Excel(name = "分切模式")
    private String slitMode;
    /** 外协厂商ID（OUTSOURCE 模式必填） */
    private Long vendorId;
    private String vendorCode;
    @Excel(name = "外协厂商")
    private String vendorName;

    /** 母卷列表（外协多卷发料时填充，详情查询用；不入库） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<WmRollDetail> parentRolls;

    /** 子卷明细列表（详情查询时填充，不入库） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<WmRollDetail> childRolls;

    public Long getSlitId() { return slitId; }
    public void setSlitId(Long v) { this.slitId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getSlitBatchNo() { return slitBatchNo; }
    public void setSlitBatchNo(String v) { this.slitBatchNo = v; }

    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }

    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }

    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }

    public Long getParentRollId() { return parentRollId; }
    public void setParentRollId(Long v) { this.parentRollId = v; }

    public String getParentRollCode() { return parentRollCode; }
    public void setParentRollCode(String v) { this.parentRollCode = v; }

    public Long getSourceItemId() { return sourceItemId; }
    public void setSourceItemId(Long v) { this.sourceItemId = v; }
    public String getSourceItemCode() { return sourceItemCode; }
    public void setSourceItemCode(String v) { this.sourceItemCode = v; }
    public String getSourceItemName() { return sourceItemName; }
    public void setSourceItemName(String v) { this.sourceItemName = v; }
    public Long getSourceWarehouseId() { return sourceWarehouseId; }
    public void setSourceWarehouseId(Long v) { this.sourceWarehouseId = v; }
    public String getSourceWarehouseCode() { return sourceWarehouseCode; }
    public void setSourceWarehouseCode(String v) { this.sourceWarehouseCode = v; }
    public String getSourceWarehouseName() { return sourceWarehouseName; }
    public void setSourceWarehouseName(String v) { this.sourceWarehouseName = v; }
    public BigDecimal getPickQty() { return pickQty; }
    public void setPickQty(BigDecimal v) { this.pickQty = v; }
    public Date getPickTime() { return pickTime; }
    public void setPickTime(Date v) { this.pickTime = v; }
    public String getPickBy() { return pickBy; }
    public void setPickBy(String v) { this.pickBy = v; }

    public Long getParentItemId() { return parentItemId; }
    public void setParentItemId(Long v) { this.parentItemId = v; }

    public String getParentItemCode() { return parentItemCode; }
    public void setParentItemCode(String v) { this.parentItemCode = v; }

    public String getParentItemName() { return parentItemName; }
    public void setParentItemName(String v) { this.parentItemName = v; }

    public String getParentWidth() { return parentWidth; }
    public void setParentWidth(String v) { this.parentWidth = v; }

    public BigDecimal getParentWeight() { return parentWeight; }
    public void setParentWeight(BigDecimal v) { this.parentWeight = v; }

    public Integer getChildCount() { return childCount; }
    public void setChildCount(Integer v) { this.childCount = v; }

    public BigDecimal getChildTotalWeight() { return childTotalWeight; }
    public void setChildTotalWeight(BigDecimal v) { this.childTotalWeight = v; }

    public Long getEdgeItemId() { return edgeItemId; }
    public void setEdgeItemId(Long v) { this.edgeItemId = v; }

    public String getEdgeItemCode() { return edgeItemCode; }
    public void setEdgeItemCode(String v) { this.edgeItemCode = v; }

    public String getEdgeItemName() { return edgeItemName; }
    public void setEdgeItemName(String v) { this.edgeItemName = v; }

    public BigDecimal getEdgeWeight() { return edgeWeight; }
    public void setEdgeWeight(BigDecimal v) { this.edgeWeight = v; }

    public BigDecimal getLossWeight() { return lossWeight; }
    public void setLossWeight(BigDecimal v) { this.lossWeight = v; }

    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal v) { this.lossRate = v; }

    public String getOperator() { return operator; }
    public void setOperator(String v) { this.operator = v; }

    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }

    public Date getSlitTime() { return slitTime; }
    public void setSlitTime(Date v) { this.slitTime = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getSlitMode() { return slitMode; }
    public void setSlitMode(String v) { this.slitMode = v; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }

    public List<WmRollDetail> getParentRolls() { return parentRolls; }
    public void setParentRolls(List<WmRollDetail> v) { this.parentRolls = v; }

    public List<WmRollDetail> getChildRolls() { return childRolls; }
    public void setChildRolls(List<WmRollDetail> v) { this.childRolls = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("slitId", getSlitId())
            .append("slitBatchNo", getSlitBatchNo())
            .append("parentRollCode", getParentRollCode())
            .append("childCount", getChildCount())
            .append("status", getStatus())
            .toString();
    }
}
