package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Supplier;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesBoxMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 销售出库-装箱 Service 单元测试
 * 覆盖：insert（自动箱号 BOX-NNN + 体积自动算 + 锁串行）/
 *      update/delete（SHIPPED 状态守卫）
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("销售装箱服务单元测试")
class WmProductSalesBoxServiceImplTest
{
    @Mock private WmProductSalesBoxMapper boxMapper;
    @Mock private WmProductSalesLineMapper lineMapper;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private WmProductSalesBoxServiceImpl boxService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private MockedStatic<DateUtils> dateUtilsMock;

    @BeforeEach
    void setUp()
    {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
        dateUtilsMock = mockStatic(DateUtils.class);
        dateUtilsMock.when(DateUtils::getNowDate).thenReturn(new Date());

        when(lockTemplate.executeWithResult(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(inv -> { Supplier<?> s = inv.getArgument(2); return s.get(); });

        ReflectionTestUtils.setField(boxService, "txTemplate", new TransactionTemplate(transactionManager));
        when(transactionManager.getTransaction(any())).thenReturn(null);
    }

    @AfterEach
    void tearDown()
    {
        securityUtilsMock.close();
        dateUtilsMock.close();
    }

    @Test
    @DisplayName("insert - 空箱号自动生成 BOX-NNN（最大序号+1）")
    void insert_autoBoxNo()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(215L)).thenReturn(7);
        when(boxMapper.insertWmProductSalesBox(any())).thenReturn(1);

        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        boxService.insertWmProductSalesBox(box);

        assertThat(box.getBoxNo()).isEqualTo("BOX-008");
        assertThat(box.getStatus()).isEqualTo("PACKED");
        verify(boxMapper).insertWmProductSalesBox(box);
    }

    @Test
    @DisplayName("insert - 首箱从 BOX-001 开始")
    void insert_firstBox()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(215L)).thenReturn(0);
        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        boxService.insertWmProductSalesBox(box);
        assertThat(box.getBoxNo()).isEqualTo("BOX-001");
    }

    @Test
    @DisplayName("insert - 体积 = 长×宽×高÷1000000（cm³→m³，4位小数）")
    void insert_calcVolume()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(anyLong())).thenReturn(0);
        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        box.setBoxLength(new BigDecimal("50"));
        box.setBoxWidth(new BigDecimal("40"));
        box.setBoxHeight(new BigDecimal("30"));
        boxService.insertWmProductSalesBox(box);
        // 50*40*30=60000 cm³ = 0.06 m³
        assertThat(box.getVolume()).isEqualByComparingTo("0.0600");
    }

    @Test
    @DisplayName("insert - 任一维度为空体积为 0")
    void insert_zeroVolume()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(anyLong())).thenReturn(0);
        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        box.setBoxLength(new BigDecimal("50"));
        // width/height 空
        boxService.insertWmProductSalesBox(box);
        assertThat(box.getVolume()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("insert - 缺 salesId 拒绝")
    void insert_noSalesId_reject()
    {
        WmProductSalesBox box = new WmProductSalesBox();
        assertThatThrownBy(() -> boxService.insertWmProductSalesBox(box))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("出库单ID不能为空");
    }

    @Test
    @DisplayName("insert - 使用 Redis 锁（同一 salesId 串行）")
    void insert_useLock()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(anyLong())).thenReturn(0);
        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        boxService.insertWmProductSalesBox(box);
        // 锁 key 必须含 salesId，保证同一出库单装箱串行
        verify(lockTemplate).executeWithResult(eq("wm:salesout:lock:215"), eq(10L), any(Supplier.class));
    }

    @Test
    @DisplayName("update - 已发运的箱拒绝修改")
    void update_shipped_reject()
    {
        WmProductSalesBox exist = new WmProductSalesBox();
        exist.setBoxId(201L);
        exist.setStatus("SHIPPED");
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(exist);

        WmProductSalesBox box = new WmProductSalesBox();
        box.setBoxId(201L);
        assertThatThrownBy(() -> boxService.updateWmProductSalesBox(box))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已发运的箱不可修改");
        verify(boxMapper, never()).updateWmProductSalesBox(any());
    }

    @Test
    @DisplayName("delete - 已发运的箱拒绝删除")
    void delete_shipped_reject()
    {
        WmProductSalesBox exist = new WmProductSalesBox();
        exist.setBoxId(201L);
        exist.setStatus("SHIPPED");
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(exist);

        assertThatThrownBy(() -> boxService.deleteWmProductSalesBoxByBoxId(201L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已发运的箱不可删除");
        verify(boxMapper, never()).deleteWmProductSalesBoxByBoxId(anyLong());
    }

    // ═══════════════ 装箱量 ≤ 已出库确认量 守卫 ═══════════════

    private WmProductSalesLine buildLine(Long lineId, String posted) {
        WmProductSalesLine l = new WmProductSalesLine();
        l.setLineId(lineId);
        l.setItemCode("P001");
        l.setQuantityPosted(new BigDecimal(posted));
        return l;
    }

    @Test
    @DisplayName("insert - 装箱量超过已出库确认量拒绝")
    void insert_overPosted_reject()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(215L)).thenReturn(0);
        when(lineMapper.selectWmProductSalesLineByLineId(301L)).thenReturn(buildLine(301L, "5"));
        when(boxMapper.selectWmProductSalesBoxList(any())).thenReturn(java.util.Collections.emptyList());

        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        box.setLineId(301L);
        box.setQuantity(new BigDecimal("8")); // 8 > 5

        assertThatThrownBy(() -> boxService.insertWmProductSalesBox(box))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("超过已出库确认量");
        verify(boxMapper, never()).insertWmProductSalesBox(any());
    }

    @Test
    @DisplayName("insert - 装箱量等于已出库确认量通过")
    void insert_withinPosted_ok()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(215L)).thenReturn(0);
        when(lineMapper.selectWmProductSalesLineByLineId(301L)).thenReturn(buildLine(301L, "5"));
        when(boxMapper.selectWmProductSalesBoxList(any())).thenReturn(java.util.Collections.emptyList());
        when(boxMapper.insertWmProductSalesBox(any())).thenReturn(1);

        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        box.setLineId(301L);
        box.setQuantity(new BigDecimal("5"));
        boxService.insertWmProductSalesBox(box);

        verify(boxMapper).insertWmProductSalesBox(box);
    }

    @Test
    @DisplayName("insert - 已有箱累计 + 本次超过已出库确认量拒绝")
    void insert_accumulatedOverPosted_reject()
    {
        when(boxMapper.selectMaxBoxSeqBySalesId(215L)).thenReturn(1);
        when(lineMapper.selectWmProductSalesLineByLineId(301L)).thenReturn(buildLine(301L, "5"));
        // 已有一箱 3
        WmProductSalesBox existed = new WmProductSalesBox();
        existed.setBoxId(201L);
        existed.setQuantity(new BigDecimal("3"));
        when(boxMapper.selectWmProductSalesBoxList(any())).thenReturn(java.util.Collections.singletonList(existed));

        WmProductSalesBox box = new WmProductSalesBox();
        box.setSalesId(215L);
        box.setLineId(301L);
        box.setQuantity(new BigDecimal("3")); // 3+3=6 > 5

        assertThatThrownBy(() -> boxService.insertWmProductSalesBox(box))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("超过已出库确认量");
    }

    @Test
    @DisplayName("update - 非发运箱改数量超过已出库确认量拒绝（走锁+事务）")
    void update_overPosted_reject()
    {
        WmProductSalesBox exist = new WmProductSalesBox();
        exist.setBoxId(201L);
        exist.setSalesId(215L);
        exist.setLineId(301L);
        exist.setStatus("PACKED");
        when(boxMapper.selectWmProductSalesBoxByBoxId(201L)).thenReturn(exist);
        when(lineMapper.selectWmProductSalesLineByLineId(301L)).thenReturn(buildLine(301L, "5"));
        when(boxMapper.selectWmProductSalesBoxList(any())).thenReturn(java.util.Collections.emptyList());

        WmProductSalesBox box = new WmProductSalesBox();
        box.setBoxId(201L);
        box.setQuantity(new BigDecimal("8"));

        assertThatThrownBy(() -> boxService.updateWmProductSalesBox(box))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("超过已出库确认量");
        verify(boxMapper, never()).updateWmProductSalesBox(any());
    }
}
