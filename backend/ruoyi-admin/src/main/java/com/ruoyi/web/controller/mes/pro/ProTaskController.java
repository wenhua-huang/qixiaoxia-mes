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
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.service.mes.pro.IProTaskService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 生产任务/排产Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/task")
public class ProTaskController extends BaseController
{
    @Autowired
    private IProTaskService proTaskService;

    @Autowired(required = false)
    private com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator autoCodeGenerator;

    @PreAuthorize("@ss.hasPermi('mes:pro:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProTask proTask)
    {
        startPage();
        List<ProTask> list = proTaskService.selectProTaskList(proTask);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        List<ProTask> list = proTaskService.selectAll();
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:export')")
    @Log(title = "生产任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProTask proTask)
    {
        List<ProTask> list = proTaskService.selectProTaskList(proTask);
        ExcelUtil<ProTask> util = new ExcelUtil<ProTask>(ProTask.class);
        util.exportExcel(response, list, "生产任务数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:query')")
    @GetMapping("/progressByWorkorder/{workorderId}")
    public AjaxResult progressByWorkorder(@PathVariable Long workorderId)
    {
        return success(proTaskService.selectProcessProgressByWorkorder(workorderId));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:query')")
    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        return success(proTaskService.selectProTaskByTaskId(taskId));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:add')")
    @Log(title = "生产任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProTask proTask)
    {
        // 自动生成任务编码（与 ktg-mes 一致）
        if (proTask.getTaskCode() == null || proTask.getTaskCode().isEmpty())
        {
            if (autoCodeGenerator != null)
            {
                try { proTask.setTaskCode(autoCodeGenerator.genSerialCode("TASK_CODE", null)); }
                catch (Exception e) { proTask.setTaskCode("TASK" + System.currentTimeMillis()); }
            }
            else
            {
                proTask.setTaskCode("TASK" + System.currentTimeMillis());
            }
        }
        // 校验
        if (proTask.getQuantity() != null && proTask.getQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0)
            return error("排产数量必须大于0！");
        if (proTask.getDuration() != null && proTask.getDuration() <= 0)
            proTask.setDuration(1);
        proTaskService.insertProTask(proTask);
        return success(proTask);
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProTask proTask)
    {
        return toAjax(proTaskService.updateProTask(proTask));
    }

    /**
     * 下发排产任务：NORMAL → PRODUCING
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务下发", businessType = BusinessType.UPDATE)
    @PutMapping("/dispatch/{taskId}")
    public AjaxResult dispatch(@PathVariable Long taskId)
    {
        ProTask task = proTaskService.selectProTaskByTaskId(taskId);
        if (task == null) return error("任务不存在");
        if (!"NORMAL".equals(task.getStatus()) && !"PREPARE".equals(task.getStatus()))
            return error("只有待排产/正常状态的任务才能下发，当前状态：" + task.getStatus());
        task.setStatus("PRODUCING");
        proTaskService.updateProTask(task);
        return success();
    }

    /**
     * 完成任务：PRODUCING → COMPLETED
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务完成", businessType = BusinessType.UPDATE)
    @PutMapping("/complete/{taskId}")
    public AjaxResult complete(@PathVariable Long taskId)
    {
        ProTask task = proTaskService.selectProTaskByTaskId(taskId);
        if (task == null) return error("任务不存在");
        if (!"PRODUCING".equals(task.getStatus()))
            return error("只有生产中的任务才能完成，当前状态：" + task.getStatus());
        task.setStatus("COMPLETED");
        proTaskService.updateProTask(task);
        return success();
    }

    /**
     * 取消任务
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{taskId}")
    public AjaxResult cancel(@PathVariable Long taskId)
    {
        ProTask task = proTaskService.selectProTaskByTaskId(taskId);
        if (task == null) return error("任务不存在");
        if ("COMPLETED".equals(task.getStatus()))
            return error("已完成的任务不能取消");
        task.setStatus("CANCEL");
        proTaskService.updateProTask(task);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:remove')")
    @Log(title = "生产任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(proTaskService.deleteProTaskByTaskIds(taskIds));
    }
}
