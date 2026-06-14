package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalHolidayMapper;
import com.ruoyi.system.domain.mes.cal.CalHoliday;
import com.ruoyi.system.service.mes.cal.ICalHolidayService;

/**
 * 节假日设置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalHolidayServiceImpl implements ICalHolidayService 
{
    @Autowired
    private CalHolidayMapper qxxCalHolidayMapper;

    /**
     * 查询节假日设置
     * 
     * @param holidayId 节假日设置主键
     * @return 节假日设置
     */
    @Override
    public CalHoliday selectCalHolidayByHolidayId(Long holidayId)
    {
        return qxxCalHolidayMapper.selectCalHolidayByHolidayId(holidayId);
    }

    /**
     * 查询节假日设置列表
     * 
     * @param calHoliday 节假日设置
     * @return 节假日设置
     */
    @Override
    public List<CalHoliday> selectCalHolidayList(CalHoliday calHoliday)
    {
        return qxxCalHolidayMapper.selectCalHolidayList(calHoliday);
    }

    /**
     * 新增节假日设置
     * 
     * @param calHoliday 节假日设置
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalHoliday(CalHoliday calHoliday)
    {
        calHoliday.setCreateTime(DateUtils.getNowDate());
        calHoliday.setCreateBy(SecurityUtils.getUsername());
        return qxxCalHolidayMapper.insertCalHoliday(calHoliday);
    }

    /**
     * 修改节假日设置
     * 
     * @param calHoliday 节假日设置
     * @return 结果
     */
    @Override
    public int updateCalHoliday(CalHoliday calHoliday)
    {
        calHoliday.setUpdateTime(DateUtils.getNowDate());
        calHoliday.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalHolidayMapper.updateCalHoliday(calHoliday);
    }

    /**
     * 批量删除节假日设置
     * 
     * @param holidayIds 需要删除的节假日设置主键
     * @return 结果
     */
    @Override
    public int deleteCalHolidayByHolidayIds(Long[] holidayIds)
    {
        return qxxCalHolidayMapper.deleteCalHolidayByHolidayIds(holidayIds);
    }

    /**
     * 删除节假日设置信息
     * 
     * @param holidayId 节假日设置主键
     * @return 结果
     */
    @Override
    public int deleteCalHolidayByHolidayId(Long holidayId)
    {
        return qxxCalHolidayMapper.deleteCalHolidayByHolidayId(holidayId);
    }
}
