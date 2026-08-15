package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;

public interface IWmMaterialStockService
{
    public List<WmMaterialStock> selectWmMaterialStockList(WmMaterialStock entity);
    public List<WmMaterialStock> selectWmMaterialStockAll();
    public WmMaterialStock selectWmMaterialStockByMaterialStockId(Long materialStockId);
    public int insertWmMaterialStock(WmMaterialStock entity);
    public int updateWmMaterialStock(WmMaterialStock entity);
    public int deleteWmMaterialStockByMaterialStockId(Long materialStockId);
    public int deleteWmMaterialStockByMaterialStockIds(Long[] materialStockIds);

    /**
     * 查可用批次列表（发料弹窗批次下拉用）：按 itemId 查所有 onhand>0 的批次。
     */
    public List<WmMaterialStock> selectAvailableBatches(Long itemId, Long warehouseId);

    /**
     * 按批次码精确反查库存记录（扫码用）。批次码冗余在库存表，同批次可分布在多仓/多库位。
     *
     * @param batchCode 批次码（会 trim）
     * @return 匹配的库存行列表（空码抛 ServiceException）
     */
    public List<WmMaterialStock> scanByBatchCode(String batchCode);
}