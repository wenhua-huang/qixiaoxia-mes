package com.ruoyi.system.mapper.mes.pro;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProCard;
import org.apache.ibatis.annotations.Param;

public interface ProCardMapper {
    ProCard selectProCardByCardId(Long cardId);
    /** 行锁查询：SELECT ... FOR UPDATE，用于末次完工并发判定 */
    ProCard selectProCardByCardIdForUpdate(Long cardId);
    List<ProCard> selectProCardList(ProCard e);
    int insertProCard(ProCard e);
    int updateProCard(ProCard e);
    int deleteProCardByCardId(Long cardId);
    int deleteProCardByCardIds(Long[] cardIds);
    /** 拆卡原子扣减：仅在 status=ACTIVE 且余量充足时扣减，返回影响行数（0=不满足条件） */
    int decrementQuantity(@Param("cardId") Long cardId, @Param("delta") BigDecimal delta, @Param("factoryId") Long factoryId);

    /**
     * 条件推进流转卡：仅当卡当前状态 = expectedStatus 时更新，防并发收货/报工丢失更新。
     * 厂内报工 expectedStatus=ACTIVE；外协收货 expectedStatus=OUTSOURCING。
     * @return 影响行数；0 表示卡已被推进（幂等）或状态不符
     */
    int advanceCard(@Param("cardId") Long cardId,
                    @Param("currentProcessId") Long currentProcessId,
                    @Param("currentProcessName") String currentProcessName,
                    @Param("status") String status,
                    @Param("expectedStatus") String expectedStatus,
                    @Param("operator") String operator);

    /**
     * 发料外协时标记卡为外协中：仅 ACTIVE → OUTSOURCING，防止回退 COMPLETED/CANCELLED 卡，
     * 也防止同卡不同工序并发外协单互相覆盖 currentProcessId。
     * @return 影响行数；0 表示卡非 ACTIVE（已外协/已完工等），调用方应跳过而非覆盖
     */
    int markOutsourcingIfActive(@Param("cardId") Long cardId,
                                @Param("currentProcessId") Long currentProcessId,
                                @Param("currentProcessName") String currentProcessName,
                                @Param("operator") String operator);
}
