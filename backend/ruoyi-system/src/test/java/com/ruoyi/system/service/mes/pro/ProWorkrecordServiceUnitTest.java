package com.ruoyi.system.service.mes.pro;

import java.util.Date;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkrecordMapper;
import com.ruoyi.system.service.mes.pro.impl.ProWorkrecordServiceImpl;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 上下工会话记录Service单元测试
 * 覆盖：上工正常/重复上工拦截/下工结算/无在岗下工报错
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("上下工打卡服务单元测试")
class ProWorkrecordServiceUnitTest {

    @Mock private ProWorkrecordMapper workrecordMapper;
    @Mock private ProUserWorkstationMapper userWorkstationMapper;
    @Mock private MdWorkstationMapper mdWorkstationMapper;
    @Mock private ProTaskMapper proTaskMapper;
    @Mock private RedisLockTemplate lockTemplate;

    @InjectMocks
    private ProWorkrecordServiceImpl workrecordService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");

        // getLoginUser().getUser().getNickName() 链
        LoginUser loginUser = new LoginUser();
        SysUser user = new SysUser();
        user.setNickName("管理员");
        loginUser.setUser(user);
        securityUtilsMock.when(SecurityUtils::getLoginUser).thenReturn(loginUser);

        // 锁直接执行 action（绕过 Redis）— clockIn/clockOut 用 Supplier 重载（带返回值）
        doAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(1)).get())
            .when(lockTemplate).execute(anyString(), any(java.util.function.Supplier.class));

        // 工位反查
        MdWorkstation ws = new MdWorkstation();
        ws.setWorkstationId(100L);
        ws.setWorkstationCode("WS-001");
        ws.setWorkstationName("测试工位");
        when(mdWorkstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("上工正常：注入用户 + 反查工位 + INSERT ACTIVE")
    void testClockInSuccess() {
        when(workrecordMapper.selectActiveByUser(any(ProWorkrecord.class))).thenReturn(null);
        when(workrecordMapper.insertProWorkrecord(any(ProWorkrecord.class))).thenReturn(1);

        ProWorkrecord e = new ProWorkrecord();
        e.setWorkstationId(100L);
        workrecordService.clockIn(e);

        assertThat(e.getUserId()).isEqualTo(1L);
        assertThat(e.getUserName()).isEqualTo("admin");
        assertThat(e.getNickName()).isEqualTo("管理员");
        assertThat(e.getStatus()).isEqualTo("ACTIVE");
        assertThat(e.getWorkstationCode()).isEqualTo("WS-001");
        assertThat(e.getWorkstationName()).isEqualTo("测试工位");
        assertThat(e.getClockInTime()).isNotNull();
        verify(workrecordMapper).insertProWorkrecord(e);
    }

    @Test
    @DisplayName("重复上工拦截：已有 ACTIVE 会话抛异常")
    void testClockInDuplicateBlocked() {
        ProWorkrecord active = new ProWorkrecord();
        active.setStatus("ACTIVE");
        active.setWorkstationName("已有工位");
        when(workrecordMapper.selectActiveByUser(any(ProWorkrecord.class))).thenReturn(active);

        ProWorkrecord e = new ProWorkrecord();
        e.setWorkstationId(100L);
        assertThatThrownBy(() -> workrecordService.clockIn(e))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("请先下工");
        verify(workrecordMapper, never()).insertProWorkrecord(any());
    }

    @Test
    @DisplayName("下工结算：算工时 + status=CLOSED")
    void testClockOutCalculatesDuration() {
        ProWorkrecord active = new ProWorkrecord();
        active.setRecordId(1L);
        active.setStatus("ACTIVE");
        active.setClockInTime(new Date(System.currentTimeMillis() - 120 * 60 * 1000L)); // 2 小时前
        when(workrecordMapper.selectActiveByUser(any(ProWorkrecord.class))).thenReturn(active);
        when(workrecordMapper.closeSession(any(ProWorkrecord.class))).thenReturn(1);

        workrecordService.clockOut(new ProWorkrecord());

        assertThat(active.getStatus()).isEqualTo("CLOSED");
        assertThat(active.getClockOutTime()).isNotNull();
        assertThat(active.getWorkDuration()).isBetween(119, 121); // ≈120 分钟
        verify(workrecordMapper).closeSession(active);
    }

    @Test
    @DisplayName("无在岗下工报错：没有 ACTIVE 会话")
    void testClockOutNoActiveThrows() {
        when(workrecordMapper.selectActiveByUser(any(ProWorkrecord.class))).thenReturn(null);

        assertThatThrownBy(() -> workrecordService.clockOut(new ProWorkrecord()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("没有进行中的上工记录");
        verify(workrecordMapper, never()).closeSession(any());
    }

    @Test
    @DisplayName("查当前在岗会话：无记录返回 null")
    void testSelectActiveSessionReturnsNull() {
        when(workrecordMapper.selectActiveByUser(any(ProWorkrecord.class))).thenReturn(null);
        assertThat(workrecordService.selectActiveSession()).isNull();
    }
}
