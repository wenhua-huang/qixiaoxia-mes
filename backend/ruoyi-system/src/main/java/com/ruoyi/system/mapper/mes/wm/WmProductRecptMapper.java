package com.ruoyi.system.mapper.mes.wm;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;

public interface WmProductRecptMapper
{
    public List<WmProductRecpt> selectWmProductRecptList(WmProductRecpt entity);
    public List<WmProductRecpt> selectWmProductRecptAll();
    public WmProductRecpt selectWmProductRecptByRecptId(Long recptId);
    public int insertWmProductRecpt(WmProductRecpt entity);
    public int updateWmProductRecpt(WmProductRecpt entity);
    public int deleteWmProductRecptByRecptId(Long recptId);
    public int deleteWmProductRecptByRecptIds(Long[] recptIds);

    /**
     * 汇总某工单已生成的入库单总量。
     * 语义：status IN ('DRAFT', 'CONFIRMED', 'POSTED')；status IS NULL 视为脏数据不计入。
     * 状态机：DRAFT → CONFIRMED (库存已扣) → POSTED (已过账)。
     * 用于手动补录时计算"未入库差额"。
     * factory_id 由 FactoryIdInterceptor 自动注入到 SQL。
     */
    public BigDecimal sumQuantityByWorkorderId(@Param("workorderId") Long workorderId);
}
