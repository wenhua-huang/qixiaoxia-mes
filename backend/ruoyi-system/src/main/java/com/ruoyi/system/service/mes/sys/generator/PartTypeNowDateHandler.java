package com.ruoyi.system.service.mes.sys.generator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.ruoyi.common.enums.PartTypeEnum;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * NOWDATE 处理器 — 返回格式化后的当前日期时间 (Java 17 DateTimeFormatter)
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Order(1)
@Component
public class PartTypeNowDateHandler implements PartTypeTemplate
{
    @Override
    public String partHandle(SysAutoCodePart part)
    {
        String dateFormat = part.getDateFormat();
        if (dateFormat == null || dateFormat.isEmpty())
        {
            dateFormat = "yyyyMMdd";
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        return now.format(formatter);
    }

    @Override
    public String getPartType()
    {
        return PartTypeEnum.NOWDATE.getCode();
    }
}
