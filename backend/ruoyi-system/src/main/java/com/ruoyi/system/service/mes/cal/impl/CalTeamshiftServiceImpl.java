package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalTeamshiftMapper;
import com.ruoyi.system.domain.mes.cal.CalTeamshift;
import com.ruoyi.system.service.mes.cal.ICalTeamshiftService;

/**
 * 班组排班明细Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalTeamshiftServiceImpl implements ICalTeamshiftService 
{
    @Autowired
    private CalTeamshiftMapper qxxCalTeamshiftMapper;

    /**
     * 查询班组排班明细
     * 
     * @param teamshiftId 班组排班明细主键
     * @return 班组排班明细
     */
    @Override
    public CalTeamshift selectCalTeamshiftByTeamshiftId(Long teamshiftId)
    {
        return qxxCalTeamshiftMapper.selectCalTeamshiftByTeamshiftId(teamshiftId);
    }

    /**
     * 查询班组排班明细列表
     * 
     * @param calTeamshift 班组排班明细
     * @return 班组排班明细
     */
    @Override
    public List<CalTeamshift> selectCalTeamshiftList(CalTeamshift calTeamshift)
    {
        return qxxCalTeamshiftMapper.selectCalTeamshiftList(calTeamshift);
    }

    /**
     * 新增班组排班明细
     * 
     * @param calTeamshift 班组排班明细
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalTeamshift(CalTeamshift calTeamshift)
    {
        calTeamshift.setCreateTime(DateUtils.getNowDate());
        calTeamshift.setCreateBy(SecurityUtils.getUsername());
        return qxxCalTeamshiftMapper.insertCalTeamshift(calTeamshift);
    }

    /**
     * 修改班组排班明细
     * 
     * @param calTeamshift 班组排班明细
     * @return 结果
     */
    @Override
    public int updateCalTeamshift(CalTeamshift calTeamshift)
    {
        calTeamshift.setUpdateTime(DateUtils.getNowDate());
        calTeamshift.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalTeamshiftMapper.updateCalTeamshift(calTeamshift);
    }

    /**
     * 批量删除班组排班明细
     * 
     * @param teamshiftIds 需要删除的班组排班明细主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamshiftByTeamshiftIds(Long[] teamshiftIds)
    {
        return qxxCalTeamshiftMapper.deleteCalTeamshiftByTeamshiftIds(teamshiftIds);
    }

    /**
     * 删除班组排班明细信息
     * 
     * @param teamshiftId 班组排班明细主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamshiftByTeamshiftId(Long teamshiftId)
    {
        return qxxCalTeamshiftMapper.deleteCalTeamshiftByTeamshiftId(teamshiftId);
    }
}
