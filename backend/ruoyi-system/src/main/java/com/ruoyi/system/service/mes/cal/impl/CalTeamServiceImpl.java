package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalTeamMapper;
import com.ruoyi.system.domain.mes.cal.CalTeam;
import com.ruoyi.system.service.mes.cal.ICalTeamService;

/**
 * 班组Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalTeamServiceImpl implements ICalTeamService 
{
    @Autowired
    private CalTeamMapper qxxCalTeamMapper;

    /**
     * 查询班组
     * 
     * @param teamId 班组主键
     * @return 班组
     */
    @Override
    public CalTeam selectCalTeamByTeamId(Long teamId)
    {
        return qxxCalTeamMapper.selectCalTeamByTeamId(teamId);
    }

    /**
     * 查询班组列表
     * 
     * @param calTeam 班组
     * @return 班组
     */
    @Override
    public List<CalTeam> selectCalTeamList(CalTeam calTeam)
    {
        return qxxCalTeamMapper.selectCalTeamList(calTeam);
    }

    /**
     * 新增班组
     * 
     * @param calTeam 班组
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalTeam(CalTeam calTeam)
    {
        calTeam.setCreateTime(DateUtils.getNowDate());
        calTeam.setCreateBy(SecurityUtils.getUsername());
        return qxxCalTeamMapper.insertCalTeam(calTeam);
    }

    /**
     * 修改班组
     * 
     * @param calTeam 班组
     * @return 结果
     */
    @Override
    public int updateCalTeam(CalTeam calTeam)
    {
        calTeam.setUpdateTime(DateUtils.getNowDate());
        calTeam.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalTeamMapper.updateCalTeam(calTeam);
    }

    /**
     * 批量删除班组
     * 
     * @param teamIds 需要删除的班组主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamByTeamIds(Long[] teamIds)
    {
        return qxxCalTeamMapper.deleteCalTeamByTeamIds(teamIds);
    }

    /**
     * 删除班组信息
     * 
     * @param teamId 班组主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamByTeamId(Long teamId)
    {
        return qxxCalTeamMapper.deleteCalTeamByTeamId(teamId);
    }
}
