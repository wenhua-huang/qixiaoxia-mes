package com.ruoyi.web.controller.mes.md;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.system.service.mes.md.IMdWorkstationService;
import com.ruoyi.system.service.mes.md.IMdWorkstationMachineService;
import com.ruoyi.system.service.mes.md.IMdWorkstationWorkerService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.md.MdWorkstationMachine;
import com.ruoyi.system.domain.mes.md.MdWorkstationWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工作站Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/workstation")
public class MdWorkstationController extends BaseController
{
    @Autowired
    private IMdWorkstationService mdWorkstationService;

    @Autowired
    private IMdWorkstationMachineService mdWorkstationMachineService;

    @Autowired
    private IMdWorkstationWorkerService mdWorkstationWorkerService;

    // ===== 工作站 CRUD =====

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        MdWorkstation cond = new MdWorkstation();
        cond.setEnableFlag("1");
        List<MdWorkstation> list = mdWorkstationService.selectMdWorkstationList(cond);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdWorkstation mdWorkstation)
    {
        startPage();
        List<MdWorkstation> list = mdWorkstationService.selectMdWorkstationList(mdWorkstation);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:export')")
    @Log(title = "工作站", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdWorkstation mdWorkstation)
    {
        List<MdWorkstation> list = mdWorkstationService.selectMdWorkstationList(mdWorkstation);
        ExcelUtil<MdWorkstation> util = new ExcelUtil<MdWorkstation>(MdWorkstation.class);
        util.exportExcel(response, list, "工作站数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:query')")
    @GetMapping(value = "/{workstationId}")
    public AjaxResult getInfo(@PathVariable("workstationId") Long workstationId)
    {
        return AjaxResult.success(mdWorkstationService.selectMdWorkstationByWorkstationId(workstationId));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:add')")
    @Log(title = "工作站", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MdWorkstation mdWorkstation)
    {
        if (!mdWorkstationService.checkWorkstationCodeUnique(mdWorkstation))
        {
            return AjaxResult.error("工作站编码已存在！");
        }
        return toAjax(mdWorkstationService.insertMdWorkstation(mdWorkstation));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:edit')")
    @Log(title = "工作站", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MdWorkstation mdWorkstation)
    {
        if (!mdWorkstationService.checkWorkstationCodeUnique(mdWorkstation))
        {
            return AjaxResult.error("工作站编码已存在！");
        }
        return toAjax(mdWorkstationService.updateMdWorkstation(mdWorkstation));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:remove')")
    @Log(title = "工作站", businessType = BusinessType.DELETE)
    @DeleteMapping("/{workstationIds}")
    public AjaxResult remove(@PathVariable Long[] workstationIds)
    {
        return toAjax(mdWorkstationService.deleteMdWorkstationByWorkstationIds(workstationIds));
    }

    // ===== 工作站-设备 子资源 =====

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:query')")
    @GetMapping("/{workstationId}/machines")
    public AjaxResult listMachines(@PathVariable("workstationId") Long workstationId)
    {
        MdWorkstationMachine machine = new MdWorkstationMachine();
        machine.setWorkstationId(workstationId);
        List<MdWorkstationMachine> list = mdWorkstationMachineService.selectMdWorkstationMachineList(machine);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:add')")
    @Log(title = "工作站-设备", businessType = BusinessType.INSERT)
    @PostMapping("/machine")
    public AjaxResult addMachine(@RequestBody MdWorkstationMachine mdWorkstationMachine)
    {
        return toAjax(mdWorkstationMachineService.insertMdWorkstationMachine(mdWorkstationMachine));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:edit')")
    @Log(title = "工作站-设备", businessType = BusinessType.UPDATE)
    @PutMapping("/machine")
    public AjaxResult editMachine(@RequestBody MdWorkstationMachine mdWorkstationMachine)
    {
        return toAjax(mdWorkstationMachineService.updateMdWorkstationMachine(mdWorkstationMachine));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:remove')")
    @Log(title = "工作站-设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/machine/{recordIds}")
    public AjaxResult removeMachine(@PathVariable Long[] recordIds)
    {
        return toAjax(mdWorkstationMachineService.deleteMdWorkstationMachineByRecordIds(recordIds));
    }

    // ===== 工作站-人员 子资源 =====

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:query')")
    @GetMapping("/{workstationId}/workers")
    public AjaxResult listWorkers(@PathVariable("workstationId") Long workstationId)
    {
        MdWorkstationWorker worker = new MdWorkstationWorker();
        worker.setWorkstationId(workstationId);
        List<MdWorkstationWorker> list = mdWorkstationWorkerService.selectMdWorkstationWorkerList(worker);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:add')")
    @Log(title = "工作站-人员", businessType = BusinessType.INSERT)
    @PostMapping("/worker")
    public AjaxResult addWorker(@RequestBody MdWorkstationWorker mdWorkstationWorker)
    {
        return toAjax(mdWorkstationWorkerService.insertMdWorkstationWorker(mdWorkstationWorker));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:edit')")
    @Log(title = "工作站-人员", businessType = BusinessType.UPDATE)
    @PutMapping("/worker")
    public AjaxResult editWorker(@RequestBody MdWorkstationWorker mdWorkstationWorker)
    {
        return toAjax(mdWorkstationWorkerService.updateMdWorkstationWorker(mdWorkstationWorker));
    }

    @PreAuthorize("@ss.hasPermi('mes:md:workstation:remove')")
    @Log(title = "工作站-人员", businessType = BusinessType.DELETE)
    @DeleteMapping("/worker/{recordIds}")
    public AjaxResult removeWorker(@PathVariable Long[] recordIds)
    {
        return toAjax(mdWorkstationWorkerService.deleteMdWorkstationWorkerByRecordIds(recordIds));
    }
}
