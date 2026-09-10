package com.ruoyi.system.service.mes.sal;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.event.mes.WorkorderStartedEvent;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;
import com.ruoyi.system.service.mes.sal.impl.SalOrderLifecycleListener;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalOrderLifecycleListenerTest {
    @Mock SalOrderMapper salOrderMapper;
    @Mock SalOrderLineMapper salOrderLineMapper;
    @InjectMocks SalOrderLifecycleListener listener;

    @Test
    void started_confirmed_order_becomes_producing() {
        SalOrderLine line = new SalOrderLine();
        line.setLineId(10L); line.setOrderId(1L);
        when(salOrderLineMapper.selectSalOrderLineByLineId(10L)).thenReturn(line);
        listener.onWorkorderStarted(new WorkorderStartedEvent(99L, 10L, 1L));
        verify(salOrderMapper).confirmProducing(eq(1L), eq(1L), anyString(), any());
    }

    @Test
    void started_without_sales_line_is_ignored() {
        listener.onWorkorderStarted(new WorkorderStartedEvent(99L, null, 1L));
        verifyNoInteractions(salOrderMapper);
    }

    @Test
    void started_unknown_line_is_ignored() {
        when(salOrderLineMapper.selectSalOrderLineByLineId(404L)).thenReturn(null);
        listener.onWorkorderStarted(new WorkorderStartedEvent(99L, 404L, 1L));
        verify(salOrderMapper, never()).confirmProducing(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void listener_annotation_is_after_commit_requires_new() throws Exception {
        Method m = SalOrderLifecycleListener.class.getMethod("onWorkorderStarted", WorkorderStartedEvent.class);
        TransactionalEventListener a = m.getAnnotation(TransactionalEventListener.class);
        org.assertj.core.api.Assertions.assertThat(a.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        org.assertj.core.api.Assertions.assertThat(m.getAnnotation(Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }
}
