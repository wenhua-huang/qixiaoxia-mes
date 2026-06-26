package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdItemService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 物料产品Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/item")
public class MdItemController extends BaseController
{
    @Autowired
    private IMdItemService mdItemService;

    @PreAuthorize("@ss.hasPermi('mes:md:item:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<MdItem> list = mdItemService.selectMdItemAllEnabled();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdItem mdItem)
    {
        startPage();
        List<MdItem> list = mdItemService.selectMdItemList(mdItem);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:export')")
    @Log(title = "物料产品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdItem mdItem)
    {
        List<MdItem> list = mdItemService.selectMdItemList(mdItem);
        ExcelUtil<MdItem> util = new ExcelUtil<MdItem>(MdItem.class);
        util.exportExcel(response, list, "物料数据");
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<MdItem> util = new ExcelUtil<MdItem>(MdItem.class);
        util.importTemplateExcel(response, "物料数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:import')")
    @Log(title = "物料产品", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<MdItem> util = new ExcelUtil<MdItem>(MdItem.class);
        List<MdItem> itemList = util.importExcel(file.getInputStream());
        String message = mdItemService.importItem(itemList, updateSupport, getUsername());
        return AjaxResult.success(message);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return AjaxResult.success(mdItemService.selectMdItemById(itemId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:add')")
    @Log(title = "物料产品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdItem mdItem)
    {
        if (!mdItemService.checkItemCodeUnique(mdItem))
        {
            return AjaxResult.error("物料编码已存在！");
        }
        return toAjax(mdItemService.insertMdItem(mdItem));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:edit')")
    @Log(title = "物料产品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdItem mdItem)
    {
        if (!mdItemService.checkItemCodeUnique(mdItem))
        {
            return AjaxResult.error("物料编码已存在！");
        }
        return toAjax(mdItemService.updateMdItem(mdItem));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:item:remove')")
    @Log(title = "物料产品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(mdItemService.deleteMdItemByIds(itemIds));
    }
}
