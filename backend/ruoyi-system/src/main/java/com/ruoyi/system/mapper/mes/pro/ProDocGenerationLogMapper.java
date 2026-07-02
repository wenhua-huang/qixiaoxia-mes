package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProDocGenerationLog;

/**
 * 工单单据生成日志Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
public interface ProDocGenerationLogMapper
{
    ProDocGenerationLog selectById(Long logId);

    List<ProDocGenerationLog> selectList(ProDocGenerationLog log);

    int insert(ProDocGenerationLog log);

    int deleteByWorkorderIdAndDocId(Long workorderId, String docType, Long docId);

    int deleteByWorkorderId(Long workorderId);
}
