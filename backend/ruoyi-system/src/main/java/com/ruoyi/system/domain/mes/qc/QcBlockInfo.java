package com.ruoyi.system.domain.mes.qc;

/**
 * 质检不合格阻塞信息（非持久化 DTO）：findBlock 命中时描述拦截来源与给前端/报工的提示文案。
 *
 * @author qixiaoxia
 */
public class QcBlockInfo
{
    /** 被放行/拦截来源的 IPQC 检验单ID */
    private Long ipqcId;

    /** 检验单号 */
    private String ipqcCode;

    /** 检验工序ID（上道 is_check='Y' 节点） */
    private Long checkProcessId;

    /** 检验工序名 */
    private String checkProcessName;

    /** 阻塞原因文案（直接抛给用户） */
    private String reason;

    public QcBlockInfo()
    {
    }

    public QcBlockInfo(Long ipqcId, String ipqcCode, Long checkProcessId,
                       String checkProcessName, String reason)
    {
        this.ipqcId = ipqcId;
        this.ipqcCode = ipqcCode;
        this.checkProcessId = checkProcessId;
        this.checkProcessName = checkProcessName;
        this.reason = reason;
    }

    public Long getIpqcId() { return ipqcId; }
    public void setIpqcId(Long ipqcId) { this.ipqcId = ipqcId; }

    public String getIpqcCode() { return ipqcCode; }
    public void setIpqcCode(String ipqcCode) { this.ipqcCode = ipqcCode; }

    public Long getCheckProcessId() { return checkProcessId; }
    public void setCheckProcessId(Long checkProcessId) { this.checkProcessId = checkProcessId; }

    public String getCheckProcessName() { return checkProcessName; }
    public void setCheckProcessName(String checkProcessName) { this.checkProcessName = checkProcessName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
