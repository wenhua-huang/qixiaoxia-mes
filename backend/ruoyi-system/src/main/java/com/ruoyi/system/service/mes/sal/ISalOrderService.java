package com.ruoyi.system.service.mes.sal;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrderToWorkorderRequest;

/**
 * 销售订单Service接口
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public interface ISalOrderService
{
    public SalOrder selectSalOrderByOrderId(Long orderId);
    public List<SalOrder> selectSalOrderList(SalOrder salOrder);
    /** 工单选择器用:已确认可转单的订单 */
    public List<SalOrder> selectAllConvertible();
    public boolean checkOrderCodeUnique(SalOrder salOrder);

    /** 创建订单(头+明细行,一个事务) */
    public SalOrder createWithLines(SalOrderCreateRequest req);
    /** 修改订单(头+全量替换行,仅 PREPARE 状态可改明细) */
    public SalOrder updateWithLines(SalOrderCreateRequest req);
    /** 详情(头+行,行带已转量/可转量) */
    public SalOrder getDetail(Long orderId);

    /** 确认:PREPARE->CONFIRMED */
    public int confirmOrder(Long orderId);
    /** 关闭:CONFIRMED->CLOSED */
    public int closeOrder(Long orderId);
    /** 取消:PREPARE/CONFIRMED->CANCEL */
    public int cancelOrder(Long orderId);

    public int deleteSalOrderByOrderIds(Long[] orderIds);

    /** 销售订单明细行 -> 一键转工单(先锁后事务,防并发超转) */
    public ProWorkorder toWorkorder(SalOrderToWorkorderRequest req);
}
