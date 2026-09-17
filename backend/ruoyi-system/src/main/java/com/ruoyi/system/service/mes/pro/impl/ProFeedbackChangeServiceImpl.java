package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProFeedbackChange;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackChangeMapper;
import com.ruoyi.system.service.mes.pro.IProFeedbackChangeService;

/**
 * 报工字段变更痕迹Service实现
 *
 * @author qixiaoxia
 */
@Service
public class ProFeedbackChangeServiceImpl implements IProFeedbackChangeService
{
    private static final Logger log = LoggerFactory.getLogger(ProFeedbackChangeServiceImpl.class);

    @Autowired
    private ProFeedbackChangeMapper proFeedbackChangeMapper;

    @Override
    public void recordInputChange(Long feedbackId, ProFeedback fb, BigDecimal oldVal, BigDecimal newVal,
                                  String source, String reason)
    {
        if (feedbackId == null || !isDifferent(oldVal, newVal)) {
            return;
        }
        try {
            ProFeedbackChange change = new ProFeedbackChange();
            change.setFeedbackId(feedbackId);
            if (fb != null) {
                change.setTaskId(fb.getTaskId());
                change.setWorkorderId(fb.getWorkorderId());
            }
            change.setFieldName(ProConstants.CHANGE_FIELD_QUANTITY_INPUT);
            change.setOldValue(oldVal != null ? oldVal.toPlainString() : null);
            change.setNewValue(newVal != null ? newVal.toPlainString() : "");
            change.setChangeSource(source != null ? source : ProConstants.CHANGE_SOURCE_MANUAL);
            change.setChangeReason(reason);
            change.setCreateBy(SecurityUtils.getUsername());
            change.setCreateTime(DateUtils.getNowDate());
            proFeedbackChangeMapper.insertProFeedbackChange(change);
        } catch (Exception e) {
            // 留痕失败不阻断报工主流程
            log.error("报工上机数量留痕失败 feedbackId={}, {} -> {}", feedbackId, oldVal, newVal, e);
        }
    }

    @Override
    public List<ProFeedbackChange> selectByFeedback(Long feedbackId)
    {
        return proFeedbackChangeMapper.selectByFeedback(feedbackId);
    }

    @Override
    public List<ProFeedbackChange> selectByTask(Long taskId)
    {
        return proFeedbackChangeMapper.selectByTask(taskId);
    }

    /** BigDecimal 数值语义不等（1.0 与 1.00 视为相等）；纯 null→null 不算差异 */
    private boolean isDifferent(BigDecimal oldVal, BigDecimal newVal)
    {
        if (oldVal == null && newVal == null) {
            return false;
        }
        if (oldVal == null || newVal == null) {
            return true;
        }
        return oldVal.compareTo(newVal) != 0;
    }
}
