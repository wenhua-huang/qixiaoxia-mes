package com.ruoyi.system.service.mes.sys.generator;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分段处理器调度器 — 按 PartTypeEnum.order 顺序注入所有处理器，类型到处理器的映射
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Component
public class PartTypeHandlerDispatcher
{
    @Autowired
    private List<PartTypeTemplate> handlers;

    /**
     * 根据分段类型找到对应处理器并执行
     */
    public String choiceExecute(SysAutoCodePart part)
    {
        String partType = part.getPartType();
        for (PartTypeTemplate handler : handlers)
        {
            if (handler.getPartType().equals(partType))
            {
                return handler.partHandle(part);
            }
        }
        throw new RuntimeException("未知的分段类型: " + partType);
    }

    /**
     * 根据分段类型获取处理器
     */
    public PartTypeTemplate getHandler(String partType)
    {
        for (PartTypeTemplate handler : handlers)
        {
            if (handler.getPartType().equals(partType))
            {
                return handler;
            }
        }
        throw new RuntimeException("未知的分段类型: " + partType);
    }
}
