package com.ruoyi.system.domain.mes.pro;

import java.util.List;
import java.util.Map;

/**
 * 生产工单详情响应VO（工单头+BOM+参数+路线工序+路线选项，一次返回前端所需全部数据）
 *
 * @author qixiaoxia
 * @date 2026-06-22
 */
public class ProWorkorderDetailVO
{
    private ProWorkorder workorder;
    private List<ProWorkorderBom> bomList;
    /** 参数列表（已富化：含 _paramName、_processName、paramName、standardValue 等前端展示字段） */
    private List<Map<String, Object>> paramList;
    /** 工艺路线工序列表（用于 Step 2 accordion 骨架） */
    private List<ProRouteProcess> routeProcesses;
    /** 可选工艺路线产品列表（已富化：含 _routeName 显示字段） */
    private List<Map<String, Object>> routeOptions;

    public ProWorkorder getWorkorder() { return workorder; }
    public void setWorkorder(ProWorkorder workorder) { this.workorder = workorder; }

    public List<ProWorkorderBom> getBomList() { return bomList; }
    public void setBomList(List<ProWorkorderBom> bomList) { this.bomList = bomList; }

    public List<Map<String, Object>> getParamList() { return paramList; }
    public void setParamList(List<Map<String, Object>> paramList) { this.paramList = paramList; }

    public List<ProRouteProcess> getRouteProcesses() { return routeProcesses; }
    public void setRouteProcesses(List<ProRouteProcess> routeProcesses) { this.routeProcesses = routeProcesses; }

    public List<Map<String, Object>> getRouteOptions() { return routeOptions; }
    public void setRouteOptions(List<Map<String, Object>> routeOptions) { this.routeOptions = routeOptions; }
}
