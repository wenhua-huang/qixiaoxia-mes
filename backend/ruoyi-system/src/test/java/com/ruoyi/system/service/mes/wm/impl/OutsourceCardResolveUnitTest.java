package com.ruoyi.system.service.mes.wm.impl;

import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 外协单缺卡关联回补（resolveOrderCardIfAbsent）单元测试。
 *
 * <p>背景：外协单创建早于开工建卡时 order.cardId 为空，发料标记与收货推进都会被跳过；
 * 收货时按工单回补 ACTIVE 卡后再走标记+推进（见 doReceive）。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
class OutsourceCardResolveUnitTest {

    @Mock private ProCardMapper cardMapper;

    @InjectMocks private OutsourceServiceImpl service;

    private WmOutsourceOrder order(Long cardId, Long workorderId) {
        WmOutsourceOrder o = new WmOutsourceOrder();
        o.setOrderId(94L);
        o.setCardId(cardId);
        o.setWorkorderId(workorderId);
        return o;
    }

    private ProCard card(Long cardId, String status) {
        ProCard c = new ProCard();
        c.setCardId(cardId);
        c.setStatus(status);
        return c;
    }

    @Test
    void nullCardId_picksActiveCardOfWorkorder() {
        when(cardMapper.selectProCardList(any())).thenReturn(List.of(
                card(268L, "OUTSOURCING"), card(269L, "ACTIVE"), card(270L, "COMPLETED")));
        WmOutsourceOrder o = order(null, 387L);
        service.resolveOrderCardIfAbsent(o);
        assertThat(o.getCardId()).isEqualTo(269L);
    }

    @Test
    void cardIdAlreadySet_noLookup() {
        WmOutsourceOrder o = order(269L, 387L);
        service.resolveOrderCardIfAbsent(o);
        assertThat(o.getCardId()).isEqualTo(269L);
        verifyNoInteractions(cardMapper);
    }

    @Test
    void noCards_staysNull() {
        when(cardMapper.selectProCardList(any())).thenReturn(Collections.emptyList());
        WmOutsourceOrder o = order(null, 387L);
        service.resolveOrderCardIfAbsent(o);
        assertThat(o.getCardId()).isNull();
    }

    @Test
    void onlyNonActiveCards_staysNull() {
        // 其它外协单占用（OUTSOURCING）或已完结的卡不可回补，避免推进别人的卡
        when(cardMapper.selectProCardList(any())).thenReturn(List.of(
                card(268L, "OUTSOURCING"), card(270L, "COMPLETED")));
        WmOutsourceOrder o = order(null, 387L);
        service.resolveOrderCardIfAbsent(o);
        assertThat(o.getCardId()).isNull();
    }

    @Test
    void nullWorkorder_staysNull_noLookup() {
        WmOutsourceOrder o = order(null, null);
        service.resolveOrderCardIfAbsent(o);
        assertThat(o.getCardId()).isNull();
        verifyNoInteractions(cardMapper);
    }
}
