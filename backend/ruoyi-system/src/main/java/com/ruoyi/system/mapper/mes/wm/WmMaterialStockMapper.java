package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import org.apache.ibatis.annotations.Param;

public interface WmMaterialStockMapper
{
    /**
     * 根据唯一键查询库存记录（UPSERT 查重用）
     */
    public WmMaterialStock loadMaterialStock(WmMaterialStock stock);

    /**
     * 根据唯一键查询库存记录并锁定行（SELECT FOR UPDATE，事务内使用）
     */
    public WmMaterialStock loadMaterialStockForUpdate(WmMaterialStock stock);

    public List<WmMaterialStock> selectWmMaterialStockList(WmMaterialStock entity);
    public List<WmMaterialStock> selectWmMaterialStockAll();
    public WmMaterialStock selectWmMaterialStockByMaterialStockId(Long materialStockId);
    public int insertWmMaterialStock(WmMaterialStock entity);
    public int updateWmMaterialStock(WmMaterialStock entity);
    public int deleteWmMaterialStockByMaterialStockId(Long materialStockId);
    public int deleteWmMaterialStockByMaterialStockIds(Long[] materialStockIds);

    /**
     * FIFO 批次自动分配：按 itemId 查可用库存（quantity_available > 0），
     * 可选限定 warehouseId（为 null 表示跨仓），按 create_time 升序（先进先出）并加行锁。
     * 不限定 vendor_id/workorder_id/batch_id —— 跨任意批次/vendor 分配。
     */
    public List<WmMaterialStock> selectAvailableStocksForFifo(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId,
            @Param("qualityStatus") String qualityStatus);
}