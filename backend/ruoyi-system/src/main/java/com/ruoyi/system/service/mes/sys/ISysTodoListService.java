package com.ruoyi.system.service.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysTodoList;

/**
 * 通用待办事项Service接口
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface ISysTodoListService
{
    public SysTodoList selectSysTodoListByTodoId(Long todoId);
    public List<SysTodoList> selectSysTodoListList(SysTodoList sysTodoList);
    public int insertSysTodoList(SysTodoList sysTodoList);
    public int updateSysTodoList(SysTodoList sysTodoList);
    public java.util.List<java.util.Map<String, Object>> countByStatus(Long userId);
    public int deleteSysTodoListByTodoIds(Long[] todoIds);
    public int deleteSysTodoListByTodoId(Long todoId);
}
