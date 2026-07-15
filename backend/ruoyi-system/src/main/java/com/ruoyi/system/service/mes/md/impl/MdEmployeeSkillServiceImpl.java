package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.md.MdEmployeeSkillMapper;
import com.ruoyi.system.domain.mes.md.MdEmployeeSkill;
import com.ruoyi.system.service.mes.md.IMdEmployeeSkillService;

/**
 * 员工技能Service业务层处理
 * 
 * @author qixiaoxia
 * @date 2026-07-15
 */
@Service
public class MdEmployeeSkillServiceImpl implements IMdEmployeeSkillService 
{
    @Autowired
    private MdEmployeeSkillMapper mdEmployeeSkillMapper;

    /**
     * 查询员工技能
     * 
     * @param skillId 员工技能主键
     * @return 员工技能
     */
    @Override
    public MdEmployeeSkill selectMdEmployeeSkillBySkillId(Long skillId)
    {
        return mdEmployeeSkillMapper.selectMdEmployeeSkillBySkillId(skillId);
    }

    /**
     * 查询员工技能列表
     * 
     * @param mdEmployeeSkill 员工技能
     * @return 员工技能
     */
    @Override
    public List<MdEmployeeSkill> selectMdEmployeeSkillList(MdEmployeeSkill mdEmployeeSkill)
    {
        return mdEmployeeSkillMapper.selectMdEmployeeSkillList(mdEmployeeSkill);
    }

    /**
     * 新增员工技能
     * 
     * @param mdEmployeeSkill 员工技能
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMdEmployeeSkill(MdEmployeeSkill mdEmployeeSkill)
    {
        mdEmployeeSkill.setCreateTime(DateUtils.getNowDate());
        mdEmployeeSkill.setCreateBy(SecurityUtils.getUsername());
        return mdEmployeeSkillMapper.insertMdEmployeeSkill(mdEmployeeSkill);
    }

    /**
     * 修改员工技能
     * 
     * @param mdEmployeeSkill 员工技能
     * @return 结果
     */
    @Override
    public int updateMdEmployeeSkill(MdEmployeeSkill mdEmployeeSkill)
    {
        mdEmployeeSkill.setUpdateTime(DateUtils.getNowDate());
        mdEmployeeSkill.setUpdateBy(SecurityUtils.getUsername());
        return mdEmployeeSkillMapper.updateMdEmployeeSkill(mdEmployeeSkill);
    }

    /**
     * 批量删除员工技能
     * 
     * @param skillIds 需要删除的员工技能主键
     * @return 结果
     */
    @Override
    public int deleteMdEmployeeSkillBySkillIds(Long[] skillIds)
    {
        return mdEmployeeSkillMapper.deleteMdEmployeeSkillBySkillIds(skillIds);
    }

    /**
     * 删除员工技能信息
     * 
     * @param skillId 员工技能主键
     * @return 结果
     */
    @Override
    public int deleteMdEmployeeSkillBySkillId(Long skillId)
    {
        return mdEmployeeSkillMapper.deleteMdEmployeeSkillBySkillId(skillId);
    }
}
