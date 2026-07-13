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

    /**
     * 精准查询 source_feedback_id IS NULL 的 RECPT log (手动补录场景专用)。
     * 避免 selectList 走 <if test="sourceFeedbackId != null"> 时忽略过滤 → 拉全量 log 再 Java 端过滤的 N+1 隐患
     * (Fix Finding #11)。
     */
    List<ProDocGenerationLog> selectManualReceiptLogs(
            @org.apache.ibatis.annotations.Param("workorderId") Long workorderId,
            @org.apache.ibatis.annotations.Param("docType") String docType);

    /**
     * 撤销同一 (workorder, docType, sourceFeedbackId) 下的旧 ACTIVE log。
     * 用于 CANCEL 入库单再生场景：旧 log 仍为 ACTIVE 会阻塞新 log 的唯一索引插入。
     */
    int revokeBySourceFeedbackId(@org.apache.ibatis.annotations.Param("workorderId") Long workorderId,
            @org.apache.ibatis.annotations.Param("docType") String docType,
            @org.apache.ibatis.annotations.Param("sourceFeedbackId") Long sourceFeedbackId);
}
