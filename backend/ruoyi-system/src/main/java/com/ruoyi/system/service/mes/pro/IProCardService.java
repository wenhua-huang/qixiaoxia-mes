package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProCard;

/**
 * ProCardService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProCardService
{
    public ProCard selectProCardByCardId(Long cardId);
    public List<ProCard> selectProCardList(ProCard e);
    public List<ProCard> selectAll();
    public int insertProCard(ProCard e);
    public int updateProCard(ProCard e);
    public int deleteProCardByCardIds(Long[] cardIds);
    public int deleteProCardByCardId(Long cardId);
    /** 拆卡：从原卡拆出 splitQty 生成新卡，原卡数量原子扣减，单事务保证一致性 */
    public ProCard splitCard(Long cardId, java.math.BigDecimal splitQty);
}
