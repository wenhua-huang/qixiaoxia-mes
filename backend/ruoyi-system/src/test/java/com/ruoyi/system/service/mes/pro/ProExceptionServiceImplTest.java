package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.ProExceptionParty;
import com.ruoyi.common.enums.ProExceptionResolve;
import com.ruoyi.common.enums.ProExceptionStatus;
import com.ruoyi.common.enums.ProExceptionType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProException;
import com.ruoyi.system.domain.mes.pro.ProExceptionConstants;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.pro.ProExceptionMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.service.mes.pro.impl.ProExceptionServiceImpl;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 生产异常服务单元测试：上报/定责锁定/七出口流转/关闭。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("生产异常服务")
class ProExceptionServiceImplTest {

    @Mock private ProExceptionMapper proExceptionMapper;
    @Mock private ProTaskMapper proTaskMapper;
    @Mock private IProTaskService proTaskService;
    @Mock private IPurOrderService purOrderService;
    @Mock private IPurOrderLineService purOrderLineService;
    @Mock private MdItemMapper mdItemMapper;
    @Mock private AutoCodeGenerator autoCodeGenerator;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private ProExceptionServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtils;

    private static final Long EX_ID = 100L;
    private static final Long TASK_ID = 20L;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");
        securityUtils.when(SecurityUtils::getUserId).thenReturn(1L);

        // 锁直接执行动作；事务模板走 mock 事务管理器（@PostConstruct 在 Mockito 下不触发）
        lenient().doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(lockTemplate).execute(anyString(), any(Runnable.class));
        // 带返回值的锁重载（补全/关闭/作废）直接执行 Supplier
        lenient().doAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get())
                .when(lockTemplate).execute(anyString(), any(Supplier.class));
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service, "txTemplate", new TransactionTemplate(transactionManager));

        lenient().when(autoCodeGenerator.genSerialCode(anyString(), any()))
                .thenReturn("EX20260923-001");
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    // ---------------- E6 上报 ----------------

    @Test
    @DisplayName("上报：回填任务快照，状态待处理/责任方待定，自动编码")
    void should_snapshot_task_when_report() {
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(originTask());
        doAnswer(inv -> {
            inv.getArgument(0, ProException.class).setExceptionId(77L);
            return 1;
        }).when(proExceptionMapper).insertProException(any(ProException.class));
        ProException input = new ProException();
        input.setTaskId(TASK_ID);
        input.setExceptionType(ProExceptionType.QUALITY.getCode());
        input.setImpactQuantity(new BigDecimal("5"));
        input.setDescription(" 做坏了一批 ");

        Long id = service.reportException(input);

        ArgumentCaptor<ProException> cap = ArgumentCaptor.forClass(ProException.class);
        verify(proExceptionMapper).insertProException(cap.capture());
        ProException saved = cap.getValue();
        assertThat(id).isEqualTo(77L);
        assertThat(saved.getExceptionCode()).isEqualTo("EX20260923-001");
        assertThat(saved.getStatus()).isEqualTo(ProExceptionStatus.OPEN.getCode());
        assertThat(saved.getResponsibleParty()).isEqualTo(ProExceptionParty.PENDING.getCode());
        assertThat(saved.getWorkorderId()).isEqualTo(10L);
        assertThat(saved.getWorkorderCode()).isEqualTo("WO-1");
        assertThat(saved.getTaskCode()).isEqualTo("T1");
        assertThat(saved.getProcessId()).isEqualTo(8L);
        assertThat(saved.getTargetType()).isEqualTo(ProExceptionConstants.TARGET_TYPE_TASK);
        assertThat(saved.getReporterId()).isEqualTo(1L);
        assertThat(saved.getOccurTime()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("做坏了一批");
    }

    @Test
    @DisplayName("上报：类型非法/缺任务/缺说明均拒绝")
    void should_reject_invalid_report() {
        ProException noType = new ProException();
        noType.setTaskId(TASK_ID);
        noType.setDescription("x");
        assertThatThrownBy(() -> service.reportException(noType))
                .isInstanceOf(ServiceException.class).hasMessageContaining("异常类型");

        ProException noTask = new ProException();
        noTask.setExceptionType(ProExceptionType.DELAY.getCode());
        noTask.setDescription("x");
        assertThatThrownBy(() -> service.reportException(noTask))
                .isInstanceOf(ServiceException.class).hasMessageContaining("任务");

        ProException noDesc = new ProException();
        noDesc.setTaskId(TASK_ID);
        noDesc.setExceptionType(ProExceptionType.QUALITY.getCode());
        // 空说明在任务快照装载之前即被拦截
        assertThatThrownBy(() -> service.reportException(noDesc))
                .isInstanceOf(ServiceException.class).hasMessageContaining("异常说明");
    }

    // ---------------- E1/E2 补全 ----------------

    @Test
    @DisplayName("补全：责任方首次选定成功，再次改派被拒（锁定）")
    void should_lock_responsible_party_after_first_choice() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.PENDING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        ProException first = new ProException();
        first.setExceptionId(EX_ID);
        first.setResponsibleParty(ProExceptionParty.FACTORY.getCode());
        first.setQualitySubclass("BROKEN");
        service.updateProException(first);
        assertThat(ex.getResponsibleParty()).isEqualTo(ProExceptionParty.FACTORY.getCode());

        ProException second = new ProException();
        second.setExceptionId(EX_ID);
        second.setResponsibleParty(ProExceptionParty.SUPPLIER.getCode());
        second.setQualitySubclass("BROKEN");
        assertThatThrownBy(() -> service.updateProException(second))
                .isInstanceOf(ServiceException.class).hasMessageContaining("责任方已确定");
    }

    @Test
    @DisplayName("补全：质量异常必须选细分；异常类型不可变；非待处理不可改")
    void should_validate_edit_rules() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        ProException noSub = new ProException();
        noSub.setExceptionId(EX_ID);
        assertThatThrownBy(() -> service.updateProException(noSub))
                .isInstanceOf(ServiceException.class).hasMessageContaining("细分");

        ProException changedType = new ProException();
        changedType.setExceptionId(EX_ID);
        changedType.setExceptionType(ProExceptionType.DELAY.getCode());
        assertThatThrownBy(() -> service.updateProException(changedType))
                .isInstanceOf(ServiceException.class).hasMessageContaining("类型不可修改");

        ex.setStatus(ProExceptionStatus.PROCESSING.getCode());
        ProException any = new ProException();
        any.setExceptionId(EX_ID);
        assertThatThrownBy(() -> service.updateProException(any))
                .isInstanceOf(ServiceException.class).hasMessageContaining("待处理");
    }

    @Test
    @DisplayName("补全：延迟类型三要素齐全则通过")
    void should_accept_complete_delay_fields() {
        ProException ex = openException(ProExceptionType.DELAY, ProExceptionParty.CUSTOMER.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        ProException input = new ProException();
        input.setExceptionId(EX_ID);
        input.setOriginalPlanTime(new Date());
        input.setNewExpectedTime(new Date(System.currentTimeMillis() + 86_400_000L));
        input.setDelayReason("客户确认晚了");
        service.updateProException(input);

        assertThat(ex.getDelayReason()).isEqualTo("客户确认晚了");
        verify(proExceptionMapper).updateProException(ex);
    }

    // ---------------- E3 七出口 ----------------

    @Test
    @DisplayName("返工出口：建 -E1 异常任务，异常单置处理中并挂链")
    void should_create_rework_task_e1() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(originTask());
        when(proExceptionMapper.countExceptionTasksByOrigin(TASK_ID)).thenReturn(0);

        ProException input = new ProException();
        input.setResolveType(ProExceptionResolve.REWORK.getCode());
        ProException result = service.resolveException(EX_ID, input);

        ArgumentCaptor<ProTask> cap = ArgumentCaptor.forClass(ProTask.class);
        verify(proTaskService).insertProTask(cap.capture());
        ProTask task = cap.getValue();
        assertThat(task.getTaskCode()).isEqualTo("T1-E1");
        assertThat(task.getTaskName()).contains("（返工）");
        assertThat(task.getProcessName()).contains("（返工）");
        assertThat(task.getQuantity()).isEqualByComparingTo("5");
        assertThat(task.getStatus()).isEqualTo(ProConstants.TASK_STATUS_NORMAL);
        assertThat(task.getIsException()).isEqualTo("Y");
        assertThat(task.getOriginTaskId()).isEqualTo(TASK_ID);
        assertThat(task.getExceptionId()).isEqualTo(EX_ID);
        assertThat(result.getStatus()).isEqualTo(ProExceptionStatus.PROCESSING.getCode());
        assertThat(result.getTargetDocCode()).isEqualTo("T1-E1");
        assertThat(result.getResolveType()).isEqualTo(ProExceptionResolve.REWORK.getCode());
    }

    @Test
    @DisplayName("第二次补做出口：编号 -E2，数量取弹窗输入，后缀补做")
    void should_create_remake_task_e2_with_input_qty() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(originTask());
        when(proExceptionMapper.countExceptionTasksByOrigin(TASK_ID)).thenReturn(1);

        ProException input = new ProException();
        input.setResolveType(ProExceptionResolve.REMAKE.getCode());
        input.setResolveQuantity(new BigDecimal("3"));
        service.resolveException(EX_ID, input);

        ArgumentCaptor<ProTask> cap = ArgumentCaptor.forClass(ProTask.class);
        verify(proTaskService).insertProTask(cap.capture());
        assertThat(cap.getValue().getTaskCode()).isEqualTo("T1-E2");
        assertThat(cap.getValue().getTaskName()).contains("（补做）");
        assertThat(cap.getValue().getQuantity()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("处理前置：责任方待定拒绝；非待处理拒绝重复出口")
    void should_reject_resolve_when_party_pending_or_not_open() {
        ProException pending = openException(ProExceptionType.QUALITY, ProExceptionParty.PENDING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(pending);
        assertThatThrownBy(() -> service.resolveException(EX_ID, resolveInput(ProExceptionResolve.REWORK)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("责任方");

        ProException processing = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        processing.setStatus(ProExceptionStatus.PROCESSING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(processing);
        assertThatThrownBy(() -> service.resolveException(EX_ID, resolveInput(ProExceptionResolve.REWORK)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("重复操作");
    }

    @Test
    @DisplayName("补料出口：建 DRAFT 采购单+一行缺料，异常单号双向挂链")
    void should_create_draft_purchase_order() {
        ProException ex = openException(ProExceptionType.MATERIAL, ProExceptionParty.SUPPLIER.getCode());
        ex.setItemName("铜版纸");
        ex.setShortageQuantity(new BigDecimal("8"));
        ex.setExpectedArrivalDate(new Date());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(originTask());
        doAnswer(inv -> {
            PurOrder po = inv.getArgument(0);
            po.setOrderId(9L);
            po.setOrderCode("PO-001");
            return 1;
        }).when(purOrderService).insertPurOrder(any(PurOrder.class));

        ProException result = service.resolveException(EX_ID, resolveInput(ProExceptionResolve.PURCHASE));

        ArgumentCaptor<PurOrder> poCap = ArgumentCaptor.forClass(PurOrder.class);
        verify(purOrderService).insertPurOrder(poCap.capture());
        PurOrder po = poCap.getValue();
        assertThat(po.getStatus()).isEqualTo("DRAFT");
        assertThat(po.getVendorId()).isZero();
        assertThat(po.getSourceOrderCode()).isEqualTo("EX-001");
        assertThat(po.getWorkorderId()).isEqualTo(10L);
        verify(purOrderLineService).insertPurOrderLine(any());
        verify(purOrderService).updatePurOrder(any(PurOrder.class));
        assertThat(result.getStatus()).isEqualTo(ProExceptionStatus.PROCESSING.getCode());
        assertThat(result.getTargetDocType()).isEqualTo(ProExceptionConstants.DOC_TYPE_PUR_ORDER);
        assertThat(result.getTargetDocCode()).isEqualTo("PO-001");
    }

    @Test
    @DisplayName("顺延出口：回写原计划时间并精准更新任务结束时间")
    void should_reschedule_task_end_time() {
        ProException ex = openException(ProExceptionType.DELAY, ProExceptionParty.CUSTOMER.getCode());
        Date newTime = new Date(System.currentTimeMillis() + 3 * 86_400_000L);
        ex.setNewExpectedTime(newTime);
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        ProTask task = originTask();
        Date oldEnd = new Date();
        task.setEndTime(oldEnd);
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(task);

        service.resolveException(EX_ID, resolveInput(ProExceptionResolve.RESCHEDULE));

        verify(proTaskMapper).updateTaskEndTime(TASK_ID, newTime, "admin");
        assertThat(ex.getOriginalPlanTime()).isEqualTo(oldEnd);
        assertThat(ex.getStatus()).isEqualTo(ProExceptionStatus.PROCESSING.getCode());
    }

    @Test
    @DisplayName("报废出口：报废数量必填，直接关闭")
    void should_scrap_and_close() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        assertThatThrownBy(() -> service.resolveException(EX_ID, resolveInput(ProExceptionResolve.SCRAP)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("报废数量");

        ProException withQty = resolveInput(ProExceptionResolve.SCRAP);
        withQty.setScrapQuantity(new BigDecimal("5"));
        ProException result = service.resolveException(EX_ID, withQty);
        assertThat(result.getStatus()).isEqualTo(ProExceptionStatus.CLOSED.getCode());
        assertThat(result.getScrapQuantity()).isEqualByComparingTo("5");
        assertThat(result.getCloseBy()).isEqualTo("admin");
        assertThat(result.getCloseTime()).isNotNull();
        verify(proTaskService, never()).insertProTask(any());
    }

    @Test
    @DisplayName("让步接收出口：结论必填，直接关闭")
    void should_concession_need_conclusion_and_close() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.SUPPLIER.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        assertThatThrownBy(() -> service.resolveException(EX_ID, resolveInput(ProExceptionResolve.CONCESSION)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("让步接收结论");

        ProException withConclusion = resolveInput(ProExceptionResolve.CONCESSION);
        withConclusion.setConclusion("特采放行");
        ProException result = service.resolveException(EX_ID, withConclusion);
        assertThat(result.getStatus()).isEqualTo(ProExceptionStatus.CLOSED.getCode());
        assertThat(result.getConclusion()).isEqualTo("特采放行");
    }

    // ---------------- 手动收口 ----------------

    @Test
    @DisplayName("关闭：处理中+结论 → 已关闭；其他状态/空结论拒绝")
    void should_close_only_processing_with_conclusion() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        ex.setStatus(ProExceptionStatus.PROCESSING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        assertThatThrownBy(() -> service.closeException(EX_ID, "  "))
                .isInstanceOf(ServiceException.class).hasMessageContaining("处理结论");

        // 单字结论后端兜底拒绝（前端 prompt 校验不能绕过）
        assertThatThrownBy(() -> service.closeException(EX_ID, "x"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("至少");

        service.closeException(EX_ID, "返工完成复检合格");
        assertThat(ex.getStatus()).isEqualTo(ProExceptionStatus.CLOSED.getCode());
        assertThat(ex.getConclusion()).isEqualTo("返工完成复检合格");
        assertThat(ex.getCloseBy()).isEqualTo("admin");

        ProException open = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(open);
        assertThatThrownBy(() -> service.closeException(EX_ID, "x"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("处理中");
    }

    // ---------------- E1 作废 ----------------

    @Test
    @DisplayName("作废：待处理+原因 → 已作废终态，原因入结论，留关闭人/时间")
    void should_void_open_exception_with_reason() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.PENDING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        service.voidException(EX_ID, " 挂错任务了 ");

        assertThat(ex.getStatus()).isEqualTo(ProExceptionStatus.VOID.getCode());
        assertThat(ex.getConclusion()).isEqualTo(ProExceptionConstants.VOID_CONCLUSION_PREFIX + "挂错任务了");
        assertThat(ex.getCloseBy()).isEqualTo("admin");
        assertThat(ex.getCloseTime()).isNotNull();
        verify(proExceptionMapper).updateProException(ex);
    }

    @Test
    @DisplayName("作废：空原因拒绝；非待处理（处理中/已关闭/已作废）拒绝")
    void should_reject_void_when_blank_reason_or_not_open() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.PENDING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        assertThatThrownBy(() -> service.voidException(EX_ID, " "))
                .isInstanceOf(ServiceException.class).hasMessageContaining("作废原因");

        ex.setStatus(ProExceptionStatus.PROCESSING.getCode());
        assertThatThrownBy(() -> service.voidException(EX_ID, "x"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("待处理");

        ex.setStatus(ProExceptionStatus.CLOSED.getCode());
        assertThatThrownBy(() -> service.voidException(EX_ID, "x"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("待处理");
    }

    @Test
    @DisplayName("结论字数口径统一：关闭/作废/终结出口都拒绝单字与超长（防绕前端打出 Data too long）")
    void should_reject_conclusion_out_of_length_bounds() {
        // 关闭：超长拒绝
        ProException processing = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        processing.setStatus(ProExceptionStatus.PROCESSING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(processing);
        assertThatThrownBy(() -> service.closeException(EX_ID, "x".repeat(ProExceptionConstants.MAX_CONCLUSION_LEN + 1)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不能超过");

        // 作废：待处理下单字拒绝、超长拒绝（前缀占 4 字，原因上限 996）
        ProException openVoid = openException(ProExceptionType.QUALITY, ProExceptionParty.PENDING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(openVoid);
        assertThatThrownBy(() -> service.voidException(EX_ID, "x"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("至少");
        assertThatThrownBy(() -> service.voidException(EX_ID, "y".repeat(ProExceptionConstants.MAX_VOID_REASON_LEN + 1)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("不能超过");

        // 让步接收（终结出口）：单字拒绝，口径与手动关闭一致
        ProException openConc = openException(ProExceptionType.QUALITY, ProExceptionParty.SUPPLIER.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(openConc);
        ProException oneChar = resolveInput(ProExceptionResolve.CONCESSION);
        oneChar.setConclusion("行");
        assertThatThrownBy(() -> service.resolveException(EX_ID, oneChar))
                .isInstanceOf(ServiceException.class).hasMessageContaining("至少");
    }

    @Test
    @DisplayName("并发防护：作废走异常单维度同一把锁，与补全/关闭/选出口互斥")
    void should_lock_by_exception_when_void() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.PENDING.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        service.voidException(EX_ID, "挂错任务");

        verify(lockTemplate).execute(eq(ProExceptionConstants.LOCK_PREFIX + EX_ID),
                any(Supplier.class));
    }

    @Test
    @DisplayName("并发防护：开返工任务按原任务加 -En 序号锁，防跨异常单并发重号")
    void should_lock_origin_task_when_create_exception_task() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(originTask());
        when(proExceptionMapper.countExceptionTasksByOrigin(TASK_ID)).thenReturn(0);

        service.resolveException(EX_ID, resolveInput(ProExceptionResolve.REWORK));

        // 序号锁改为 Runnable 且必须在事务外获取：先异常单锁、后原任务锁，顺序固定防死锁
        InOrder locks = inOrder(lockTemplate);
        locks.verify(lockTemplate).execute(eq(ProExceptionConstants.LOCK_PREFIX + EX_ID),
                any(Runnable.class));
        locks.verify(lockTemplate).execute(eq(ProExceptionConstants.LOCK_TASK_PREFIX + TASK_ID),
                any(Runnable.class));
    }

    @Test
    @DisplayName("补全编辑：清空现场照片/单选标记可落库（专用置空 UPDATE 收到 null），并走异常单锁")
    void should_clear_scene_images_on_edit() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        ex.setQualitySubclass("BROKEN");
        ex.setSceneImages("http://minio/x.png");
        ex.setNeedRework("Y");
        ex.setCustomerAcceptRework("N");
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);

        ProException input = new ProException();
        input.setExceptionId(EX_ID);
        input.setQualitySubclass("BROKEN");
        input.setSceneImages("   ");
        input.setNeedRework("  ");
        input.setCustomerAcceptRework(" ");
        service.updateProException(input);

        ArgumentCaptor<ProException> cap = ArgumentCaptor.forClass(ProException.class);
        verify(proExceptionMapper).updateExceptionEditOptional(cap.capture());
        assertThat(cap.getValue().getSceneImages()).isNull();
        assertThat(cap.getValue().getNeedRework()).isNull();
        assertThat(cap.getValue().getCustomerAcceptRework()).isNull();
        verify(lockTemplate).execute(eq(ProExceptionConstants.LOCK_PREFIX + EX_ID),
                any(Supplier.class));
    }

    @Test
    @DisplayName("上报/补全：现场照片超过 9 张或超长被后端拒绝")
    void should_reject_too_many_or_too_long_scene_images() {
        // 上报路径：10 个 URL
        ProException report = new ProException();
        report.setTaskId(TASK_ID);
        report.setExceptionType(ProExceptionType.QUALITY.getCode());
        report.setDescription("x");
        report.setSceneImages("u1,u2,u3,u4,u5,u6,u7,u8,u9,u10");
        assertThatThrownBy(() -> service.reportException(report))
                .isInstanceOf(ServiceException.class).hasMessageContaining("9 张");

        // 补全路径：总长超 2000
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        ProException input = new ProException();
        input.setExceptionId(EX_ID);
        input.setQualitySubclass("BROKEN");
        input.setSceneImages("u".repeat(2001));
        assertThatThrownBy(() -> service.updateProException(input))
                .isInstanceOf(ServiceException.class).hasMessageContaining("超长");
    }

    @Test
    @DisplayName("现场照片：串内空段/段内空白落库前归一化（a,, ,b → a,b）")
    void should_normalize_scene_images_segments() {
        when(proTaskMapper.selectProTaskByTaskId(TASK_ID)).thenReturn(originTask());
        ArgumentCaptor<ProException> cap = ArgumentCaptor.forClass(ProException.class);
        ProException input = new ProException();
        input.setTaskId(TASK_ID);
        input.setExceptionType(ProExceptionType.QUALITY.getCode());
        input.setDescription("x");
        input.setSceneImages(" http://minio/a.png ,,  , http://minio/b.png , ");

        service.reportException(input);

        verify(proExceptionMapper).insertProException(cap.capture());
        assertThat(cap.getValue().getSceneImages()).isEqualTo("http://minio/a.png,http://minio/b.png");
    }

    // ---------------- E3/E4 详情聚合处理单据状态 ----------------

    @Test
    @DisplayName("详情：处理单据是异常任务时回填任务状态")
    void should_fill_target_task_status() {
        ProException ex = openException(ProExceptionType.QUALITY, ProExceptionParty.FACTORY.getCode());
        ex.setTargetDocType(ProExceptionConstants.DOC_TYPE_TASK);
        ex.setTargetDocId(99L);
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        ProTask doc = new ProTask();
        doc.setStatus("COMPLETED");
        when(proTaskMapper.selectProTaskByTaskId(99L)).thenReturn(doc);

        ProException detail = service.selectProExceptionByExceptionId(EX_ID);

        assertThat(detail.getTargetDocStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("详情：处理单据是补料采购单时回填采购单状态")
    void should_fill_target_purchase_status() {
        ProException ex = openException(ProExceptionType.MATERIAL, ProExceptionParty.SUPPLIER.getCode());
        ex.setTargetDocType(ProExceptionConstants.DOC_TYPE_PUR_ORDER);
        ex.setTargetDocId(9L);
        when(proExceptionMapper.selectProExceptionByExceptionId(EX_ID)).thenReturn(ex);
        PurOrderVO po = new PurOrderVO();
        po.setStatus("ORDERED");
        when(purOrderService.selectPurOrderByOrderId(9L)).thenReturn(po);

        ProException detail = service.selectProExceptionByExceptionId(EX_ID);

        assertThat(detail.getTargetDocStatus()).isEqualTo("ORDERED");
    }

    // ---------------- fixtures ----------------

    private ProException resolveInput(ProExceptionResolve resolve) {
        ProException input = new ProException();
        input.setResolveType(resolve.getCode());
        return input;
    }

    private ProException openException(ProExceptionType type, String party) {
        ProException ex = new ProException();
        ex.setExceptionId(EX_ID);
        ex.setExceptionCode("EX-001");
        ex.setExceptionType(type.getCode());
        ex.setStatus(ProExceptionStatus.OPEN.getCode());
        ex.setResponsibleParty(party);
        ex.setWorkorderId(10L);
        ex.setWorkorderCode("WO-1");
        ex.setWorkorderName("测试工单");
        ex.setTaskId(TASK_ID);
        ex.setTaskCode("T1");
        ex.setProcessId(8L);
        ex.setProcessCode("P8");
        ex.setProcessName("糊盒");
        ex.setImpactQuantity(new BigDecimal("5"));
        return ex;
    }

    private ProTask originTask() {
        ProTask t = new ProTask();
        t.setTaskId(TASK_ID);
        t.setTaskCode("T1");
        t.setTaskName("盒子【100】个");
        t.setWorkorderId(10L);
        t.setWorkorderCode("WO-1");
        t.setWorkorderName("测试工单");
        t.setRouteId(3L);
        t.setProcessId(8L);
        t.setProcessCode("P8");
        t.setProcessName("糊盒");
        t.setUnitOfMeasure("PCS");
        t.setUnitName("个");
        t.setQuantity(new BigDecimal("100"));
        t.setStatus("PRODUCING");
        return t;
    }
}
