package com.ruoyi.system.mapper.mes.pro;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import org.apache.ibatis.annotations.Param;

public interface ProFeedbackMapper {
    ProFeedback selectProFeedbackByRecordId(Long id);
    /** 行级锁查询 — 审核报工时防并发重复累加 */
    ProFeedback selectProFeedbackByRecordIdForUpdate(Long id);
    List<ProFeedback> selectProFeedbackList(ProFeedback fb);
    ProFeedback selectProFeedbackByFeedbackCode(String code);
    int insertProFeedback(ProFeedback fb);
    int updateProFeedback(ProFeedback fb);
    int deleteProFeedbackByRecordId(Long id);
    int deleteProFeedbackByRecordIds(Long[] ids);
    /** 统计流转卡在某工序已审核报工的合格品总量（用于判断卡是否完工） */
    BigDecimal sumAuditedQualifiedByCardAndProcess(@Param("cardId") Long cardId, @Param("processId") Long processId);

    /**
     * 统计工单某工序已审核(AUDITED)报工的报工数量合计（上机数量默认值：上工序产出）。
     * 一期按工单+工序粒度聚合，不区分流转卡（与质检门控/放行口径一致）。
     * factory_id 由 FactoryIdInterceptor 自动注入 SQL 文本，参数无需传 factoryId。
     */
    BigDecimal sumAuditedQuantityFeedback(@Param("workorderId") Long workorderId,
                                          @Param("processId") Long processId);

    /**
     * 统计工单某工序全部报工的本次上机数量合计（上机数量默认值：本工序已投入）。
     * 一期按工单+工序粒度聚合，不区分流转卡。
     * factory_id 由 FactoryIdInterceptor 自动注入 SQL 文本，参数无需传 factoryId。
     */
    BigDecimal sumQuantityInput(@Param("workorderId") Long workorderId,
                                @Param("processId") Long processId);

    /** 批量查询有 PREPARE（待审核）报工的 taskId 列表 */
    List<Long> selectPendingTaskIds(@Param("taskIds") Collection<Long> taskIds);

    /** 查卡下所有报工的工序ID（轻量查询，仅 process_id 列，用于顺序校验） */
    List<Long> selectProcessIdsByCardId(@Param("cardId") Long cardId);
}
