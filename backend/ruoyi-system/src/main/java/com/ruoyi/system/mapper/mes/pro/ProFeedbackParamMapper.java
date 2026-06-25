package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProFeedbackParam;

public interface ProFeedbackParamMapper {
    ProFeedbackParam selectProFeedbackParamByRecordId(Long id);
    List<ProFeedbackParam> selectProFeedbackParamList(ProFeedbackParam p);
    List<ProFeedbackParam> selectProFeedbackParamByFeedbackId(Long feedbackId);
    int insertProFeedbackParam(ProFeedbackParam p);
    int updateProFeedbackParam(ProFeedbackParam p);
    int deleteProFeedbackParamByRecordId(Long id);
    int deleteProFeedbackParamByRecordIds(Long[] ids);
    int deleteProFeedbackParamByFeedbackId(Long feedbackId);
}
