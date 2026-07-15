package com.ruoyi.system.mapper.mes.sal;

import java.util.List;
import com.ruoyi.system.domain.mes.sal.SalOrder;

/**
 * 销售订单Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public interface SalOrderMapper
{
    public SalOrder selectSalOrderByOrderId(Long orderId);
    public SalOrder selectSalOrderByOrderCode(String orderCode);
    public SalOrder checkOrderCodeUnique(SalOrder salOrder);
    public List<SalOrder> selectSalOrderList(SalOrder salOrder);
    /** 工单选择器用:查已确认且未关闭/取消的订单 */
    public List<SalOrder> selectSalOrderAllConvertible();
    public int insertSalOrder(SalOrder salOrder);
    public int updateSalOrder(SalOrder salOrder);
    public int deleteSalOrderByOrderId(Long orderId);
    public int deleteSalOrderByOrderIds(Long[] orderIds);
}
