package com.ruoyi.system.domain.mes.qc;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 质检不合格拦截-放行记录对象 qxx_qc_block_release
 *
 * <p>一张 FAIL 的 IPQC 对每个被拦截任务至多一条（uk_ipqc_target 兜底并发重复放行）。
 *
 * @author qixiaoxia
 */
public class QcBlockRelease extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long releaseId;

    /** 工厂ID */
    private Long factoryId;

    /** 被放行的IPQC检验单ID */
    private Long ipqcId;

    /** 检验单号 */
    private String ipqcCode;

    /** 工单ID */
    private Long workorderId;

    /** 工单编码 */
    private String workorderCode;

    /** 流转卡ID */
    private Long cardId;

    /** 流转卡编码 */
    private String cardCode;

    /** 检验工序ID */
    private Long checkProcessId;

    /** 检验工序名 */
    private String checkProcessName;

    /** 被拦截(放行)任务ID */
    private Long targetTaskId;

    /** 被拦截工序ID */
    private Long targetProcessId;

    /** 被拦截工序名 */
    private String targetProcessName;

    /** 放行理由 */
    private String releaseReason;

    /** 放行人用户ID */
    private Long approverId;

    /** 放行人姓名(账号)快照 */
    private String approverName;

    /** 放行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    public Long getReleaseId() { return releaseId; }
    public void setReleaseId(Long releaseId) { this.releaseId = releaseId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getIpqcId() { return ipqcId; }
    public void setIpqcId(Long ipqcId) { this.ipqcId = ipqcId; }

    public String getIpqcCode() { return ipqcCode; }
    public void setIpqcCode(String ipqcCode) { this.ipqcCode = ipqcCode; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long workorderId) { this.workorderId = workorderId; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String workorderCode) { this.workorderCode = workorderCode; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getCardCode() { return cardCode; }
    public void setCardCode(String cardCode) { this.cardCode = cardCode; }

    public Long getCheckProcessId() { return checkProcessId; }
    public void setCheckProcessId(Long checkProcessId) { this.checkProcessId = checkProcessId; }

    public String getCheckProcessName() { return checkProcessName; }
    public void setCheckProcessName(String checkProcessName) { this.checkProcessName = checkProcessName; }

    public Long getTargetTaskId() { return targetTaskId; }
    public void setTargetTaskId(Long targetTaskId) { this.targetTaskId = targetTaskId; }

    public Long getTargetProcessId() { return targetProcessId; }
    public void setTargetProcessId(Long targetProcessId) { this.targetProcessId = targetProcessId; }

    public String getTargetProcessName() { return targetProcessName; }
    public void setTargetProcessName(String targetProcessName) { this.targetProcessName = targetProcessName; }

    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String releaseReason) { this.releaseReason = releaseReason; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }
}
