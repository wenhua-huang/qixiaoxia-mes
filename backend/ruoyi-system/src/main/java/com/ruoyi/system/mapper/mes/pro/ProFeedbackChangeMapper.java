package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProFeedbackChange;

/**
 * 报工字段变更痕迹Mapper接口
 *
 * @author qixiaoxia
 */
public interface ProFeedbackChangeMapper
{
    int insertProFeedbackChange(ProFeedbackChange change);

    List<ProFeedbackChange> selectByFeedback(@Param("feedbackId") Long feedbackId);

    List<ProFeedbackChange> selectByTask(@Param("taskId") Long taskId);
}
