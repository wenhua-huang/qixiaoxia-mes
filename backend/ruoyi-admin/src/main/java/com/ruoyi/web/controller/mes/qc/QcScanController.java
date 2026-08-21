package com.ruoyi.web.controller.mes.qc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.service.mes.qc.IQcIqcService;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;

/**
 * 质检移动端扫码查询Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-20
 */
@RestController
@RequestMapping("/mes/qc/scan")
public class QcScanController extends BaseController
{
    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IQcIqcService qcIqcService;

    /**
     * 扫收货单号：返回收货单及其 IQC 检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:query')")
    @GetMapping("/iqc")
    public AjaxResult scanIqc(@RequestParam("code") String recptCode)
    {
        // 扫码枪常带 CR/LF，trim 后再校验空串
        recptCode = StringUtils.trim(recptCode);
        if (StringUtils.isEmpty(recptCode))
        {
            return AjaxResult.error("收货单号不能为空");
        }
        WmItemRecpt recpt = wmItemRecptService.selectByRecptCode(recptCode);
        if (recpt == null)
        {
            return AjaxResult.error("收货单不存在：" + recptCode);
        }
        List<QcIqc> iqcList = qcIqcService.listBySource(QcConstants.SOURCE_ITEM_RECPT, recpt.getRecptId());
        Map<String, Object> data = new HashMap<>();
        data.put("recpt", recpt);
        data.put("iqcList", iqcList);
        return AjaxResult.success(data);
    }
}
