package com.ruoyi.system.domain.mes.sys;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 通用待办事项对象 sys_todo_list
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public class SysTodoList extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 待办ID */
    private Long todoId;

    /** 工厂ID */
    private Long factoryId;

    /** 待办人用户ID(关联sys_user) */
    private Long userId;

    /** 待办标题 */
    @Excel(name = "待办标题")
    private String todoTitle;

    /** 待办类型:APPROVAL-审批,QC_CHECK-质检,DV_CHECK-点检,MAINTEN-保养,REPAIR-维修,OTHER-其他 */
    @Excel(name = "待办类型", readConverterExp = "APPROVAL=审批,QC_CHECK=质检,DV_CHECK=点检,MAINTEN=保养,REPAIR=维修,OTHER=其他")
    private String todoType;

    /** 关联业务单据ID */
    private Long sourceDocId;

    /** 关联业务单据类型 */
    private String sourceDocType;

    /** 关联业务单据编码 */
    @Excel(name = "单据编码")
    private String sourceDocCode;

    /** 优先级:URGENT-紧急,HIGH-高,NORMAL-普通,LOW-低 */
    @Excel(name = "优先级", readConverterExp = "URGENT=紧急,HIGH=高,NORMAL=普通,LOW=低")
    private String priority;

    /** 状态:PENDING-待处理,PROCESSING-处理中,COMPLETED-已完成,REJECTED-已驳回 */
    @Excel(name = "状态", readConverterExp = "PENDING=待处理,PROCESSING=处理中,COMPLETED=已完成,REJECTED=已驳回")
    private String status;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;

    /** 处理结果/意见 */
    private String handleResult;

    public Long getTodoId() { return todoId; }
    public void setTodoId(Long todoId) { this.todoId = todoId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTodoTitle() { return todoTitle; }
    public void setTodoTitle(String todoTitle) { this.todoTitle = todoTitle; }

    public String getTodoType() { return todoType; }
    public void setTodoType(String todoType) { this.todoType = todoType; }

    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long sourceDocId) { this.sourceDocId = sourceDocId; }

    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String sourceDocType) { this.sourceDocType = sourceDocType; }

    public String getSourceDocCode() { return sourceDocCode; }
    public void setSourceDocCode(String sourceDocCode) { this.sourceDocCode = sourceDocCode; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Date getHandleTime() { return handleTime; }
    public void setHandleTime(Date handleTime) { this.handleTime = handleTime; }

    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("todoId", getTodoId())
            .append("factoryId", getFactoryId())
            .append("userId", getUserId())
            .append("todoTitle", getTodoTitle())
            .append("todoType", getTodoType())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocType", getSourceDocType())
            .append("sourceDocCode", getSourceDocCode())
            .append("priority", getPriority())
            .append("status", getStatus())
            .append("deadline", getDeadline())
            .append("handleTime", getHandleTime())
            .append("handleResult", getHandleResult())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
