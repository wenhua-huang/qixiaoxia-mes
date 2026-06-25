package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackParamMapper;
import com.ruoyi.system.domain.mes.pro.ProFeedbackParam;
import com.ruoyi.system.service.mes.pro.IProFeedbackParamService;

/**
 * 报工实际参数值Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProFeedbackParamServiceImpl implements IProFeedbackParamService
{
    @Autowired
    private ProFeedbackParamMapper qxxProFeedbackParamMapper;

    /**
     * 查询报工实际参数值
     *
     * @param recordId 报工实际参数值主键
     * @return 报工实际参数值
     */
    @Override
    public ProFeedbackParam selectProFeedbackParamByRecordId(Long recordId)
    {
        return qxxProFeedbackParamMapper.selectProFeedbackParamByRecordId(recordId);
    }

    /**
     * 查询报工实际参数值列表
     *
     * @param proFeedbackParam 报工实际参数值
     * @return 报工实际参数值
     */
    @Override
    public List<ProFeedbackParam> selectProFeedbackParamList(ProFeedbackParam proFeedbackParam)
    {
        return qxxProFeedbackParamMapper.selectProFeedbackParamList(proFeedbackParam);
    }

    /**
     * 根据报工ID查询参数列表
     *
     * @param feedbackId 报工ID
     * @return 报工实际参数值集合
     */
    @Override
    public List<ProFeedbackParam> selectProFeedbackParamByFeedbackId(Long feedbackId)
    {
        return qxxProFeedbackParamMapper.selectProFeedbackParamByFeedbackId(feedbackId);
    }

    /**
     * 新增报工实际参数值
     *
     * @param proFeedbackParam 报工实际参数值
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProFeedbackParam(ProFeedbackParam proFeedbackParam)
    {
        proFeedbackParam.setCreateTime(DateUtils.getNowDate());
        proFeedbackParam.setCreateBy(SecurityUtils.getUsername());
        return qxxProFeedbackParamMapper.insertProFeedbackParam(proFeedbackParam);
    }

    /**
     * 修改报工实际参数值
     *
     * @param proFeedbackParam 报工实际参数值
     * @return 结果
     */
    @Override
    public int updateProFeedbackParam(ProFeedbackParam proFeedbackParam)
    {
        proFeedbackParam.setUpdateTime(DateUtils.getNowDate());
        proFeedbackParam.setUpdateBy(SecurityUtils.getUsername());
        return qxxProFeedbackParamMapper.updateProFeedbackParam(proFeedbackParam);
    }

    /**
     * 批量删除报工实际参数值
     *
     * @param recordIds 需要删除的报工实际参数值主键
     * @return 结果
     */
    @Override
    public int deleteProFeedbackParamByRecordIds(Long[] recordIds)
    {
        return qxxProFeedbackParamMapper.deleteProFeedbackParamByRecordIds(recordIds);
    }

    /**
     * 删除报工实际参数值信息
     *
     * @param recordId 报工实际参数值主键
     * @return 结果
     */
    @Override
    public int deleteProFeedbackParamByRecordId(Long recordId)
    {
        return qxxProFeedbackParamMapper.deleteProFeedbackParamByRecordId(recordId);
    }

    /**
     * 根据报工ID删除参数
     *
     * @param feedbackId 报工ID
     * @return 结果
     */
    @Override
    public int deleteProFeedbackParamByFeedbackId(Long feedbackId)
    {
        return qxxProFeedbackParamMapper.deleteProFeedbackParamByFeedbackId(feedbackId);
    }
}
