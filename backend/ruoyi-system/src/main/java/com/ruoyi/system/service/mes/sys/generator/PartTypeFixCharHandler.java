package com.ruoyi.system.service.mes.sys.generator;

import com.ruoyi.common.enums.PartTypeEnum;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * FIXCHAR 处理器 — 返回固定字符
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Order(2)
@Component
public class PartTypeFixCharHandler implements PartTypeTemplate
{
    @Override
    public String partHandle(SysAutoCodePart part)
    {
        String fixChar = part.getFixCharacter();
        if (fixChar == null || fixChar.isEmpty())
        {
            throw new RuntimeException("分段[" + part.getPartName() + "]为FIXCHAR类型，但未设置固定字符！");
        }
        return fixChar;
    }

    @Override
    public String getPartType()
    {
        return PartTypeEnum.FIXCHAR.getCode();
    }
}
