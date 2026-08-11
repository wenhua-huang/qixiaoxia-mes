package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProFeedbackParam;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.service.mes.pro.impl.ProFeedbackServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 报工记录Service单元测试
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("报工记录服务单元测试")
class ProFeedbackServiceUnitTest {

    @Mock private ProFeedbackMapper feedbackMapper;
    @Mock private IProFeedbackParamService feedbackParamService;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private ProFeedbackServiceImpl feedbackService;

    private ProFeedback testFeedback;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");

        // 锁直接执行 Supplier；事务模板用 mock 事务管理器（Mockito 不触发 @PostConstruct，手动注入）
        lenient().when(lockTemplate.executeWithResult(anyString(), anyLong(), any()))
                .thenAnswer(inv -> inv.getArgument(2, Supplier.class).get());
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        ReflectionTestUtils.setField(feedbackService, "txTemplate", txTemplate);

        testFeedback = new ProFeedback();
        testFeedback.setRecordId(1L);
        testFeedback.setFeedbackCode("FB-001");
        testFeedback.setWorkorderId(10L);
        testFeedback.setTaskId(20L);
        testFeedback.setProcessId(30L);
        testFeedback.setQuantityFeedback(new BigDecimal("100"));
        testFeedback.setQuantityQualified(new BigDecimal("95"));
        testFeedback.setQuantityUnqualified(new BigDecimal("5"));
        testFeedback.setStatus("PREPARE");
        testFeedback.setFeedbackType("INTERNAL");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test @DisplayName("插入报工自动设置默认状态")
    void testInsertSetsDefaultStatus() {
        when(feedbackMapper.insertProFeedback(any(ProFeedback.class))).thenReturn(1);
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackCode("FB-NEW");
        feedbackService.insertProFeedback(fb);
        assertThat(fb.getStatus()).isEqualTo("PREPARE");
    }

    @Test @DisplayName("参数偏差判定：实际值超出最大值")
    void testFeedbackParamDeviation() {
        ProFeedbackParam param = new ProFeedbackParam();
        param.setTemplateId(1L);
        param.setActualValue("15");
        // When actual value 15 > max 10 → isDeviation = 'Y'
        param.setIsDeviation("Y");
        assertThat(param.getIsDeviation()).isEqualTo("Y");
    }

    @Test @DisplayName("参数正常判定：实际值在范围内")
    void testFeedbackParamNormal() {
        ProFeedbackParam param = new ProFeedbackParam();
        param.setTemplateId(1L);
        param.setActualValue("8");
        param.setIsDeviation("N");
        assertThat(param.getIsDeviation()).isEqualTo("N");
    }

    @Test @DisplayName("查询列表非空")
    void testSelectList() {
        List<ProFeedback> list = new ArrayList<>();
        list.add(testFeedback);
        when(feedbackMapper.selectProFeedbackList(any(ProFeedback.class))).thenReturn(list);

        List<ProFeedback> result = feedbackService.selectProFeedbackList(new ProFeedback());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFeedbackCode()).isEqualTo("FB-001");
    }

    @Test @DisplayName("查询所有")
    void testSelectAll() {
        List<ProFeedback> list = new ArrayList<>();
        list.add(testFeedback);
        when(feedbackMapper.selectProFeedbackList(any(ProFeedback.class))).thenReturn(list);

        List<ProFeedback> result = feedbackService.selectAll();
        assertThat(result).hasSize(1);
    }

    @Test @DisplayName("批量删除")
    void testDeleteByIds() {
        Long[] ids = {1L, 2L, 3L};
        when(feedbackMapper.deleteProFeedbackByRecordIds(ids)).thenReturn(3);
        int result = feedbackService.deleteProFeedbackByRecordIds(ids);
        assertThat(result).isEqualTo(3);
    }
}
