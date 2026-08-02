package com.ruoyi.system.mapper.mes.pro;

import java.math.BigDecimal;
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
}
