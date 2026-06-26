package com.ruoyi.web.controller.mes.pro;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.domain.mes.pro.ProProcess;
import com.ruoyi.system.service.mes.pro.IProProcessService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 生产工序Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/process")
public class ProProcessController extends BaseController
{
    @Autowired
    private IProProcessService proProcessService;

    /**
     * 查询生产工序列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProProcess proProcess)
    {
        startPage();
        List<ProProcess> list = proProcessService.selectProProcessList(proProcess);
        return getDataTable(list);
    }

    /**
     * 查询所有生产工序列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<ProProcess> list = proProcessService.selectProProcessAll();
        return success(list);
    }

    /**
     * 导出生产工序列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:export')")
    @Log(title = "生产工序", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProProcess proProcess)
    {
        List<ProProcess> list = proProcessService.selectProProcessList(proProcess);
        ExcelUtil<ProProcess> util = new ExcelUtil<ProProcess>(ProProcess.class);
        util.exportExcel(response, list, "生产工序数据");
    }

    /**
     * 获取生产工序详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:query')")
    @GetMapping(value = "/{processId}")
    public AjaxResult getInfo(@PathVariable("processId") Long processId)
    {
        return success(proProcessService.selectProProcessByProcessId(processId));
    }

    /**
     * 新增生产工序
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:add')")
    @Log(title = "生产工序", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProProcess proProcess)
    {
        proProcessService.checkProcessCodeUnique(proProcess);
        proProcessService.insertProProcess(proProcess);
        return success(proProcess);
    }

    /**
     * 修改生产工序
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:edit')")
    @Log(title = "生产工序", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProProcess proProcess)
    {
        proProcessService.checkProcessCodeUnique(proProcess);
        return toAjax(proProcessService.updateProProcess(proProcess));
    }

    /**
     * 删除生产工序
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:remove')")
    @Log(title = "生产工序", businessType = BusinessType.DELETE)
    @DeleteMapping("/{processIds}")
    public AjaxResult remove(@PathVariable Long[] processIds)
    {
        return toAjax(proProcessService.deleteProProcessByProcessIds(processIds));
    }
}
