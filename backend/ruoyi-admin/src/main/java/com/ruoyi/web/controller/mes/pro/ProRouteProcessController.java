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
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.service.mes.pro.IProRouteProcessService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工艺路线-工序组成Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/routeprocess")
public class ProRouteProcessController extends BaseController
{
    @Autowired
    private IProRouteProcessService proRouteProcessService;

    /**
     * 查询工艺路线-工序组成列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProRouteProcess proRouteProcess)
    {
        startPage();
        List<ProRouteProcess> list = proRouteProcessService.selectProRouteProcessList(proRouteProcess);
        return getDataTable(list);
    }

    /**
     * 根据工艺路线ID查询工序组成列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping("/listByRouteId/{routeId}")
    public AjaxResult listByRouteId(@PathVariable("routeId") Long routeId)
    {
        List<ProRouteProcess> list = proRouteProcessService.selectProRouteProcessByRouteId(routeId);
        return success(list);
    }

    /**
     * 导出工艺路线-工序组成列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:export')")
    @Log(title = "工艺路线-工序组成", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProRouteProcess proRouteProcess)
    {
        List<ProRouteProcess> list = proRouteProcessService.selectProRouteProcessList(proRouteProcess);
        ExcelUtil<ProRouteProcess> util = new ExcelUtil<ProRouteProcess>(ProRouteProcess.class);
        util.exportExcel(response, list, "工艺路线-工序组成数据");
    }

    /**
     * 获取工艺路线-工序组成详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proRouteProcessService.selectProRouteProcessByRecordId(recordId));
    }

    /**
     * 新增工艺路线-工序组成
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:add')")
    @Log(title = "工艺路线-工序组成", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProRouteProcess proRouteProcess)
    {
        proRouteProcessService.insertProRouteProcess(proRouteProcess);
        return success(proRouteProcess);
    }

    /**
     * 修改工艺路线-工序组成
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:edit')")
    @Log(title = "工艺路线-工序组成", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProRouteProcess proRouteProcess)
    {
        return toAjax(proRouteProcessService.updateProRouteProcess(proRouteProcess));
    }

    /**
     * 删除工艺路线-工序组成
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:remove')")
    @Log(title = "工艺路线-工序组成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proRouteProcessService.deleteProRouteProcessByRecordIds(recordIds));
    }
}
