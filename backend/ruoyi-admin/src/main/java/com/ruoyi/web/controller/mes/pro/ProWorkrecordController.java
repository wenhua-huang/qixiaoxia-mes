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
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;
import com.ruoyi.system.service.mes.pro.IProWorkrecordService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 上下工会话记录Controller
 * <p>
 * 提供打卡业务接口（clockIn/clockOut/activeSession/myWorkstations）+ 标准 CRUD（PC 端管理）
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/workrecord")
public class ProWorkrecordController extends BaseController
{
    @Autowired
    private IProWorkrecordService proWorkrecordService;

    // ════════════════════════════════════════════
    // 打卡业务接口（移动端 + PC 端共用）
    // ════════════════════════════════════════════

    /** 上工打卡（body 传 workstationId 必填 + 可选 taskId/workorderId） */
    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:clock')")
    @Log(title = "上工打卡", businessType = BusinessType.INSERT)
    @PutMapping("/clockIn")
    public AjaxResult clockIn(@RequestBody ProWorkrecord e) {
        proWorkrecordService.clockIn(e);
        return success("上工成功");
    }

    /** 下工结算（body 可传 recordId，不传则按当前登录用户结算） */
    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:clock')")
    @Log(title = "下工打卡", businessType = BusinessType.UPDATE)
    @PutMapping("/clockOut")
    public AjaxResult clockOut(@RequestBody(required = false) ProWorkrecord e) {
        proWorkrecordService.clockOut(e == null ? new ProWorkrecord() : e);
        return success("下工成功");
    }

    /** 查当前用户在岗会话（移动端进页面判上工/下工按钮） */
    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:clock')")
    @GetMapping("/activeSession")
    public AjaxResult activeSession() {
        return success(proWorkrecordService.selectActiveSession());
    }

    /** 查当前用户的绑定工位（绑定优先，快捷选择） */
    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:clock')")
    @GetMapping("/myWorkstations")
    public AjaxResult myWorkstations() {
        return success(proWorkrecordService.selectMyWorkstations());
    }

    /** 查当前登录用户的打卡历史（分页，移动端） */
    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:clock')")
    @GetMapping("/myHistory")
    public TableDataInfo myHistory(ProWorkrecord e) {
        startPage();
        return getDataTable(proWorkrecordService.selectMyHistory(e));
    }

    // ════════════════════════════════════════════
    // 标准 CRUD（PC 端管理）
    // ════════════════════════════════════════════

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProWorkrecord e) {
        startPage();
        return getDataTable(proWorkrecordService.selectProWorkrecordList(e));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() {
        return success(proWorkrecordService.selectAll());
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:export')")
    @Log(title = "上下工记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProWorkrecord e) {
        List<ProWorkrecord> list = proWorkrecordService.selectProWorkrecordList(e);
        ExcelUtil<ProWorkrecord> util = new ExcelUtil<>(ProWorkrecord.class);
        util.exportExcel(response, list, "上下工记录数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId) {
        return success(proWorkrecordService.selectProWorkrecordByRecordId(recordId));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:add')")
    @Log(title = "上下工记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProWorkrecord e) {
        return toAjax(proWorkrecordService.insertProWorkrecord(e));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:edit')")
    @Log(title = "上下工记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProWorkrecord e) {
        return toAjax(proWorkrecordService.updateProWorkrecord(e));
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:workrecord:remove')")
    @Log(title = "上下工记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds) {
        return toAjax(proWorkrecordService.deleteProWorkrecordByRecordIds(recordIds));
    }
}
