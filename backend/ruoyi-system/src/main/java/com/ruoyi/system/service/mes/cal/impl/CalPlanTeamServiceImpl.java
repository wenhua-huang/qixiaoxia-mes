package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalPlanTeamMapper;
import com.ruoyi.system.domain.mes.cal.CalPlanTeam;
import com.ruoyi.system.service.mes.cal.ICalPlanTeamService;

/**
 * 计划班组关联Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalPlanTeamServiceImpl implements ICalPlanTeamService 
{
    @Autowired
    private CalPlanTeamMapper qxxCalPlanTeamMapper;

    /**
     * 查询计划班组关联
     * 
     * @param recordId 计划班组关联主键
     * @return 计划班组关联
     */
    @Override
    public CalPlanTeam selectCalPlanTeamByRecordId(Long recordId)
    {
        return qxxCalPlanTeamMapper.selectCalPlanTeamByRecordId(recordId);
    }

    /**
     * 查询计划班组关联列表
     * 
     * @param calPlanTeam 计划班组关联
     * @return 计划班组关联
     */
    @Override
    public List<CalPlanTeam> selectCalPlanTeamList(CalPlanTeam calPlanTeam)
    {
        return qxxCalPlanTeamMapper.selectCalPlanTeamList(calPlanTeam);
    }

    /**
     * 新增计划班组关联
     * 
     * @param calPlanTeam 计划班组关联
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalPlanTeam(CalPlanTeam calPlanTeam)
    {
        calPlanTeam.setCreateTime(DateUtils.getNowDate());
        calPlanTeam.setCreateBy(SecurityUtils.getUsername());
        return qxxCalPlanTeamMapper.insertCalPlanTeam(calPlanTeam);
    }

    /**
     * 修改计划班组关联
     * 
     * @param calPlanTeam 计划班组关联
     * @return 结果
     */
    @Override
    public int updateCalPlanTeam(CalPlanTeam calPlanTeam)
    {
        calPlanTeam.setUpdateTime(DateUtils.getNowDate());
        calPlanTeam.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalPlanTeamMapper.updateCalPlanTeam(calPlanTeam);
    }

    /**
     * 批量删除计划班组关联
     * 
     * @param recordIds 需要删除的计划班组关联主键
     * @return 结果
     */
    @Override
    public int deleteCalPlanTeamByRecordIds(Long[] recordIds)
    {
        return qxxCalPlanTeamMapper.deleteCalPlanTeamByRecordIds(recordIds);
    }

    /**
     * 删除计划班组关联信息
     * 
     * @param recordId 计划班组关联主键
     * @return 结果
     */
    @Override
    public int deleteCalPlanTeamByRecordId(Long recordId)
    {
        return qxxCalPlanTeamMapper.deleteCalPlanTeamByRecordId(recordId);
    }
}
