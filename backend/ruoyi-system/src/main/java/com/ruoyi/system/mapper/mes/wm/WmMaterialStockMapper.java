package com.ruoyi.system.mapper.mes.wm;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.vo.WmStockWarehouseSummary;
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

    /** 按 materialStockId 查询并锁定行（发料指定批次扣减用） */
    public WmMaterialStock selectMaterialStockForUpdateById(Long materialStockId);
    public int insertWmMaterialStock(WmMaterialStock entity);
    public int updateWmMaterialStock(WmMaterialStock entity);
    public int deleteWmMaterialStockByMaterialStockId(Long materialStockId);
    public int deleteWmMaterialStockByMaterialStockIds(Long[] materialStockIds);

    /**
     * FIFO 批次自动分配（预占用）：按 itemId 查可用库存（quantity_available > 0），
     * 可选限定 warehouseId（为 null 表示跨仓），按 create_time 升序（先进先出）并加行锁。
     * 不限定 vendor_id/workorder_id/batch_id —— 跨任意批次/vendor 分配。
     */
    public List<WmMaterialStock> selectAvailableStocksForFifo(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId,
            @Param("qualityStatus") String qualityStatus);

    /**
     * 查可用批次列表（前端发料弹窗批次下拉用，只读不加行锁）。
     * 返回所有 onhand>0 的批次记录（含已预占的），前端据此选择指定发料批次。
     */
    public List<WmMaterialStock> selectAvailableBatches(
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId,
            @Param("qualityStatus") String qualityStatus);

    /**
     * 按 itemId 集合聚合各仓库可用量（SUM quantity_available），用于「从销售订单生成」时建议出库仓库与按仓拆行。
     * 支持多 itemId（工单反查精确制导：一个销售行可能关联多张工单产出多个变体），按 (item_id, warehouse_id) 分组。
     * 按 MIN(create_time) ASC 排序（FIFO：早入库的仓优先）。factory_id 由拦截器注入。
     *
     * @param itemIds 物料ID集合（空集合返回空列表）
     */
    public List<WmStockWarehouseSummary> selectStockWarehouseSummary(@Param("itemIds") Collection<Long> itemIds);

    /**
     * 按 itemId 集合批量查库存（消除逐物料 N+1，factory_id 由拦截器注入）。
     *
     * @param itemIds 物料ID集合（空集合返回空列表）
     * @return 匹配的库存记录
     */
    public List<WmMaterialStock> selectByItemIds(@Param("itemIds") Collection<Long> itemIds);
}