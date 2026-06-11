package com.ruoyi.web.controller.mes.md;

import java.util.List;
import com.ruoyi.system.service.mes.md.IMdFactoryService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.mes.md.MdFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工厂定义Controller（只读 — 工厂由开发人员直接SQL操作，仅提供下拉查询接口）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/mes/md/factory")
public class MdFactoryController extends BaseController
{
    @Autowired
    private IMdFactoryService mdFactoryService;

    /**
     * 分页查询工厂列表（Vendor页面外协工厂下拉等场景使用）
     */
    @PreAuthorize("@ss.hasPermi('mes:md:factory:list')")
    @GetMapping("/list")
    public TableDataInfo list(MdFactory mdFactory)
    {
        startPage();
        List<MdFactory> list = mdFactoryService.selectMdFactoryList(mdFactory);
        return getDataTable(list);
    }

    /**
     * 查询所有启用工厂（下拉选择器用）
     */
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        MdFactory mdFactory = new MdFactory();
        mdFactory.setEnableFlag("1");
        List<MdFactory> list = mdFactoryService.selectMdFactoryList(mdFactory);
        return AjaxResult.success(list);
    }
}
