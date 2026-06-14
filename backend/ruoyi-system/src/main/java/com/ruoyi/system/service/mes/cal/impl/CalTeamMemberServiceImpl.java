package com.ruoyi.system.service.mes.cal.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.cal.CalTeamMemberMapper;
import com.ruoyi.system.domain.mes.cal.CalTeamMember;
import com.ruoyi.system.service.mes.cal.ICalTeamMemberService;

/**
 * 班组成员Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class CalTeamMemberServiceImpl implements ICalTeamMemberService 
{
    @Autowired
    private CalTeamMemberMapper qxxCalTeamMemberMapper;

    /**
     * 查询班组成员
     * 
     * @param memberId 班组成员主键
     * @return 班组成员
     */
    @Override
    public CalTeamMember selectCalTeamMemberByMemberId(Long memberId)
    {
        return qxxCalTeamMemberMapper.selectCalTeamMemberByMemberId(memberId);
    }

    /**
     * 查询班组成员列表
     * 
     * @param calTeamMember 班组成员
     * @return 班组成员
     */
    @Override
    public List<CalTeamMember> selectCalTeamMemberList(CalTeamMember calTeamMember)
    {
        return qxxCalTeamMemberMapper.selectCalTeamMemberList(calTeamMember);
    }

    /**
     * 新增班组成员
     * 
     * @param calTeamMember 班组成员
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCalTeamMember(CalTeamMember calTeamMember)
    {
        calTeamMember.setCreateTime(DateUtils.getNowDate());
        calTeamMember.setCreateBy(SecurityUtils.getUsername());
        return qxxCalTeamMemberMapper.insertCalTeamMember(calTeamMember);
    }

    /**
     * 修改班组成员
     * 
     * @param calTeamMember 班组成员
     * @return 结果
     */
    @Override
    public int updateCalTeamMember(CalTeamMember calTeamMember)
    {
        calTeamMember.setUpdateTime(DateUtils.getNowDate());
        calTeamMember.setUpdateBy(SecurityUtils.getUsername());
        return qxxCalTeamMemberMapper.updateCalTeamMember(calTeamMember);
    }

    /**
     * 批量删除班组成员
     * 
     * @param memberIds 需要删除的班组成员主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamMemberByMemberIds(Long[] memberIds)
    {
        return qxxCalTeamMemberMapper.deleteCalTeamMemberByMemberIds(memberIds);
    }

    /**
     * 删除班组成员信息
     * 
     * @param memberId 班组成员主键
     * @return 结果
     */
    @Override
    public int deleteCalTeamMemberByMemberId(Long memberId)
    {
        return qxxCalTeamMemberMapper.deleteCalTeamMemberByMemberId(memberId);
    }
}
