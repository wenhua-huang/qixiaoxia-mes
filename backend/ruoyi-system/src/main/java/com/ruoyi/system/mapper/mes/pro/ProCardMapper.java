package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProCard;

public interface ProCardMapper {
    ProCard selectProCardByCardId(Long cardId);
    List<ProCard> selectProCardList(ProCard e);
    int insertProCard(ProCard e);
    int updateProCard(ProCard e);
    int deleteProCardByCardId(Long cardId);
    int deleteProCardByCardIds(Long[] cardIds);
}
