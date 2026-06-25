package com.ruoyi.system.domain.mes.pro;

import java.util.List;

/**
 * 生产工单创建请求（包含工单头、BOM和工序参数）
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProWorkorderCreateRequest
{
    private ProWorkorder workorder;
    private List<ProWorkorderBom> bomList;
    private List<ProWorkorderParam> paramList;

    public ProWorkorder getWorkorder()
    {
        return workorder;
    }

    public void setWorkorder(ProWorkorder workorder)
    {
        this.workorder = workorder;
    }

    public List<ProWorkorderBom> getBomList()
    {
        return bomList;
    }

    public void setBomList(List<ProWorkorderBom> bomList)
    {
        this.bomList = bomList;
    }

    public List<ProWorkorderParam> getParamList()
    {
        return paramList;
    }

    public void setParamList(List<ProWorkorderParam> paramList)
    {
        this.paramList = paramList;
    }
}
