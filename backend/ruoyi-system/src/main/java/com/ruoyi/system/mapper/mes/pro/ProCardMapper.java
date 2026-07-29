package com.ruoyi.system.mapper.mes.pro;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProCard;
import org.apache.ibatis.annotations.Param;

public interface ProCardMapper {
    ProCard selectProCardByCardId(Long cardId);
    List<ProCard> selectProCardList(ProCard e);
    int insertProCard(ProCard e);
    int updateProCard(ProCard e);
    int deleteProCardByCardId(Long cardId);
    int deleteProCardByCardIds(Long[] cardIds);
    /** 拆卡原子扣减：仅在 status=ACTIVE 且余量充足时扣减，返回影响行数（0=不满足条件） */
    int decrementQuantity(@Param("cardId") Long cardId, @Param("delta") BigDecimal delta, @Param("factoryId") Long factoryId);
}
