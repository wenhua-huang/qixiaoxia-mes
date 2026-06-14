package com.ruoyi.system.mapper.mes.cal;

import java.util.List;
import com.ruoyi.system.domain.mes.cal.CalTeamshift;

/**
 * 班组排班明细Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface CalTeamshiftMapper 
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
     * 删除班组排班明细
     * 
     * @param teamshiftId 班组排班明细主键
     * @return 结果
     */
    public int deleteCalTeamshiftByTeamshiftId(Long teamshiftId);

    /**
     * 批量删除班组排班明细
     * 
     * @param teamshiftIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCalTeamshiftByTeamshiftIds(Long[] teamshiftIds);
}
