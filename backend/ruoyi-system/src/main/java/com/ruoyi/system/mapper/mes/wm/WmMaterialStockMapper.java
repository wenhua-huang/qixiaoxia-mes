package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;

public interface WmMaterialStockMapper
{
    /**
     * 根据唯一键查询库存记录（UPSERT 查重用）
     */
    public WmMaterialStock loadMaterialStock(WmMaterialStock stock);

    public List<WmMaterialStock> selectWmMaterialStockList(WmMaterialStock entity);
    public List<WmMaterialStock> selectWmMaterialStockAll();
    public WmMaterialStock selectWmMaterialStockByMaterialStockId(Long materialStockId);
    public int insertWmMaterialStock(WmMaterialStock entity);
    public int updateWmMaterialStock(WmMaterialStock entity);
    public int deleteWmMaterialStockByMaterialStockId(Long materialStockId);
    public int deleteWmMaterialStockByMaterialStockIds(Long[] materialStockIds);
}