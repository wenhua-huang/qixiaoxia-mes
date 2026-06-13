package com.ruoyi.web.controller.mes.wm;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.wm.IWmRollDetailService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 纸卷明细表Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
@RestController
@RequestMapping("/mes/wm/roll_detail")
public class WmRollDetailController extends BaseController
{
    @Autowired
    private IWmRollDetailService wmRollDetailService;

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRollDetail entity)
    {
        startPage();
        List<WmRollDetail> list = wmRollDetailService.selectWmRollDetailList(entity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<WmRollDetail> list = wmRollDetailService.selectWmRollDetailAll();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:export')")
    @Log(title = "纸卷明细表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRollDetail entity)
    {
        List<WmRollDetail> list = wmRollDetailService.selectWmRollDetailList(entity);
        ExcelUtil<WmRollDetail> util = new ExcelUtil<WmRollDetail>(WmRollDetail.class);
        util.exportExcel(response, list, "纸卷明细表数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:query')")
    @GetMapping(value = "/{rollId}")
    public AjaxResult getInfo(@PathVariable("rollId") Long rollId)
    {
        return AjaxResult.success(wmRollDetailService.selectWmRollDetailByRollId(rollId));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:add')")
    @Log(title = "纸卷明细表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRollDetail entity)
    {
        return toAjax(wmRollDetailService.insertWmRollDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:edit')")
    @Log(title = "纸卷明细表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmRollDetail entity)
    {
        return toAjax(wmRollDetailService.updateWmRollDetail(entity));
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:roll_detail:remove')")
    @Log(title = "纸卷明细表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{rollIds}")
    public AjaxResult remove(@PathVariable Long[] rollIds)
    {
        return toAjax(wmRollDetailService.deleteWmRollDetailByRollIds(rollIds));
    }
}