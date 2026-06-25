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
import com.ruoyi.system.domain.mes.pro.ProProcessContent;
import com.ruoyi.system.service.mes.pro.IProProcessContentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 工序作业内容Controller
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@RestController
@RequestMapping("/mes/pro/processcontent")
public class ProProcessContentController extends BaseController
{
    @Autowired
    private IProProcessContentService proProcessContentService;

    /**
     * 查询工序作业内容列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProProcessContent proProcessContent)
    {
        startPage();
        List<ProProcessContent> list = proProcessContentService.selectProProcessContentList(proProcessContent);
        return getDataTable(list);
    }

    /**
     * 根据工序ID查询作业内容
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:query')")
    @GetMapping("/listByProcessId/{processId}")
    public AjaxResult listByProcessId(@PathVariable("processId") Long processId)
    {
        List<ProProcessContent> list = proProcessContentService.selectProProcessContentByProcessId(processId);
        return success(list);
    }

    /**
     * 获取工序作业内容详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:query')")
    @GetMapping(value = "/{contentId}")
    public AjaxResult getInfo(@PathVariable("contentId") Long contentId)
    {
        return success(proProcessContentService.selectProProcessContentByContentId(contentId));
    }

    /**
     * 新增工序作业内容
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:add')")
    @Log(title = "工序作业内容", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProProcessContent proProcessContent)
    {
        proProcessContentService.insertProProcessContent(proProcessContent);
        return success(proProcessContent);
    }

    /**
     * 修改工序作业内容
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:edit')")
    @Log(title = "工序作业内容", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProProcessContent proProcessContent)
    {
        return toAjax(proProcessContentService.updateProProcessContent(proProcessContent));
    }

    /**
     * 删除工序作业内容
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:process:remove')")
    @Log(title = "工序作业内容", businessType = BusinessType.DELETE)
    @DeleteMapping("/{contentIds}")
    public AjaxResult remove(@PathVariable Long[] contentIds)
    {
        return toAjax(proProcessContentService.deleteProProcessContentByContentIds(contentIds));
    }
}
