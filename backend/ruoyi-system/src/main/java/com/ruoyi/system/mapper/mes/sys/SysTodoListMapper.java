package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.sys.SysTodoList;

/**
 * 通用待办事项Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysTodoListMapper
{
    public SysTodoList selectSysTodoListByTodoId(Long todoId);
    public List<SysTodoList> selectSysTodoListList(SysTodoList sysTodoList);
    public int insertSysTodoList(SysTodoList sysTodoList);
    public int updateSysTodoList(SysTodoList sysTodoList);
    public List<Map<String, Object>> countByStatus(@Param("userId") Long userId);
    public int deleteSysTodoListByTodoId(Long todoId);
    public int deleteSysTodoListByTodoIds(Long[] todoIds);
}
