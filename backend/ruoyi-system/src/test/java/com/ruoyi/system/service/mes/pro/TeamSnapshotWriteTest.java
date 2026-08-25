package com.ruoyi.system.service.mes.pro;

import java.util.function.Supplier;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;
import com.ruoyi.system.service.mes.pro.TeamResolver.TeamSnapshot;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackConsumeMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackParamMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.pro.ProParamTemplateMapper;
import com.ruoyi.system.mapper.mes.pro.ProProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkrecordMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueDetailMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueHeaderMapper;
import com.ruoyi.system.service.mes.pro.impl.ProFeedbackServiceImpl;
import com.ruoyi.system.service.mes.pro.impl.ProWorkrecordServiceImpl;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 班组快照写入单元测试：验证报工 insert / 上下工上工 时 team_id/team_code/team_name 被固化。
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("班组快照写入测试")
class TeamSnapshotWriteTest {

    // ── ProFeedbackServiceImpl 依赖（全量 @Mock，未使用的保持 lenient）──
    @Mock private ProFeedbackMapper feedbackMapper;
    @Mock private TeamResolver teamResolver;
    @Mock private ProFeedbackConsumeMapper consumeMapper;
    @Mock private ProFeedbackParamMapper feedbackParamMapper;
    @Mock private ProParamTemplateMapper proParamTemplateMapper;
    @Mock private ProTaskMapper proTaskMapper;
    @Mock private ProWorkorderMapper proWorkorderMapper;
    @Mock private ProWorkorderBomMapper proWorkorderBomMapper;
    @Mock private ProProcessMapper proProcessMapper;
    @Mock private ProRouteProcessMapper proRouteProcessMapper;
    @Mock private ProCardProcessMapper proCardProcessMapper;
    @Mock private ProCardMapper proCardMapper;
    @Mock private ProMaterialTraceMapper proMaterialTraceMapper;
    @Mock private MdItemMapper mdItemMapper;
    @Mock private WmIssueHeaderMapper wmIssueHeaderMapper;
    @Mock private WmIssueDetailMapper wmIssueDetailMapper;
    @Mock private IProWorkorderDocService proWorkorderDocService;
    @Mock private IQcFactoryService qcFactoryService;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private ProFeedbackServiceImpl feedbackService;

    // ── ProWorkrecordServiceImpl 依赖 ──
    @Mock private ProWorkrecordMapper workrecordMapper;
    @Mock private ProUserWorkstationMapper userWorkstationMapper;
    @Mock private MdWorkstationMapper mdWorkstationMapper;
    @Mock private ProTaskMapper workrecordTaskMapper;
    @InjectMocks private ProWorkrecordServiceImpl workrecordService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        LoginUser loginUser = new LoginUser();
        SysUser user = new SysUser();
        user.setNickName("管理员");
        loginUser.setUser(user);
        securityUtilsMock.when(SecurityUtils::getLoginUser).thenReturn(loginUser);

        // 事务模板：直接执行回调（@PostConstruct 不触发，手动注入）
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        ReflectionTestUtils.setField(feedbackService, "txTemplate", txTemplate);

        // 锁模板：直接执行 Supplier
        lenient().when(lockTemplate.executeWithResult(anyString(), anyLong(), any()))
                .thenAnswer(inv -> inv.getArgument(2, Supplier.class).get());
        lenient().doAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get())
                .when(lockTemplate).execute(anyString(), any(Supplier.class));
        lenient().doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(lockTemplate).execute(anyString(), any(Runnable.class));

        // 上工锁内：无在岗会话 + insert 成功
        lenient().when(workrecordMapper.selectActiveByUser(any(ProWorkrecord.class))).thenReturn(null);
        lenient().when(workrecordMapper.insertProWorkrecord(any(ProWorkrecord.class))).thenReturn(1);
        lenient().when(feedbackMapper.insertProFeedback(any(ProFeedback.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("报工 insert：teamResolver 返回快照时 team_id/code/name 被写入")
    void feedback_insert_fills_team_snapshot_by_userId() {
        when(teamResolver.resolveByUserId(1L))
                .thenReturn(new TeamSnapshot(10L, "DAY", "甲班"));

        ProFeedback fb = new ProFeedback();
        feedbackService.insertProFeedback(fb);

        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).insertProFeedback(cap.capture());
        ProFeedback saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTeamId()).isEqualTo(10L);
        assertThat(saved.getTeamCode()).isEqualTo("DAY");
        assertThat(saved.getTeamName()).isEqualTo("甲班");
    }

    @Test
    @DisplayName("报工 insert：userId 获取失败时按 userName 兜底反查班组")
    void feedback_insert_falls_back_to_userName_when_userId_unavailable() {
        // 模拟无 SecurityContext（getUserId 抛异常），userName 仍可用
        securityUtilsMock.when(SecurityUtils::getUserId).thenThrow(new RuntimeException("no context"));
        when(teamResolver.resolveByUserName("admin"))
                .thenReturn(new TeamSnapshot(20L, "NIGHT", "乙班"));

        ProFeedback fb = new ProFeedback();
        feedbackService.insertProFeedback(fb);

        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).insertProFeedback(cap.capture());
        ProFeedback saved = cap.getValue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getTeamId()).isEqualTo(20L);
        assertThat(saved.getTeamCode()).isEqualTo("NIGHT");
        assertThat(saved.getTeamName()).isEqualTo("乙班");
    }

    @Test
    @DisplayName("报工 insert：无班组归属(空快照)不报错，team 字段保持 null")
    void feedback_insert_empty_snapshot_does_not_throw() {
        when(teamResolver.resolveByUserId(1L)).thenReturn(TeamSnapshot.empty());

        ProFeedback fb = new ProFeedback();
        assertThatCode(() -> feedbackService.insertProFeedback(fb)).doesNotThrowAnyException();

        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).insertProFeedback(cap.capture());
        assertThat(cap.getValue().getTeamId()).isNull();
        assertThat(cap.getValue().getTeamCode()).isNull();
        assertThat(cap.getValue().getTeamName()).isNull();
    }

    @Test
    @DisplayName("上工 clockIn：teamResolver 返回快照时 team 字段固化到会话记录")
    void clockIn_fills_team_snapshot() {
        when(teamResolver.resolveByUserId(1L))
                .thenReturn(new TeamSnapshot(30L, "SWING", "丙班"));

        ProWorkrecord e = new ProWorkrecord();
        workrecordService.clockIn(e);

        ArgumentCaptor<ProWorkrecord> cap = ArgumentCaptor.forClass(ProWorkrecord.class);
        verify(workrecordMapper).insertProWorkrecord(cap.capture());
        ProWorkrecord saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTeamId()).isEqualTo(30L);
        assertThat(saved.getTeamCode()).isEqualTo("SWING");
        assertThat(saved.getTeamName()).isEqualTo("丙班");
    }

    @Test
    @DisplayName("报工确认 confirmFeedback：缺失班组时补填 team 快照并置 CONFIRMED")
    void confirmFeedback_should_fill_team_snapshot_when_missing() {
        Long recordId = 5L;
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(recordId);
        fb.setStatus(ProConstants.FEEDBACK_STATUS_PREPARE);
        fb.setUserId(7L);
        fb.setUserName("zhang3");
        when(feedbackMapper.selectProFeedbackByRecordId(recordId)).thenReturn(fb);
        when(teamResolver.resolveByUserId(7L))
                .thenReturn(new TeamSnapshot(10L, "DAY", "甲班"));

        feedbackService.confirmFeedback(recordId);

        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).updateProFeedback(cap.capture());
        ProFeedback saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(ProConstants.FEEDBACK_STATUS_CONFIRMED);
        assertThat(saved.getTeamId()).isEqualTo(10L);
        assertThat(saved.getTeamCode()).isEqualTo("DAY");
        assertThat(saved.getTeamName()).isEqualTo("甲班");
    }

    @Test
    @DisplayName("报工审核 auditFeedback：缺失班组时补填 team 快照并置 AUDITED")
    void auditFeedback_should_fill_team_snapshot_when_missing() {
        Long recordId = 6L;
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(recordId);
        fb.setStatus(ProConstants.FEEDBACK_STATUS_CONFIRMED);
        fb.setUserId(8L);
        fb.setUserName("li4");
        // taskId/workorderId 置空，跳过审核后的任务/工单数量回写分支，用例聚焦快照写入
        when(feedbackMapper.selectProFeedbackByRecordIdForUpdate(recordId)).thenReturn(fb);
        when(teamResolver.resolveByUserId(8L))
                .thenReturn(new TeamSnapshot(20L, "NIGHT", "乙班"));

        feedbackService.auditFeedback(recordId);

        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).updateProFeedback(cap.capture());
        ProFeedback saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(ProConstants.FEEDBACK_STATUS_AUDITED);
        assertThat(saved.getTeamId()).isEqualTo(20L);
        assertThat(saved.getTeamCode()).isEqualTo("NIGHT");
        assertThat(saved.getTeamName()).isEqualTo("乙班");
    }

    @Test
    @DisplayName("班组快照幂等：已存在 teamId 时不再调用解析器，保留原值")
    void confirmFeedback_should_not_overwrite_existing_team_snapshot() {
        Long recordId = 7L;
        ProFeedback fb = new ProFeedback();
        fb.setRecordId(recordId);
        fb.setStatus(ProConstants.FEEDBACK_STATUS_PREPARE);
        fb.setUserId(9L);
        fb.setUserName("wang5");
        fb.setTeamId(99L);
        fb.setTeamCode("KEEP");
        fb.setTeamName("保留班");
        when(feedbackMapper.selectProFeedbackByRecordId(recordId)).thenReturn(fb);

        feedbackService.confirmFeedback(recordId);

        ArgumentCaptor<ProFeedback> cap = ArgumentCaptor.forClass(ProFeedback.class);
        verify(feedbackMapper).updateProFeedback(cap.capture());
        ProFeedback saved = cap.getValue();
        assertThat(saved.getTeamId()).isEqualTo(99L);
        assertThat(saved.getTeamCode()).isEqualTo("KEEP");
        assertThat(saved.getTeamName()).isEqualTo("保留班");
        verify(teamResolver, never()).resolveByUserId(any());
        verify(teamResolver, never()).resolveByUserName(any());
    }
}
