package com.ruoyi.web.controller.mes.pro;

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
import com.ruoyi.system.domain.mes.pro.ProCardProcess;
import com.ruoyi.system.service.mes.pro.IProCardProcessService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 流转卡工序Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/cardprocess")
public class ProCardProcessController extends BaseController
{
    @Autowired
    private IProCardProcessService proCardProcessService;

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProCardProcess e) { startPage(); return getDataTable(proCardProcessService.selectProCardProcessList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(proCardProcessService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:export')")
    @Log(title = "流转卡工序", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProCardProcess e) {
        List<ProCardProcess> list = proCardProcessService.selectProCardProcessList(e);
        ExcelUtil<ProCardProcess> util = new ExcelUtil<ProCardProcess>(ProCardProcess.class);
        util.exportExcel(response, list, "流转卡工序数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:query')")
    @GetMapping("/listByCardId/{cardId}")
    public AjaxResult listByCardId(@PathVariable Long cardId)
    {
        ProCardProcess query = new ProCardProcess();
        query.setCardId(cardId);
        return success(proCardProcessService.selectProCardProcessList(query));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId) { return success(proCardProcessService.selectProCardProcessByRecordId(recordId)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:add')")
    @Log(title = "流转卡工序", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProCardProcess e) { proCardProcessService.insertProCardProcess(e); return success(e); }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:edit')")
    @Log(title = "流转卡工序", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProCardProcess e) { return toAjax(proCardProcessService.updateProCardProcess(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:cardprocess:remove')")
    @Log(title = "流转卡工序", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) { return toAjax(proCardProcessService.deleteProCardProcessByRecordIds(recordIds)); }
}
