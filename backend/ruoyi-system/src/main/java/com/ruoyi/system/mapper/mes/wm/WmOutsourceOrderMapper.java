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

    /** 按工单ID查全部外协单（避免 N+1，一次查全后按 processId 分组） */
    List<WmOutsourceOrder> selectByWorkorderId(Long workorderId);

    WmOutsourceOrder selectOutsourceOrderByOrderId(Long orderId);

    int insertOutsourceOrder(WmOutsourceOrder order);

    int updateOutsourceOrder(WmOutsourceOrder order);

    /** 删除外协单头（仅 DRAFT 状态，由 Service 层校验状态后调用） */
    int deleteOutsourceOrderByOrderId(Long orderId);
}
