package com.ruoyi.system.domain.mes.pro;

import java.util.List;

/**
 * 工单BOM/参数偏离检测结果
 *
 * @author qixiaoxia
 */
public class ProWorkorderDeviationVO
{
    /** 是否有偏离 */
    private boolean hasDeviation;
    /** 偏离明细列表 */
    private List<DeviationItem> deviations;

    public boolean isHasDeviation() { return hasDeviation; }
    public void setHasDeviation(boolean v) { this.hasDeviation = v; }
    public List<DeviationItem> getDeviations() { return deviations; }
    public void setDeviations(List<DeviationItem> v) { this.deviations = v; }

    /**
     * 单条偏离明细
     */
    public static class DeviationItem
    {
        /** 来源：BOM / 参数 */
        private String source;
        /** 物料编码或参数名 */
        private String itemCode;
        /** 物料名或工序名 */
        private String itemName;
        /** 单位 */
        private String unitName;
        /** 标准值 */
        private String standardVal;
        /** 实际值 */
        private String actualVal;
        /** 差异标签：物料新增/用量变更/物料已删/参数调整 */
        private String diffLabel;

        public String getSource() { return source; }
        public void setSource(String v) { this.source = v; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String v) { this.itemCode = v; }
        public String getItemName() { return itemName; }
        public void setItemName(String v) { this.itemName = v; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String v) { this.unitName = v; }
        public String getStandardVal() { return standardVal; }
        public void setStandardVal(String v) { this.standardVal = v; }
        public String getActualVal() { return actualVal; }
        public void setActualVal(String v) { this.actualVal = v; }
        public String getDiffLabel() { return diffLabel; }
        public void setDiffLabel(String v) { this.diffLabel = v; }
    }
}
