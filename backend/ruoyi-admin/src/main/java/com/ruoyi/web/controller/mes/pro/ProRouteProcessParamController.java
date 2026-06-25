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
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;
import com.ruoyi.system.service.mes.pro.IProRouteProcessParamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工艺路线工序参数Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/routeprocessparam")
public class ProRouteProcessParamController extends BaseController
{
    @Autowired
    private IProRouteProcessParamService proRouteProcessParamService;

    /**
     * 查询工艺路线工序参数列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProRouteProcessParam proRouteProcessParam)
    {
        startPage();
        List<ProRouteProcessParam> list = proRouteProcessParamService.selectProRouteProcessParamList(proRouteProcessParam);
        return getDataTable(list);
    }

    /**
     * 根据路线产品ID查询参数列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping("/listByRouteProductId/{routeProductId}")
    public AjaxResult listByRouteProductId(@PathVariable("routeProductId") Long routeProductId)
    {
        List<ProRouteProcessParam> list = proRouteProcessParamService.selectProRouteProcessParamByRouteProductId(routeProductId);
        return success(list);
    }

    /**
     * 导出工艺路线工序参数列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:export')")
    @Log(title = "工艺路线工序参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProRouteProcessParam proRouteProcessParam)
    {
        List<ProRouteProcessParam> list = proRouteProcessParamService.selectProRouteProcessParamList(proRouteProcessParam);
        ExcelUtil<ProRouteProcessParam> util = new ExcelUtil<ProRouteProcessParam>(ProRouteProcessParam.class);
        util.exportExcel(response, list, "工艺路线工序参数数据");
    }

    /**
     * 获取工艺路线工序参数详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proRouteProcessParamService.selectProRouteProcessParamByRecordId(recordId));
    }

    /**
     * 新增工艺路线工序参数
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:add')")
    @Log(title = "工艺路线工序参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProRouteProcessParam proRouteProcessParam)
    {
        proRouteProcessParamService.insertProRouteProcessParam(proRouteProcessParam);
        return success(proRouteProcessParam);
    }

    /**
     * 修改工艺路线工序参数
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:edit')")
    @Log(title = "工艺路线工序参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProRouteProcessParam proRouteProcessParam)
    {
        return toAjax(proRouteProcessParamService.updateProRouteProcessParam(proRouteProcessParam));
    }

    /**
     * 删除工艺路线工序参数
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:remove')")
    @Log(title = "工艺路线工序参数", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proRouteProcessParamService.deleteProRouteProcessParamByRecordIds(recordIds));
    }

    /**
     * 从工序参数模版批量初始化路线产品参数
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:add')")
    @Log(title = "初始化工序参数", businessType = BusinessType.INSERT)
    @PostMapping("/batchInitFromTemplate")
    public AjaxResult batchInitFromTemplate(Long routeProductId, Long processId)
    {
        int count = proRouteProcessParamService.batchInsertFromTemplate(routeProductId, processId);
        return success("已初始化 " + count + " 个参数");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:edit')")
    @Log(title = "批量更新工序参数", businessType = BusinessType.UPDATE)
    @PutMapping("/batchUpdate")
    public AjaxResult batchUpdate(@RequestBody List<ProRouteProcessParam> list)
    {
        return toAjax(proRouteProcessParamService.batchUpdate(list));
    }
}
