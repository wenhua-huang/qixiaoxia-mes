package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdEmployeeSkill;

/**
 * 员工技能Mapper接口
 * 
 * @author qixiaoxia
 * @date 2026-07-15
 */
public interface MdEmployeeSkillMapper 
{
    /**
     * 查询员工技能
     * 
     * @param skillId 员工技能主键
     * @return 员工技能
     */
    public MdEmployeeSkill selectMdEmployeeSkillBySkillId(Long skillId);

    /**
     * 查询员工技能列表
     * 
     * @param mdEmployeeSkill 员工技能
     * @return 员工技能集合
     */
    public List<MdEmployeeSkill> selectMdEmployeeSkillList(MdEmployeeSkill mdEmployeeSkill);

    /**
     * 新增员工技能
     * 
     * @param mdEmployeeSkill 员工技能
     * @return 结果
     */
    public int insertMdEmployeeSkill(MdEmployeeSkill mdEmployeeSkill);

    /**
     * 修改员工技能
     * 
     * @param mdEmployeeSkill 员工技能
     * @return 结果
     */
    public int updateMdEmployeeSkill(MdEmployeeSkill mdEmployeeSkill);

    /**
     * 删除员工技能
     * 
     * @param skillId 员工技能主键
     * @return 结果
     */
    public int deleteMdEmployeeSkillBySkillId(Long skillId);

    /**
     * 批量删除员工技能
     * 
     * @param skillIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMdEmployeeSkillBySkillIds(Long[] skillIds);

    /**
     * 根据用户ID删除所有技能
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteMdEmployeeSkillByUserId(Long userId);
}
