package com.ruoyi.web.controller.mes.sys;

import java.util.Date;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.sys.ISysTodoListService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.sys.SysTodoList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 通用待办事项Controller
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@RestController
@RequestMapping("/mes/sys/todolist")
public class SysTodoListController extends BaseController
{
    @Autowired
    private ISysTodoListService sysTodoListService;

    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysTodoList sysTodoList)
    {
        startPage();
        List<SysTodoList> list = sysTodoListService.selectSysTodoListList(sysTodoList);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:export')")
    @Log(title = "待办事项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysTodoList sysTodoList)
    {
        List<SysTodoList> list = sysTodoListService.selectSysTodoListList(sysTodoList);
        ExcelUtil<SysTodoList> util = new ExcelUtil<SysTodoList>(SysTodoList.class);
        util.exportExcel(response, list, "待办事项数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:query')")
    @GetMapping(value = "/{todoId}")
    public AjaxResult getInfo(@PathVariable("todoId") Long todoId)
    {
        return AjaxResult.success(sysTodoListService.selectSysTodoListByTodoId(todoId));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:add')")
    @Log(title = "待办事项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysTodoList sysTodoList)
    {
        return toAjax(sysTodoListService.insertSysTodoList(sysTodoList));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:edit')")
    @Log(title = "待办事项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysTodoList sysTodoList)
    {
        return toAjax(sysTodoListService.updateSysTodoList(sysTodoList));
    }

    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:remove')")
    @Log(title = "待办事项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{todoIds}")
    public AjaxResult remove(@PathVariable Long[] todoIds)
    {
        return toAjax(sysTodoListService.deleteSysTodoListByTodoIds(todoIds));
    }

    /**
     * 处理待办
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:edit')")
    @Log(title = "待办事项", businessType = BusinessType.UPDATE)
    @PutMapping("/{todoId}/handle")
    public AjaxResult handle(@PathVariable Long todoId, @RequestBody SysTodoList sysTodoList)
    {
        SysTodoList todo = sysTodoListService.selectSysTodoListByTodoId(todoId);
        if (todo == null)
        {
            return AjaxResult.error("待办事项不存在！");
        }
        todo.setStatus(sysTodoList.getStatus());
        todo.setHandleResult(sysTodoList.getHandleResult());
        todo.setHandleTime(DateUtils.getNowDate());
        return toAjax(sysTodoListService.updateSysTodoList(todo));
    }

    /**
     * 按状态统计当前用户待办数量
     */
    @PreAuthorize("@ss.hasPermi('mes:sys:todolist:list')")
    @GetMapping("/countByStatus")
    public AjaxResult countByStatus()
    {
        java.util.List<java.util.Map<String, Object>> rows = sysTodoListService.countByStatus(SecurityUtils.getUserId());
        java.util.Map<String, Long> result = new java.util.HashMap<>();
        long total = 0;
        for (java.util.Map<String, Object> row : rows)
        {
            String status = (String) row.get("status");
            long cnt = ((Number) row.get("cnt")).longValue();
            total += cnt;
            result.put(status.toLowerCase(), cnt);
        }
        result.put("total", total);
        result.putIfAbsent("pending", 0L);
        result.putIfAbsent("processing", 0L);
        result.putIfAbsent("completed", 0L);
        return AjaxResult.success(result);
    }
}
