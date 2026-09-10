package com.ruoyi.system.mapper.mes.sal;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.vo.SalOrderProgressRow;

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
    /** 工单选择器用:查已确认/生产中两态（CONFIRMED/PRODUCING）可转订单 */
    public List<SalOrder> selectSalOrderAllConvertible();
    public int insertSalOrder(SalOrder salOrder);
    public int updateSalOrder(SalOrder salOrder);
    public int deleteSalOrderByOrderId(Long orderId);
    public int deleteSalOrderByOrderIds(Long[] orderIds);

    /**
     * 批量聚合订单生产进度（任务数量口径，排除已取消工单/任务）。
     * 主表 l 的 factory_id 由 FactoryIdInterceptor 注入。
     */
    public List<SalOrderProgressRow> selectProgressByOrderIds(@Param("ids") List<Long> orderIds);

    /** 仅 CONFIRMED 订单推进 PRODUCING（开工事件）。跳过拦截器：跨表事件链路显式带 factoryId */
    @SkipFactoryId
    int confirmProducing(@Param("orderId") Long orderId,
                         @Param("factoryId") Long factoryId,
                         @Param("updateBy") String updateBy,
                         @Param("updateTime") Date updateTime);
}
