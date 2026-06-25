package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProFeedbackConsume;

/**
 * 报工物料消耗Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-23
 */
public interface ProFeedbackConsumeMapper
{
    /**
     * 批量新增物料消耗
     */
    int insertBatch(List<ProFeedbackConsume> list);

    /**
     * 根据报工ID删除物料消耗
     */
    int deleteByFeedbackId(Long feedbackId);

    /**
     * 根据报工ID查询物料消耗列表
     */
    List<ProFeedbackConsume> selectByFeedbackId(Long feedbackId);
}
