package com.ruoyi.system.service.mes.cal;

import java.util.List;
import com.ruoyi.system.domain.mes.cal.CalTeamshift;

/**
 * 班组排班明细Service接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface ICalTeamshiftService 
{
    /**
     * 查询班组排班明细
     * 
     * @param teamshiftId 班组排班明细主键
     * @return 班组排班明细
     */
    public CalTeamshift selectCalTeamshiftByTeamshiftId(Long teamshiftId);

    /**
     * 查询班组排班明细列表
     * 
     * @param calTeamshift 班组排班明细
     * @return 班组排班明细集合
     */
    public List<CalTeamshift> selectCalTeamshiftList(CalTeamshift calTeamshift);

    /**
     * 新增班组排班明细
     * 
     * @param calTeamshift 班组排班明细
     * @return 结果
     */
    public int insertCalTeamshift(CalTeamshift calTeamshift);

    /**
     * 修改班组排班明细
     * 
     * @param calTeamshift 班组排班明细
     * @return 结果
     */
    public int updateCalTeamshift(CalTeamshift calTeamshift);

    /**
     * 批量删除班组排班明细
     * 
     * @param teamshiftIds 需要删除的班组排班明细主键集合
     * @return 结果
     */
    public int deleteCalTeamshiftByTeamshiftIds(Long[] teamshiftIds);

    /**
     * 删除班组排班明细信息
     *
     * @param teamshiftId 班组排班明细主键
     * @return 结果
     */
    public int deleteCalTeamshiftByTeamshiftId(Long teamshiftId);

    /**
     * 根据排班计划生成排班明细记录
     * 当计划状态变为"已确认"时调用，按日期和班次为每个班组生成每日排班
     *
     * @param planId 排班计划ID
     */
    public void genRecords(Long planId);
}
