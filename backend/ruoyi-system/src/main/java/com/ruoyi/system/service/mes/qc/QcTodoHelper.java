package com.ruoyi.system.service.mes.qc;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.enums.TodoTypeEnum;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.sys.SysTodoList;
import com.ruoyi.system.mapper.mes.sys.SysTodoListMapper;

/**
 * 质检待办联动：检验单生成 → 建 QC_CHECK 待办；判定完成 → 关闭对应待办。
 *
 * <p>供 5 个生成工厂方法与 4 个类型判定 Service 共用，避免重复拼装。
 * factory_id 由 FactoryIdInterceptor 自动注入（sys_todo_list 在拦截表白名单内）。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
@Component
public class QcTodoHelper
{
    @Autowired
    private SysTodoListMapper sysTodoListMapper;

    /**
     * 生成检验单后建待办（userId 留空 = 全员可见；sourceDocType=检验类型 IQC/IPQC/OQC/RQC）。
     *
     * @param qcType    检验业务类型（同时作为待办 source_doc_type）
     * @param qcId      检验单ID
     * @param qcCode    检验单编码
     * @param itemName  物料名称（拼标题用）
     */
    public void createTodo(String qcType, Long qcId, String qcCode, String itemName)
    {
        SysTodoList todo = new SysTodoList();
        todo.setUserId(SecurityUtils.getUserId());
        todo.setTodoType(TodoTypeEnum.QC_CHECK.getCode());
        todo.setTodoTitle(buildTitle(qcType, qcCode, itemName));
        todo.setSourceDocId(qcId);
        todo.setSourceDocType(qcType);
        todo.setSourceDocCode(qcCode);
        todo.setPriority(QcConstants.TODO_PRIORITY_NORMAL);
        todo.setStatus(QcConstants.TODO_STATUS_PENDING);
        todo.setCreateTime(new Date());
        sysTodoListMapper.insertSysTodoList(todo);
    }

    /**
     * 判定完成后按 source 关闭待处理待办（幂等：无待办不报错）。
     *
     * @param qcType     检验业务类型
     * @param qcId       检验单ID
     * @param checkResult 判定结果 PASS/FAIL/CONCESSION
     */
    public void completeTodo(String qcType, Long qcId, String checkResult)
    {
        List<SysTodoList> todos = sysTodoListMapper.selectPendingBySource(qcType, qcId);
        if (todos == null || todos.isEmpty())
        {
            return;
        }
        Date now = new Date();
        String handleResult = toHandleResult(checkResult);
        for (SysTodoList todo : todos)
        {
            SysTodoList update = new SysTodoList();
            update.setTodoId(todo.getTodoId());
            update.setStatus(QcConstants.TODO_STATUS_COMPLETED);
            update.setHandleTime(now);
            update.setHandleResult(handleResult);
            sysTodoListMapper.updateSysTodoList(update);
        }
    }

    private String buildTitle(String qcType, String qcCode, String itemName)
    {
        StringBuilder sb = new StringBuilder("待检验：").append(qcCode);
        if (StringUtils.isNotEmpty(itemName))
        {
            sb.append(' ').append(itemName);
        }
        return sb.toString();
    }

    private String toHandleResult(String checkResult)
    {
        if (QcConstants.RESULT_PASS.equals(checkResult))
        {
            return QcConstants.TODO_RESULT_PASS;
        }
        if (QcConstants.RESULT_CONCESSION.equals(checkResult))
        {
            return QcConstants.TODO_RESULT_CONCESSION;
        }
        return QcConstants.TODO_RESULT_FAIL;
    }
}
