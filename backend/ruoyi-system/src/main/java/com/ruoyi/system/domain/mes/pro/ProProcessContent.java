package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工序作业内容对象 qxx_pro_process_content
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public class ProProcessContent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 内容ID */
    private Long contentId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 工序ID(关联qxx_pro_process) */
    @Excel(name = "工序ID")
    private Long processId;

    /** 顺序编号(作业步骤序号) */
    @Excel(name = "顺序编号")
    private Integer orderNum;

    /** 作业内容说明 */
    @Excel(name = "作业内容说明")
    private String contentText;

    /** 辅助设备/工具 */
    @Excel(name = "辅助设备")
    private String device;

    /** 辅助材料/辅料 */
    @Excel(name = "辅助材料")
    private String material;

    /** 作业指导书URL(SOP文件) */
    @Excel(name = "作业指导书URL")
    private String docUrl;

    /** 是否需要质检(Y-是,N-否) */
    @Excel(name = "是否需要质检")
    private String isCheck;

    public void setContentId(Long contentId)
    {
        this.contentId = contentId;
    }

    public Long getContentId()
    {
        return contentId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setProcessId(Long processId)
    {
        this.processId = processId;
    }

    public Long getProcessId()
    {
        return processId;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setContentText(String contentText)
    {
        this.contentText = contentText;
    }

    public String getContentText()
    {
        return contentText;
    }

    public void setDevice(String device)
    {
        this.device = device;
    }

    public String getDevice()
    {
        return device;
    }

    public void setMaterial(String material)
    {
        this.material = material;
    }

    public String getMaterial()
    {
        return material;
    }

    public void setDocUrl(String docUrl)
    {
        this.docUrl = docUrl;
    }

    public String getDocUrl()
    {
        return docUrl;
    }

    public void setIsCheck(String isCheck)
    {
        this.isCheck = isCheck;
    }

    public String getIsCheck()
    {
        return isCheck;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("contentId", getContentId())
            .append("factoryId", getFactoryId())
            .append("processId", getProcessId())
            .append("orderNum", getOrderNum())
            .append("contentText", getContentText())
            .append("device", getDevice())
            .append("material", getMaterial())
            .append("docUrl", getDocUrl())
            .append("isCheck", getIsCheck())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
