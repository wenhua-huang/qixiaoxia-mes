package com.ruoyi.web.controller.mes.tm;

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
import com.ruoyi.system.domain.mes.tm.TmToolType;
import com.ruoyi.system.service.mes.tm.ITmToolTypeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工装夹具类型Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/tm/tooltype")
public class TmToolTypeController extends BaseController
{
    @Autowired
    private ITmToolTypeService tmToolTypeService;

    /**
     * 查询工装夹具类型列表
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tooltype:list')")
    @GetMapping("/list")
    public TableDataInfo list(TmToolType tmToolType)
    {
        startPage();
        List<TmToolType> list = tmToolTypeService.selectTmToolTypeList(tmToolType);
        return getDataTable(list);
    }

    /**
     * 导出工装夹具类型列表
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tooltype:export')")
    @Log(title = "工装夹具类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TmToolType tmToolType)
    {
        List<TmToolType> list = tmToolTypeService.selectTmToolTypeList(tmToolType);
        ExcelUtil<TmToolType> util = new ExcelUtil<TmToolType>(TmToolType.class);
        util.exportExcel(response, list, "工装夹具类型数据");
    }

    /**
     * 获取工装夹具类型详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tooltype:query')")
    @GetMapping(value = "/{toolTypeId}")
    public AjaxResult getInfo(@PathVariable("toolTypeId") Long toolTypeId)
    {
        return success(tmToolTypeService.selectTmToolTypeByToolTypeId(toolTypeId));
    }

    /**
     * 新增工装夹具类型
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tooltype:add')")
    @Log(title = "工装夹具类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TmToolType tmToolType)
    {
        tmToolTypeService.insertTmToolType(tmToolType);
        return success(tmToolType);
    }

    /**
     * 修改工装夹具类型
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tooltype:edit')")
    @Log(title = "工装夹具类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TmToolType tmToolType)
    {
        return toAjax(tmToolTypeService.updateTmToolType(tmToolType));
    }

    /**
     * 删除工装夹具类型
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tooltype:remove')")
    @Log(title = "工装夹具类型", businessType = BusinessType.DELETE)
	@DeleteMapping("/{toolTypeIds}")
    public AjaxResult remove(@PathVariable Long[] toolTypeIds)
    {
        return toAjax(tmToolTypeService.deleteTmToolTypeByToolTypeIds(toolTypeIds));
    }
}
