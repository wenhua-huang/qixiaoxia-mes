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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.mes.pro.ProConstants;
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

    @Autowired
    private com.ruoyi.system.service.mes.pro.IProQcBlockService qcBlockService;

    @PreAuthorize("@ss.hasPermi('mes:pro:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProTask proTask)
    {
        startPage();
        List<ProTask> list = proTaskService.selectProTaskList(proTask);
        return getDataTable(list);
    }

    /**
     * 移动端待报工列表：仅厂内 PRODUCING 任务且工单处于 PREPARE/PRODUCING。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:list')")
    @GetMapping("/reportableList")
    public TableDataInfo reportableList(ProTask proTask)
    {
        startPage();
        List<ProTask> list = proTaskService.selectReportableTaskList(proTask);
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
        // 机台必填：厂内工序排产必须落到具体工作站；外协任务打 VENDOR 占位(id=0)，不占厂内机台（甘特页可手工建外协任务）
        if (!isVendorPlaceholder(proTask)
                && (proTask.getWorkstationId() == null || proTask.getWorkstationId() <= 0))
            return error("请选择机台");
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
        // 机台校验：传了机台则必须是真实工作站(>0)；外协 VENDOR 占位(id=0)放行（前端编辑外协任务会携带 VENDOR 快照）。
        // id=null 表示部分更新不改机台列，照旧放行
        Long workstationId = proTask.getWorkstationId();
        if (workstationId != null && workstationId <= 0 && !isVendorPlaceholder(proTask))
            return error("请选择机台");
        return toAjax(proTaskService.updateProTask(proTask));
    }

    /**
     * 外协虚拟工作站占位：workstation_code='VENDOR'（与下发/报工/开工等机台闸门口径一致，见 ProConstants.WS_CODE_VENDOR）
     */
    private boolean isVendorPlaceholder(ProTask proTask)
    {
        return ProConstants.WS_CODE_VENDOR.equals(proTask.getWorkstationCode());
    }

    /**
     * 下发排产任务：NORMAL/PREPARE → PRODUCING
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务下发", businessType = BusinessType.UPDATE)
    @PutMapping("/dispatch/{taskId}")
    public AjaxResult dispatch(@PathVariable Long taskId)
    {
        proTaskService.dispatchTask(taskId);
        return success();
    }

    /**
     * 完成任务：PRODUCING → COMPLETED（末工序完成时自动检查工单完工）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务完成", businessType = BusinessType.UPDATE)
    @PutMapping("/complete/{taskId}")
    public AjaxResult complete(@PathVariable Long taskId)
    {
        proTaskService.completeTask(taskId);
        return success();
    }

    /**
     * 取消任务：非终态 → CANCEL
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:edit')")
    @Log(title = "生产任务取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{taskId}")
    public AjaxResult cancel(@PathVariable Long taskId)
    {
        proTaskService.cancelTask(taskId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:task:remove')")
    @Log(title = "生产任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable Long[] taskIds)
    {
        return toAjax(proTaskService.deleteProTaskByTaskIds(taskIds));
    }

    /**
     * 质检不合格拦截授权放行：留痕 + 关闭拦截待办（理由至少 2 个字符）
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:release')")
    @Log(title = "质检拦截放行", businessType = BusinessType.UPDATE)
    @PutMapping("/releaseQcBlock/{taskId}")
    public AjaxResult releaseQcBlock(@PathVariable("taskId") Long taskId,
                                     @RequestParam("reason") String reason)
    {
        qcBlockService.release(taskId, reason);
        return success();
    }

    /**
     * 批量查询任务质检锁态（PC 放行工作台用，单次最多 100 个任务）
     *
     * @return key=任务ID字符串，value={blocked:boolean, reason:string}
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:task:list')")
    @PostMapping("/qcBlockState")
    public AjaxResult qcBlockState(@RequestBody List<Long> taskIds)
    {
        return success(qcBlockService.qcBlockState(taskIds));
    }
}
