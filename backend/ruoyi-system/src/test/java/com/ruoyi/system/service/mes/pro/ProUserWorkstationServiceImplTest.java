package com.ruoyi.system.service.mes.pro;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchResult;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.mes.pro.impl.ProUserWorkstationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * 用户工作站绑定服务单元测试
 * 覆盖：新建回填 / 已启用跳过 / 已停用重启用 / 用户或工位非法 / 单条防重 / 启停用部分更新
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户工作站绑定服务单元测试")
class ProUserWorkstationServiceImplTest {

    @Mock private ProUserWorkstationMapper mapper;
    @Mock private ISysUserService userService;
    @Mock private MdWorkstationMapper workstationMapper;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private ProUserWorkstationServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
        securityUtilsMock.when(SecurityUtils::getFactoryId).thenReturn(1L);
        // 锁模板立即执行传入动作；TransactionTemplate 用真实实例 + mock 事务管理器，回调照常执行
        doAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get())
                .when(lockTemplate).execute(anyString(), any(Supplier.class));
        doAnswer(inv -> { ((Runnable) inv.getArgument(1)).run(); return null; })
                .when(lockTemplate).execute(anyString(), any(Runnable.class));
    }

    @AfterEach
    void tearDown() { securityUtilsMock.close(); }

    private SysUser user(long id) {
        SysUser u = new SysUser();
        u.setUserId(id);
        u.setUserName("zhangsan");
        u.setNickName("张三");
        return u;
    }

    private MdWorkstation ws(long id, String enable) {
        MdWorkstation w = new MdWorkstation();
        w.setWorkstationId(id);
        w.setWorkstationCode("WS-001");
        w.setWorkstationName("一号工位");
        w.setEnableFlag(enable);
        return w;
    }

    private ProUserWorkstation row(long recordId, String enable) {
        ProUserWorkstation r = new ProUserWorkstation();
        r.setRecordId(recordId);
        r.setUserId(1L);
        r.setWorkstationId(100L);
        r.setEnableFlag(enable);
        return r;
    }

    private UserWorkstationBatchRequest req() {
        UserWorkstationBatchRequest req = new UserWorkstationBatchRequest();
        req.setUserIds(List.of(1L));
        req.setWorkstationIds(List.of(100L));
        return req;
    }

    @Test
    @DisplayName("全新组合：新建并由后端回填名称/时间/启用标志")
    void batch_newPair_insertsWithFilledNames() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(new ArrayList<>());

        UserWorkstationBatchResult r = service.batchBind(req());

        assertThat(r.getSuccessCount()).isEqualTo(1);
        assertThat(r.getReactivatedCount()).isZero();
        assertThat(r.getSkipCount()).isZero();
        verify(mapper).insertProUserWorkstation(argThat(e ->
                e.getUserId().equals(1L)
                && "zhangsan".equals(e.getUserName())
                && "张三".equals(e.getNickName())
                && e.getWorkstationId().equals(100L)
                && "WS-001".equals(e.getWorkstationCode())
                && "一号工位".equals(e.getWorkstationName())
                && "1".equals(e.getEnableFlag())
                && e.getOperationTime() != null));
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("已存在启用绑定：跳过且不写库")
    void batch_enabledExists_skips() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(List.of(row(9L, "1")));

        UserWorkstationBatchResult r = service.batchBind(req());

        assertThat(r.getSkipCount()).isEqualTo(1);
        assertThat(r.getSkips()).hasSize(1);
        verify(mapper, never()).insertProUserWorkstation(any());
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("仅有停用绑定：重新启用并刷新名称")
    void batch_disabledExists_reactivates() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(List.of(row(8L, "0")));

        UserWorkstationBatchResult r = service.batchBind(req());

        assertThat(r.getReactivatedCount()).isEqualTo(1);
        assertThat(r.getSuccessCount()).isZero();
        verify(mapper).updateProUserWorkstation(argThat(e ->
                e.getRecordId().equals(8L) && "1".equals(e.getEnableFlag())
                && "张三".equals(e.getNickName()) && e.getOperationTime() != null));
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("用户不存在：整批报错不写库")
    void batch_userMissing_throws() {
        when(userService.selectUserById(2L)).thenReturn(null);
        UserWorkstationBatchRequest req = req();
        req.setUserIds(List.of(2L));

        assertThatThrownBy(() -> service.batchBind(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("用户不存在");
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("工位停用：整批报错不写库")
    void batch_workstationDisabled_throws() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "0"));

        assertThatThrownBy(() -> service.batchBind(req()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("工位不存在或已停用");
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("入参为空/超上限：报错")
    void batch_illegalArgs_throws() {
        UserWorkstationBatchRequest empty = new UserWorkstationBatchRequest();
        assertThatThrownBy(() -> service.batchBind(empty))
                .isInstanceOf(ServiceException.class).hasMessageContaining("人员");

        // 上限校验在去重之后：需 51 个不同 ID 才触发
        UserWorkstationBatchRequest tooMany = req();
        tooMany.setUserIds(java.util.stream.LongStream.rangeClosed(1, 51).boxed().toList());
        assertThatThrownBy(() -> service.batchBind(tooMany))
                .isInstanceOf(ServiceException.class).hasMessageContaining("50");

        // 剔空后为空列表：报「请选择绑定人员」，不得静默成 0/0/0
        UserWorkstationBatchRequest nullId = req();
        nullId.setUserIds(java.util.Arrays.asList((Long) null));
        assertThatThrownBy(() -> service.batchBind(nullId))
                .isInstanceOf(ServiceException.class).hasMessageContaining("人员");
    }

    @Test
    @DisplayName("单条新增遇启用绑定：防重报错")
    void insert_duplicate_throws() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(List.of(row(9L, "1")));

        ProUserWorkstation e = new ProUserWorkstation();
        e.setUserId(1L);
        e.setWorkstationId(100L);
        assertThatThrownBy(() -> service.insertProUserWorkstation(e))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已绑定此工位");
    }

    @Test
    @DisplayName("启停用开关：仅传 recordId+enableFlag 的部分更新可用，不触发校验")
    void update_enableFlagOnly_partialUpdate() {
        ProUserWorkstation old = row(7L, "1");
        when(mapper.selectProUserWorkstationByRecordId(7L)).thenReturn(old);

        ProUserWorkstation patch = new ProUserWorkstation();
        patch.setRecordId(7L);
        patch.setEnableFlag("0");
        service.updateProUserWorkstation(patch);

        verify(mapper).updateProUserWorkstation(argThat(e ->
                e.getRecordId().equals(7L) && "0".equals(e.getEnableFlag())));
        verify(userService, never()).selectUserById(any());
    }

    @Test
    @DisplayName("改绑到已被占用的（人,工位）：拦截")
    void update_pairChange_duplicateBlocked() {
        ProUserWorkstation old = row(7L, "1");
        ProUserWorkstation patch = new ProUserWorkstation();
        patch.setRecordId(7L);
        patch.setUserId(2L);
        when(mapper.selectProUserWorkstationByRecordId(7L)).thenReturn(old);
        when(userService.selectUserById(2L)).thenReturn(user(2L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(2L, 100L)).thenReturn(List.of(row(77L, "1")));

        assertThatThrownBy(() -> service.updateProUserWorkstation(patch))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已绑定此工位");
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("改绑目标（人,工位）已存在停用记录：同样拦截，防双启用行")
    void update_pairChange_targetHasDisabledTwin_blocked() {
        ProUserWorkstation old = row(7L, "1");
        ProUserWorkstation patch = new ProUserWorkstation();
        patch.setRecordId(7L);
        patch.setUserId(2L);
        when(mapper.selectProUserWorkstationByRecordId(7L)).thenReturn(old);
        when(userService.selectUserById(2L)).thenReturn(user(2L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(2L, 100L)).thenReturn(List.of(row(77L, "0")));

        assertThatThrownBy(() -> service.updateProUserWorkstation(patch))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已绑定此工位");
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("批量绑定：经工厂级 Redisson 锁模板串行执行（先锁后事务）")
    void batchBind_runsUnderFactoryLock() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(new ArrayList<>());

        service.batchBind(req());

        verify(lockTemplate).execute(
                startsWith("mes:pro:userworkstation:bind:"), any(Supplier.class));
        verify(mapper).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("单条新增成功：委托批处理写入，名称/时间/启用标志由后端回填")
    void insert_newPair_delegatesToBatchWithFilledNames() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(new ArrayList<>());

        ProUserWorkstation e = new ProUserWorkstation();
        e.setUserId(1L);
        e.setWorkstationId(100L);
        int rows = service.insertProUserWorkstation(e);

        assertThat(rows).isEqualTo(1);
        verify(mapper).insertProUserWorkstation(argThat(x ->
                x.getUserId().equals(1L)
                && "zhangsan".equals(x.getUserName())
                && "张三".equals(x.getNickName())
                && x.getWorkstationId().equals(100L)
                && "WS-001".equals(x.getWorkstationCode())
                && "一号工位".equals(x.getWorkstationName())
                && "1".equals(x.getEnableFlag())
                && x.getOperationTime() != null));
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("单条新增并发落败：锁外守卫通过但锁内查重已存在，仍按重复报错且 0 写入")
    void insert_concurrentLoser_stillThrows() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        // 第一次：锁外快拒未命中；第二次：锁内权威查重已被并发请求抢先建成启用行
        when(mapper.selectByUserAndWorkstation(1L, 100L))
                .thenReturn(new ArrayList<>(), List.of(row(9L, "1")));

        ProUserWorkstation e = new ProUserWorkstation();
        e.setUserId(1L);
        e.setWorkstationId(100L);
        assertThatThrownBy(() -> service.insertProUserWorkstation(e))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已绑定此工位");
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("更新（含仅启停用）也走工厂锁，且以锁内重读记录为准")
    void update_enableFlagOnly_alsoRunsUnderFactoryLock() {
        when(mapper.selectProUserWorkstationByRecordId(7L)).thenReturn(row(7L, "1"));

        ProUserWorkstation patch = new ProUserWorkstation();
        patch.setRecordId(7L);
        patch.setEnableFlag("0");
        service.updateProUserWorkstation(patch);

        verify(lockTemplate).execute(startsWith("mes:pro:userworkstation:bind:"), any(Supplier.class));
        verify(mapper).updateProUserWorkstation(argThat(e ->
                e.getRecordId().equals(7L) && "0".equals(e.getEnableFlag())));
        verify(userService, never()).selectUserById(any());
    }

    @Test
    @DisplayName("selectAll：以 enableFlag=1 条件走列表查询（App 只取启用绑定）")
    void selectAll_usesEnabledCondition() {
        when(mapper.selectProUserWorkstationList(any())).thenReturn(List.of(row(9L, "1")));

        List<ProUserWorkstation> all = service.selectAll();

        assertThat(all).hasSize(1);
        verify(mapper).selectProUserWorkstationList(argThat(e -> "1".equals(e.getEnableFlag())));
    }

    @Test
    @DisplayName("工位选项：仅查启用工位并按编码升序（null 垫底）")
    void selectWorkstationOptions_enabledOnly_sortedByCode() {
        MdWorkstation a = new MdWorkstation();
        a.setWorkstationId(1L); a.setWorkstationCode("BAG-02"); a.setEnableFlag("1");
        MdWorkstation b = new MdWorkstation();
        b.setWorkstationId(2L); b.setWorkstationCode("BAG-01"); b.setEnableFlag("1");
        MdWorkstation c = new MdWorkstation();
        c.setWorkstationId(3L); c.setWorkstationCode(null); c.setEnableFlag("1");
        when(workstationMapper.selectMdWorkstationList(argThat(w -> "1".equals(w.getEnableFlag()))))
                .thenReturn(new ArrayList<>(List.of(a, c, b)));

        List<MdWorkstation> options = service.selectWorkstationOptions();

        assertThat(options).extracting(MdWorkstation::getWorkstationCode)
                .containsExactly("BAG-01", "BAG-02", null);
    }

    @Test
    @DisplayName("删除：批量/单条均直接委托 Mapper")
    void delete_delegatesToMapper() {
        service.deleteProUserWorkstationByRecordIds(new Long[] { 1L, 2L });
        service.deleteProUserWorkstationByRecordId(3L);

        verify(mapper).deleteProUserWorkstationByRecordIds(new Long[] { 1L, 2L });
        verify(mapper).deleteProUserWorkstationByRecordId(3L);
    }
}
