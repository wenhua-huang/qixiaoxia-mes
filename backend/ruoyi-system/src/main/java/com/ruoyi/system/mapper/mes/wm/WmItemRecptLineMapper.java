package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.vo.ReturnableBatchVO;

public interface WmItemRecptLineMapper
{
    public List<WmItemRecptLine> selectWmItemRecptLineList(WmItemRecptLine entity);
    public List<WmItemRecptLine> selectWmItemRecptLineAll();
    public WmItemRecptLine selectWmItemRecptLineByLineId(Long lineId);
    public int insertWmItemRecptLine(WmItemRecptLine entity);
    public int updateWmItemRecptLine(WmItemRecptLine entity);
    public int deleteWmItemRecptLineByLineId(Long lineId);
    public int deleteWmItemRecptLineByLineIds(Long[] lineIds);

    /**
     * 可退批次聚合:按 (item, warehouse, batch) 聚合该 PO 已收货入库的可退量
     * SkipFactoryId: SQL 已用 pur_order_id 限定(同 PO 必同工厂),
     * 且子查询+外层 JOIN 结构让拦截器无法正确注入 factory_id 别名
     */
    @SkipFactoryId
    public List<ReturnableBatchVO> selectReturnableBatchesByPurOrderId(Long purOrderId);
}