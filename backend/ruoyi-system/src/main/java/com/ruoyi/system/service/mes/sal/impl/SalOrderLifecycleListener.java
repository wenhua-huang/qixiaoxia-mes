package com.ruoyi.system.service.mes.sal.impl;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.event.mes.WorkorderStartedEvent;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.mapper.mes.sal.SalOrderMapper;

/**
 * 销售订单生命周期事件监听：跨域状态推进唯一入口。
 * AFTER_COMMIT：主业务回滚则订单绝不动；REQUIRES_NEW：推进独立提交，失败仅记日志（条件 UPDATE 幂等可补偿）。
 */
@Component
public class SalOrderLifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(SalOrderLifecycleListener.class);

    @Autowired private SalOrderMapper salOrderMapper;
    @Autowired private SalOrderLineMapper salOrderLineMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onWorkorderStarted(WorkorderStartedEvent e) {
        if (e.getSalesOrderLineId() == null) return;
        SalOrderLine line = salOrderLineMapper.selectSalOrderLineByLineId(e.getSalesOrderLineId());
        if (line == null || line.getOrderId() == null) return;
        int rows = salOrderMapper.confirmProducing(line.getOrderId(), e.getFactoryId(), currentUser(), DateUtils.getNowDate());
        log.info("开工事件推进订单生产中: orderId={}, workorderId={}, affected={}", line.getOrderId(), e.getWorkorderId(), rows);
    }

    private String currentUser() {
        try { return SecurityUtils.getUsername(); } catch (Exception ex) { return "system"; }
    }
}
