package com.ruoyi.system.service.mes.qc;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

/**
 * 检验单编码生成工具 — 工厂（自动建单）与 IQcIqcService（手工建单）共用。
 *
 * 优先 AutoCodeGenerator(QC_IQC_CODE) 规则编码；规则缺失/停用/生成失败时
 * 用 IQC+时间戳+4位随机兜底，保证不阻断收货/建单业务；DB 唯一约束是最终防线。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public final class QcCodeGenerator
{
    private static final int CODE_RANDOM_BOUND = 10000;

    private static final String IQC_CODE_PREFIX = "IQC";

    private static final String OQC_CODE_PREFIX = "OQC";

    private QcCodeGenerator()
    {
    }

    /**
     * 生成 IQC 检验单编码
     *
     * @param autoCodeGenerator 自动编码生成器（可 null：未配置时直接走兜底）
     * @return 规则编码或 IQC+yyyyMMddHHmmssSSS+4位随机 兜底编码
     */
    public static String genIqcCode(AutoCodeGenerator autoCodeGenerator)
    {
        return genCode(autoCodeGenerator, QcConstants.CODE_RULE_IQC, IQC_CODE_PREFIX);
    }

    /**
     * 生成 OQC 检验单编码
     *
     * @param autoCodeGenerator 自动编码生成器（可 null：未配置时直接走兜底）
     * @return 规则编码或 OQC+yyyyMMddHHmmssSSS+4位随机 兜底编码
     */
    public static String genOqcCode(AutoCodeGenerator autoCodeGenerator)
    {
        return genCode(autoCodeGenerator, QcConstants.CODE_RULE_OQC, OQC_CODE_PREFIX);
    }

    /** 规则编码优先，失败/未配置时 前缀+时间戳+4位随机兜底，保证不阻断业务建单 */
    private static String genCode(AutoCodeGenerator autoCodeGenerator, String ruleCode, String fallbackPrefix)
    {
        if (autoCodeGenerator != null)
        {
            try
            {
                String code = autoCodeGenerator.genSerialCode(ruleCode, null);
                if (code != null && !code.isEmpty())
                {
                    return code;
                }
            }
            catch (Exception ignored)
            {
                // fall through 到时间戳兜底（编码规则缺失/停用不打断业务创建）
            }
        }
        String ts = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int rand = (int) (Math.random() * CODE_RANDOM_BOUND);
        return fallbackPrefix + ts + String.format("%04d", rand);
    }
}
