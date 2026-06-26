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
import com.ruoyi.system.domain.mes.tm.TmTool;
import com.ruoyi.system.service.mes.tm.ITmToolService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工装夹具清单Controller
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@RestController
@RequestMapping("/mes/tm/tool")
public class TmToolController extends BaseController
{
    @Autowired
    private ITmToolService tmToolService;

    /**
     * 查询工装夹具清单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tool:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        TmTool cond = new TmTool();
        cond.setEnableFlag("1");
        List<TmTool> list = tmToolService.selectTmToolList(cond);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:tm:tool:list')")
    @GetMapping("/list")
    public TableDataInfo list(TmTool tmTool)
    {
        startPage();
        List<TmTool> list = tmToolService.selectTmToolList(tmTool);
        return getDataTable(list);
    }

    /**
     * 导出工装夹具清单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tool:export')")
    @Log(title = "工装夹具清单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TmTool tmTool)
    {
        List<TmTool> list = tmToolService.selectTmToolList(tmTool);
        ExcelUtil<TmTool> util = new ExcelUtil<TmTool>(TmTool.class);
        util.exportExcel(response, list, "工装夹具清单数据");
    }

    /**
     * 获取工装夹具清单详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tool:query')")
    @GetMapping(value = "/{toolId}")
    public AjaxResult getInfo(@PathVariable("toolId") Long toolId)
    {
        return success(tmToolService.selectTmToolByToolId(toolId));
    }

    /**
     * 新增工装夹具清单
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tool:add')")
    @Log(title = "工装夹具清单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TmTool tmTool)
    {
        tmToolService.insertTmTool(tmTool);
        return success(tmTool);
    }

    /**
     * 修改工装夹具清单
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tool:edit')")
    @Log(title = "工装夹具清单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TmTool tmTool)
    {
        return toAjax(tmToolService.updateTmTool(tmTool));
    }

    /**
     * 删除工装夹具清单
     */
    @PreAuthorize("@ss.hasPermi('mes:tm:tool:remove')")
    @Log(title = "工装夹具清单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{toolIds}")
    public AjaxResult remove(@PathVariable Long[] toolIds)
    {
        return toAjax(tmToolService.deleteTmToolByToolIds(toolIds));
    }
}
