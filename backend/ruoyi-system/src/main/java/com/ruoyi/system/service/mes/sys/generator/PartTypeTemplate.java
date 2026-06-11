package com.ruoyi.system.service.mes.sys.generator;

import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;

/**
 * 编码分段处理模板接口 — 策略模式
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface PartTypeTemplate
{
    /**
     * 处理编码分段，返回该段生成的字符串
     *
     * @param part 分段配置（会被修改 seriaNowNo 等字段）
     * @return 生成的字符串片段
     */
    String partHandle(SysAutoCodePart part);

    /**
     * 返回该处理器对应的分段类型
     */
    String getPartType();
}
