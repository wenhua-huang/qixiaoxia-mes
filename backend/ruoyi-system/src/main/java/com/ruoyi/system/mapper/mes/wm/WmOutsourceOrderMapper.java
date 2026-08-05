package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;

/**
 * 外协订单 Mapper
 * @author qixiaoxia
 */
public interface WmOutsourceOrderMapper
{
    List<WmOutsourceOrder> selectOutsourceOrderList(WmOutsourceOrder query);

    WmOutsourceOrder selectOutsourceOrderByOrderId(Long orderId);

    int insertOutsourceOrder(WmOutsourceOrder order);

    int updateOutsourceOrder(WmOutsourceOrder order);
}
