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

    /**
     * 回填入库单头的 IPQC 完工检挂点（两列专用 UPDATE，避免并发覆盖整头）
     *
     * @param recptId  入库单ID
     * @param ipqcId   过程检验单ID
     * @param ipqcCode 过程检验单编码
     */
    public int updateProductRecptHeaderRefs(@Param("recptId") Long recptId,
                                            @Param("ipqcId") Long ipqcId,
                                            @Param("ipqcCode") String ipqcCode);
}
