package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.md.MdEmployeeSkill;
import com.ruoyi.system.service.mes.md.IMdEmployeeSkillService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 员工技能Controller
 * 
 * @author qixiaoxia
 * @date 2026-07-15
 */
@RestController
@RequestMapping("/mes/md/employee-skill")
public class MdEmployeeSkillController extends BaseController
{
    @Autowired
    private IMdEmployeeSkillService mdEmployeeSkillService;

    /**
     * 查询员工技能列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/list")
    public TableDataInfo list(MdEmployeeSkill mdEmployeeSkill)
    {
        List<MdEmployeeSkill> list = mdEmployeeSkillService.selectMdEmployeeSkillList(mdEmployeeSkill);
        return getDataTable(list);
    }

    /**
     * 获取员工技能详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = "/{skillId}")
    public AjaxResult getInfo(@PathVariable("skillId") Long skillId)
    {
        return success(mdEmployeeSkillService.selectMdEmployeeSkillBySkillId(skillId));
    }

    /**
     * 新增员工技能
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "员工技能", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdEmployeeSkill mdEmployeeSkill)
    {
        mdEmployeeSkillService.insertMdEmployeeSkill(mdEmployeeSkill);
        return success(mdEmployeeSkill);
    }

    /**
     * 修改员工技能
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "员工技能", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdEmployeeSkill mdEmployeeSkill)
    {
        return toAjax(mdEmployeeSkillService.updateMdEmployeeSkill(mdEmployeeSkill));
    }

    /**
     * 删除员工技能
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "员工技能", businessType = BusinessType.DELETE)
	@DeleteMapping("/{skillIds}")
    public AjaxResult remove(@PathVariable Long[] skillIds)
    {
        return toAjax(mdEmployeeSkillService.deleteMdEmployeeSkillBySkillIds(skillIds));
    }
}
