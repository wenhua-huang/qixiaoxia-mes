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
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.service.mes.pro.IProRouteProductService;
import com.ruoyi.system.service.mes.pro.IProRouteProcessParamService;
import com.ruoyi.system.service.mes.pro.IProRouteProcessService;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工艺路线产品Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/routeproduct")
public class ProRouteProductController extends BaseController
{
    @Autowired
    private IProRouteProductService proRouteProductService;

    @Autowired
    private IProRouteProcessParamService proRouteProcessParamService;

    @Autowired
    private IProRouteProcessService proRouteProcessService;

    /**
     * 查询工艺路线产品列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProRouteProduct proRouteProduct)
    {
        startPage();
        List<ProRouteProduct> list = proRouteProductService.selectProRouteProductList(proRouteProduct);
        return getDataTable(list);
    }

    /**
     * 根据路线ID查询产品列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping("/listByRouteId/{routeId}")
    public AjaxResult listByRouteId(@PathVariable("routeId") Long routeId)
    {
        List<ProRouteProduct> list = proRouteProductService.selectProRouteProductByRouteId(routeId);
        return success(list);
    }

    /**
     * 导出工艺路线产品列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:export')")
    @Log(title = "工艺路线产品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProRouteProduct proRouteProduct)
    {
        List<ProRouteProduct> list = proRouteProductService.selectProRouteProductList(proRouteProduct);
        ExcelUtil<ProRouteProduct> util = new ExcelUtil<ProRouteProduct>(ProRouteProduct.class);
        util.exportExcel(response, list, "工艺路线产品数据");
    }

    /**
     * 获取工艺路线产品详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(proRouteProductService.selectProRouteProductByRecordId(recordId));
    }

    /**
     * 新增工艺路线产品
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:add')")
    @Log(title = "工艺路线产品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProRouteProduct proRouteProduct)
    {
        proRouteProductService.insertProRouteProduct(proRouteProduct);
        // 自动从模版(L1)复制默认参数值到路线工序参数(L2)
        try {
            ProRouteProcess query = new ProRouteProcess();
            query.setRouteId(proRouteProduct.getRouteId());
            List<ProRouteProcess> processes = proRouteProcessService.selectProRouteProcessList(query);
            if (processes != null) {
                for (ProRouteProcess proc : processes) {
                    proRouteProcessParamService.batchInsertFromTemplate(
                        proRouteProduct.getRecordId(), proc.getProcessId());
                }
            }
        } catch (Exception ignored) { /* 参数复制失败不影响路线产品创建 */ }
        return success(proRouteProduct);
    }

    /**
     * 修改工艺路线产品
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:edit')")
    @Log(title = "工艺路线产品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProRouteProduct proRouteProduct)
    {
        return toAjax(proRouteProductService.updateProRouteProduct(proRouteProduct));
    }

    /**
     * 删除工艺路线产品
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:remove')")
    @Log(title = "工艺路线产品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(proRouteProductService.deleteProRouteProductByRecordIds(recordIds));
    }

    /**
     * 为SKU变体复制父产品的工艺路线关联（含BOM + 参数）
     * parentItemId: 父产品ID, skuItemId: SKU变体ID, skuItemCode: SKU编码, skuItemName: SKU名称
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:proroute:add')")
    @Log(title = "SKU路线复制", businessType = BusinessType.INSERT)
    @PostMapping("/copyRouteProductForSku")
    public AjaxResult copyRouteProductForSku(Long parentItemId, Long skuItemId, String skuItemCode, String skuItemName)
    {
        int count = proRouteProductService.copyRouteProductForSku(parentItemId, skuItemId, skuItemCode, skuItemName);
        return success("成功复制 " + count + " 条工艺路线关联");
    }
}
