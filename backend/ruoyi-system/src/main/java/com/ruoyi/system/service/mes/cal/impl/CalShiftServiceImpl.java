package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalShiftMapper;
import com.ruoyi.system.domain.mes.cal.CalShift;
import com.ruoyi.system.service.mes.cal.ICalShiftService;

/**
 * 计划班次Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalShiftServiceImpl implements ICalShiftService 
{
    @Autowired
    private CalShiftMapper qxxCalShiftMapper;

    /**
     * 查询计划班次
     * 
     * @param shiftId 计划班次主键
     * @return 计划班次
     */
    @Override
    public CalShift selectCalShiftByShiftId(Long shiftId)
    {
        return qxxCalShiftMapper.selectCalShiftByShiftId(shiftId);
    }

    /**
     * 查询计划班次列表
     * 
     * @param calShift 计划班次
     * @return 计划班次
     */
    @Override
    public List<CalShift> selectCalShiftList(CalShift calShift)
    {
        return qxxCalShiftMapper.selectCalShiftList(calShift);
    }

    /**
     * 新增计划班次
     * 
     * @param calShift 计划班次
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalShift(CalShift calShift)
    {
        calShift.setCreateTime(DateUtils.getNowDate());
        calShift.setCreateBy(SecurityUtils.getUsername());
        return qxxCalShiftMapper.insertCalShift(calShift);
    }

    /**
     * 修改计划班次
     * 
     * @param calShift 计划班次
     * @return 结果
     */
    @Override
    public int updateCalShift(CalShift calShift)
    {
        calShift.setUpdateTime(DateUtils.getNowDate());
        calShift.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalShiftMapper.updateCalShift(calShift);
    }

    /**
     * 批量删除计划班次
     * 
     * @param shiftIds 需要删除的计划班次主键
     * @return 结果
     */
    @Override
    public int deleteCalShiftByShiftIds(Long[] shiftIds)
    {
        return qxxCalShiftMapper.deleteCalShiftByShiftIds(shiftIds);
    }

    /**
     * 删除计划班次信息
     * 
     * @param shiftId 计划班次主键
     * @return 结果
     */
    @Override
    public int deleteCalShiftByShiftId(Long shiftId)
    {
        return qxxCalShiftMapper.deleteCalShiftByShiftId(shiftId);
    }
}
