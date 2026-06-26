package com.ruoyi.web.controller.mes.sys;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeRuleService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 编码生成规则Controller
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@RestController
@RequestMapping("/mes/sys/autocoderule")
public class SysAutoCodeRuleController extends BaseController
{
    @Autowired
    private ISysAutoCodeRuleService sysAutoCodeRuleService;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAutoCodeRule sysAutoCodeRule)
    {
        startPage();
        List<SysAutoCodeRule> list = sysAutoCodeRuleService.selectSysAutoCodeRuleList(sysAutoCodeRule);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:export')")
    @Log(title = "编码生成规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAutoCodeRule sysAutoCodeRule)
    {
        List<SysAutoCodeRule> list = sysAutoCodeRuleService.selectSysAutoCodeRuleList(sysAutoCodeRule);
        ExcelUtil<SysAutoCodeRule> util = new ExcelUtil<SysAutoCodeRule>(SysAutoCodeRule.class);
        util.exportExcel(response, list, "编码生成规则数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId)
    {
        return AjaxResult.success(sysAutoCodeRuleService.selectSysAutoCodeRuleByRuleId(ruleId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:add')")
    @Log(title = "编码生成规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysAutoCodeRule sysAutoCodeRule)
    {
        if (!sysAutoCodeRuleService.checkRuleCodeUnique(sysAutoCodeRule))
        {
            return AjaxResult.error("规则编码已存在！");
        }
        return toAjax(sysAutoCodeRuleService.insertSysAutoCodeRule(sysAutoCodeRule));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:edit')")
    @Log(title = "编码生成规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAutoCodeRule sysAutoCodeRule)
    {
        if (!sysAutoCodeRuleService.checkRuleCodeUnique(sysAutoCodeRule))
        {
            return AjaxResult.error("规则编码已存在！");
        }
        return toAjax(sysAutoCodeRuleService.updateSysAutoCodeRule(sysAutoCodeRule));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:remove')")
    @Log(title = "编码生成规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds)
    {
        return toAjax(sysAutoCodeRuleService.deleteSysAutoCodeRuleByRuleIds(ruleIds));
    }

    /**
     * 校验规则编码唯一性
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:query')")
    @GetMapping("/checkRuleCodeUnique")
    public AjaxResult checkRuleCodeUnique(SysAutoCodeRule sysAutoCodeRule)
    {
        return AjaxResult.success(sysAutoCodeRuleService.checkRuleCodeUnique(sysAutoCodeRule));
    }

    /**
     * 生成流水编码 (POST — 非幂等操作)
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:autocoderule:query')")
    @PostMapping("/genSerialCode/{ruleCode}")
    public AjaxResult genSerialCode(@PathVariable("ruleCode") String ruleCode,
                                     @RequestParam(required = false) String inputCharacter)
    {
        try
        {
            String code = autoCodeGenerator.genSerialCode(ruleCode, inputCharacter);
            return AjaxResult.success("编码生成成功", code);
        }
        catch (Exception e)
        {
            return AjaxResult.error("编码生成失败：" + e.getMessage());
        }
    }
}
