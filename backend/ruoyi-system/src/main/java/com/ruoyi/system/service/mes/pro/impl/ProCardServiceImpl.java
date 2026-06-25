package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.service.mes.pro.IProCardService;

/**
 * ProCardService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProCardServiceImpl implements IProCardService
{
    @Autowired
    private ProCardMapper proCardMapper;

    @Override
    public ProCard selectProCardByCardId(Long cardId) { return proCardMapper.selectProCardByCardId(cardId); }

    @Override
    public List<ProCard> selectProCardList(ProCard e) { return proCardMapper.selectProCardList(e); }

    @Override
    public List<ProCard> selectAll() { return proCardMapper.selectProCardList(new ProCard()); }

    @Override
    @Transactional
    public int insertProCard(ProCard e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        return proCardMapper.insertProCard(e);
    }

    @Override
    public int updateProCard(ProCard e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proCardMapper.updateProCard(e);
    }

    @Override
    public int deleteProCardByCardIds(Long[] cardIds) { return proCardMapper.deleteProCardByCardIds(cardIds); }

    @Override
    public int deleteProCardByCardId(Long cardId) { return proCardMapper.deleteProCardByCardId(cardId); }
}
