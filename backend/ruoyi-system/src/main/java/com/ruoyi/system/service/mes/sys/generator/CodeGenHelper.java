package com.ruoyi.system.service.mes.sys.generator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 编码生成辅助工具 — 共享的周期键计算逻辑
 *
 * @author qixiaoxia
 * @date 2025-06-12
 */
public class CodeGenHelper
{
    /**
     * 根据循环方式计算周期键（供查询和保存统一使用）
     *
     * @param cycleMethod    循环方式 (YEAR/MONTH/DAY/HOUR/MINUTE/OTHER)
     * @param inputCharacter 传入字符（OTHER 模式时使用）
     * @return 周期键字符串
     */
    public static String computeCycleKey(String cycleMethod, String inputCharacter)
    {
        if (cycleMethod == null || cycleMethod.isEmpty())
        {
            return defaultKey();
        }
        switch (cycleMethod)
        {
            case "YEAR":   return formatDate("yyyy");
            case "MONTH":  return formatDate("yyyyMM");
            case "DAY":    return formatDate("yyyyMMdd");
            case "HOUR":   return formatDate("yyyyMMddHH");
            case "MINUTE": return formatDate("yyyyMMddHHmm");
            case "OTHER":
                if (inputCharacter == null || inputCharacter.isEmpty())
                {
                    throw new RuntimeException("循环方式为OTHER，但未传入inputCharacter参数！");
                }
                return inputCharacter;
            default:       return defaultKey();
        }
    }

    private static String formatDate(String pattern)
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    private static String defaultKey()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
