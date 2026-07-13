package com.ruoyi.system.domain.mes.pro;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单单据生成日志对象 qxx_pro_doc_generation_log
 * 幂等保障：记录从工单自动生成的各类单据
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public class ProDocGenerationLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long factoryId;
    private Long workorderId;
    private String docType;
    private Long docId;
    private String docCode;
    /** 触发本次生成的报工 record_id（RECPT 类型必填, 其它类型为 null 表示工单级幂等） */
    private Long sourceFeedbackId;
    private String generationBatch;
    private String status;

    public Long getLogId() { return logId; }
    public void setLogId(Long v) { this.logId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getDocType() { return docType; }
    public void setDocType(String v) { this.docType = v; }

    public Long getDocId() { return docId; }
    public void setDocId(Long v) { this.docId = v; }

    public String getDocCode() { return docCode; }
    public void setDocCode(String v) { this.docCode = v; }

    public Long getSourceFeedbackId() { return sourceFeedbackId; }
    public void setSourceFeedbackId(Long v) { this.sourceFeedbackId = v; }

    public String getGenerationBatch() { return generationBatch; }
    public void setGenerationBatch(String v) { this.generationBatch = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
