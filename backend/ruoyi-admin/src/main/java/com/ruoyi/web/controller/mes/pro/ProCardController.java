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
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.service.mes.pro.IProCardService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 流转卡Controller
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@RestController
@RequestMapping("/mes/pro/procard")
public class ProCardController extends BaseController
{
    @Autowired
    private IProCardService proCardService;

    @PreAuthorize("@ss.hasPermi('mes:pro:card:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProCard e) { startPage(); return getDataTable(proCardService.selectProCardList(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:card:query')")
    @GetMapping("/listAll")
    public AjaxResult listAll() { return success(proCardService.selectAll()); }

    @PreAuthorize("@ss.hasPermi('mes:pro:card:export')")
    @Log(title = "流转卡", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProCard e) {
        List<ProCard> list = proCardService.selectProCardList(e);
        ExcelUtil<ProCard> util = new ExcelUtil<ProCard>(ProCard.class);
        util.exportExcel(response, list, "流转卡数据");
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:card:query')")
    @GetMapping(value = "/{cardId}")
    public AjaxResult getInfo(@PathVariable("cardId") Long cardId) { return success(proCardService.selectProCardByCardId(cardId)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:card:add')")
    @Log(title = "流转卡", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProCard e) { proCardService.insertProCard(e); return success(e); }

    @PreAuthorize("@ss.hasPermi('mes:pro:card:edit')")
    @Log(title = "流转卡", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProCard e) { return toAjax(proCardService.updateProCard(e)); }

    @PreAuthorize("@ss.hasPermi('mes:pro:card:remove')")
    @Log(title = "流转卡", businessType = BusinessType.DELETE)
    @DeleteMapping("/{cardIds}")
    public AjaxResult remove(@PathVariable Long[] cardIds) { return toAjax(proCardService.deleteProCardByCardIds(cardIds)); }
}
