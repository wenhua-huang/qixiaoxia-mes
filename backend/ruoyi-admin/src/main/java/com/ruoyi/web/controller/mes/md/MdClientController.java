package com.ruoyi.web.controller.mes.md;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdClientService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客户Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/client")
public class MdClientController extends BaseController
{
    @Autowired
    private IMdClientService mdClientService;

    @PreAuthorize("@ss.hasPermi('mes:md:client:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdClient mdClient)
    {
        startPage();
        List<MdClient> list = mdClientService.selectMdClientList(mdClient);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<MdClient> list = mdClientService.selectMdClientAllEnabled();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:export')")
    @Log(title = "客户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdClient mdClient)
    {
        List<MdClient> list = mdClientService.selectMdClientList(mdClient);
        ExcelUtil<MdClient> util = new ExcelUtil<MdClient>(MdClient.class);
        util.exportExcel(response, list, "客户数据");
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<MdClient> util = new ExcelUtil<MdClient>(MdClient.class);
        util.importTemplateExcel(response, "客户数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:import')")
    @Log(title = "客户", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<MdClient> util = new ExcelUtil<MdClient>(MdClient.class);
        List<MdClient> clientList = util.importExcel(file.getInputStream());
        String message = "客户数据导入成功";
        return AjaxResult.success(message);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:query')")
    @GetMapping(value = "/{clientId}")
    public AjaxResult getInfo(@PathVariable("clientId") Long clientId)
    {
        return AjaxResult.success(mdClientService.selectMdClientByClientId(clientId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:add')")
    @Log(title = "客户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdClient mdClient)
    {
        if (!mdClientService.checkClientCodeUnique(mdClient))
        {
            return AjaxResult.error("客户编码已存在！");
        }
        return toAjax(mdClientService.insertMdClient(mdClient));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:edit')")
    @Log(title = "客户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdClient mdClient)
    {
        if (!mdClientService.checkClientCodeUnique(mdClient))
        {
            return AjaxResult.error("客户编码已存在！");
        }
        return toAjax(mdClientService.updateMdClient(mdClient));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:client:remove')")
    @Log(title = "客户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{clientIds}")
    public AjaxResult remove(@PathVariable Long[] clientIds)
    {
        return toAjax(mdClientService.deleteMdClientByClientIds(clientIds));
    }
}
