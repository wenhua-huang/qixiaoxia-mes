package com.ruoyi.system.domain.mes.sys;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 通用附件对象 sys_attachment
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public class SysAttachment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 附件ID */
    private Long attachmentId;

    /** 工厂ID */
    private Long factoryId;

    /** 关联的业务单据ID */
    private Long sourceDocId;

    /** 业务单据类型 */
    private String sourceDocType;

    /** 文件访问URL(相对路径) */
    private String fileUrl;

    /** 存储域名/Base路径 */
    private String basePath;

    /** 存储文件名(UUID) */
    private String fileName;

    /** 原始文件名(含扩展名) */
    @Excel(name = "原始文件名")
    private String orignalName;

    /** 文件类型(png/jpg/pdf/docx/xlsx等) */
    private String fileType;

    /** 文件大小(KB) */
    @Excel(name = "文件大小(KB)")
    private Double fileSize;

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long sourceDocId) { this.sourceDocId = sourceDocId; }

    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String sourceDocType) { this.sourceDocType = sourceDocType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOrignalName() { return orignalName; }
    public void setOrignalName(String orignalName) { this.orignalName = orignalName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Double getFileSize() { return fileSize; }
    public void setFileSize(Double fileSize) { this.fileSize = fileSize; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("attachmentId", getAttachmentId())
            .append("factoryId", getFactoryId())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocType", getSourceDocType())
            .append("fileUrl", getFileUrl())
            .append("basePath", getBasePath())
            .append("fileName", getFileName())
            .append("orignalName", getOrignalName())
            .append("fileType", getFileType())
            .append("fileSize", getFileSize())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
