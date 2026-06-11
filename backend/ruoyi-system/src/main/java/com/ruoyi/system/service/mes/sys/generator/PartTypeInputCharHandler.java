package com.ruoyi.system.service.mes.sys.generator;

import com.ruoyi.common.enums.PartTypeEnum;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * INPUTCHAR 处理器 — 返回传入的输入字符
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Order(0)
@Component
public class PartTypeInputCharHandler implements PartTypeTemplate
{
    @Override
    public String partHandle(SysAutoCodePart part)
    {
        String inputChar = part.getInputCharacter();
        if (inputChar == null || inputChar.isEmpty())
        {
            throw new RuntimeException("分段[" + part.getPartName() + "]为INPUTCHAR类型，但未设置输入字符！");
        }
        int partLength = part.getPartLength() != null ? part.getPartLength() : inputChar.length();
        if (inputChar.length() > partLength)
        {
            throw new RuntimeException("分段[" + part.getPartName() + "]输入字符长度(" + inputChar.length()
                    + ")超过分段长度(" + partLength + ")！");
        }
        // 按分段长度补齐（左补空格，或按规则设置）
        StringBuilder sb = new StringBuilder();
        while (sb.length() + inputChar.length() < partLength)
        {
            sb.append(" ");
        }
        sb.append(inputChar);
        return sb.toString();
    }

    @Override
    public String getPartType()
    {
        return PartTypeEnum.INPUTCHAR.getCode();
    }
}
