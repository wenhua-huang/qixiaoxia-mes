package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdVendorService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdVendor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 供应商Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/vendor")
public class MdVendorController extends BaseController
{
    @Autowired
    private IMdVendorService mdVendorService;

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdVendor mdVendor)
    {
        startPage();
        List<MdVendor> list = mdVendorService.selectMdVendorList(mdVendor);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<MdVendor> list = mdVendorService.selectMdVendorAllEnabled();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:export')")
    @Log(title = "供应商", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdVendor mdVendor)
    {
        List<MdVendor> list = mdVendorService.selectMdVendorList(mdVendor);
        ExcelUtil<MdVendor> util = new ExcelUtil<MdVendor>(MdVendor.class);
        util.exportExcel(response, list, "供应商数据");
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<MdVendor> util = new ExcelUtil<MdVendor>(MdVendor.class);
        util.importTemplateExcel(response, "供应商数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:import')")
    @Log(title = "供应商", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<MdVendor> util = new ExcelUtil<MdVendor>(MdVendor.class);
        List<MdVendor> vendorList = util.importExcel(file.getInputStream());
        String message = "供应商数据导入成功";
        return AjaxResult.success(message);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:query')")
    @GetMapping(value = "/{vendorId}")
    public AjaxResult getInfo(@PathVariable("vendorId") Long vendorId)
    {
        return AjaxResult.success(mdVendorService.selectMdVendorByVendorId(vendorId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:add')")
    @Log(title = "供应商", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdVendor mdVendor)
    {
        if (!mdVendorService.checkVendorCodeUnique(mdVendor))
        {
            return AjaxResult.error("供应商编码已存在！");
        }
        return toAjax(mdVendorService.insertMdVendor(mdVendor));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:edit')")
    @Log(title = "供应商", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdVendor mdVendor)
    {
        if (!mdVendorService.checkVendorCodeUnique(mdVendor))
        {
            return AjaxResult.error("供应商编码已存在！");
        }
        return toAjax(mdVendorService.updateMdVendor(mdVendor));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:vendor:remove')")
    @Log(title = "供应商", businessType = BusinessType.DELETE)
    @DeleteMapping("/{vendorIds}")
    public AjaxResult remove(@PathVariable Long[] vendorIds)
    {
        return toAjax(mdVendorService.deleteMdVendorByVendorIds(vendorIds));
    }
}
