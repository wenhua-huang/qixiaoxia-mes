package com.ruoyi.system.domain.mes.wm.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 外协批量操作结果 VO（执行发料/收货等复用）。
 * 逐单独立事务执行，单条失败不影响其他单；失败明细按单号+原因回传前端。
 *
 * @author qixiaoxia
 */
public class OutsourceBatchResult
{
    /** 本次提交总数 */
    private int total;

    /** 成功数 */
    private int success;

    /** 失败数 */
    private int failed;

    /** 失败明细 */
    private List<FailItem> failures = new ArrayList<>();

    public static class FailItem
    {
        private Long orderId;
        private String orderCode;
        private String reason;

        public FailItem() {}

        public FailItem(Long orderId, String orderCode, String reason)
        {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.reason = reason;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccess() { return success; }
    public void setSuccess(int success) { this.success = success; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public List<FailItem> getFailures() { return failures; }
    public void setFailures(List<FailItem> failures) { this.failures = failures; }
}
