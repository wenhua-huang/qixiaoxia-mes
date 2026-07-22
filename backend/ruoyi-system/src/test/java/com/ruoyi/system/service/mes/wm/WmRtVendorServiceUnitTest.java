package com.ruoyi.system.service.mes.wm;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.domain.mes.wm.RtVendorFromPurOrderRequest;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;
import com.ruoyi.system.domain.mes.wm.WmRtVendorLine;
import com.ruoyi.system.domain.mes.wm.vo.ReturnableBatchVO;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmRtVendorMapper;
import com.ruoyi.system.service.mes.wm.impl.WmRtVendorServiceImpl;
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
import org.mockito.invocation.InvocationOnMock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 退货单 Service 单元测试
 * 覆盖：createRtVendorFromPurOrder(可退批次聚合校验/构建写库/行级仓库 + 并发锁)
 *       + postRtVendor 回写(lineId 优先 + itemId fallback)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("退货单服务单元测试")
class WmRtVendorServiceUnitTest {

    @Mock private WmRtVendorMapper wmRtVendorMapper;
    @Mock private IWmRtVendorLineService wmRtVendorLineService;
    @Mock private IWmStorageCoreService storageCoreService;
    @Mock private PurOrderLineMapper purOrderLineMapper;
    @Mock private PurOrderMapper purOrderMapper;
    @Mock private WmItemRecptLineMapper wmItemRecptLineMapper;
    @Mock private RedisLockTemplate lockTemplate;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private WmRtVendorServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("tester");
        // 模拟 Redis 锁:直接执行 supplier(单测无真实 Redis)
        lenient().when(lockTemplate.executeWithResult(anyString(), anyLong(), any()))
            .thenAnswer((InvocationOnMock inv) -> ((Supplier<?>) inv.getArgument(2)).get());
        // 模拟 TransactionTemplate:直接执行回调(单测不连 DB)
        // 用反射注入一个 mock 的 TransactionTemplate,绕过真实事务管理
        TransactionTemplate mockTx = mock(TransactionTemplate.class);
        lenient().when(mockTx.execute(any(TransactionCallback.class)))
            .thenAnswer((InvocationOnMock inv) -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        try {
            java.lang.reflect.Field f = WmRtVendorServiceImpl.class.getDeclaredField("txTemplate");
            f.setAccessible(true);
            f.set(service, mockTx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ==================== createRtVendorFromPurOrder ====================

    @Test
    @DisplayName("1. 正常生成：可退批次匹配成功,写入头表和退货行,头表仓库取第一个退货行仓库")
    void testCreateFromPurOrder_normal() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(buildReturnableBatches());

        service.createRtVendorFromPurOrder(req);

        ArgumentCaptor<WmRtVendor> headerCaptor = ArgumentCaptor.forClass(WmRtVendor.class);
        verify(wmRtVendorMapper).insertWmRtVendor(headerCaptor.capture());
        WmRtVendor savedHeader = headerCaptor.getValue();
        assertThat(savedHeader.getStatus()).isEqualTo("DRAFT");
        assertThat(savedHeader.getPurOrderId()).isEqualTo(100L);
        assertThat(savedHeader.getVendorCode()).isEqualTo("V001");
        // 头表仓库取第一个退货行的仓库(501)
        assertThat(savedHeader.getWarehouseId()).isEqualTo(501L);

        verify(wmRtVendorLineService, times(1)).insertWmRtVendorLine(any(WmRtVendorLine.class));
        verify(wmRtVendorMapper).updateWmRtVendor(any());
    }

    @Test
    @DisplayName("2. 校验失败：PO 不存在抛异常")
    void testCreateFromPurOrder_poNotFound() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("采购订单不存在");
        verify(wmRtVendorMapper, never()).insertWmRtVendor(any());
    }

    @Test
    @DisplayName("3. 校验失败：PO 状态为 CANCEL 不允许生成")
    void testCreateFromPurOrder_poCanceled() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("CANCEL"));

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不允许生成退货单");
    }

    @Test
    @DisplayName("4. 校验失败：无可退批次(入库批次已全部退完)")
    void testCreateFromPurOrder_noReturnableBatches() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("没有可退的入库批次");
    }

    @Test
    @DisplayName("5. 校验失败：退货数量超过可退量")
    void testCreateFromPurOrder_exceedReturnable() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        // 可退量 = 90, 请求退 150
        req.getLines().get(0).setQuantityRt(new BigDecimal("150"));
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(buildReturnableBatches());

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("超过可退量");
    }

    @Test
    @DisplayName("6. 校验失败：dto 的 itemId+warehouseId+batchId 匹配不到可退批次")
    void testCreateFromPurOrder_batchNotFound() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        // 用错误的 warehouseId(可退批次是 501,这里给 999)
        req.getLines().get(0).setWarehouseId(999L);
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(buildReturnableBatches());

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("没有可退批次");
    }

    @Test
    @DisplayName("7. 构建退货行：物料字段从可退批次回填,行级仓库/batch/purOrderLineId 正确写入")
    void testCreateFromPurOrder_lineFieldsBackfilled() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(buildReturnableBatches());

        service.createRtVendorFromPurOrder(req);

        ArgumentCaptor<WmRtVendorLine> lineCaptor = ArgumentCaptor.forClass(WmRtVendorLine.class);
        verify(wmRtVendorLineService).insertWmRtVendorLine(lineCaptor.capture());
        WmRtVendorLine savedLine = lineCaptor.getValue();
        assertThat(savedLine.getItemCode()).isEqualTo("ITEM-A");
        assertThat(savedLine.getPurOrderLineId()).isEqualTo(201L);
        assertThat(savedLine.getQuantityOrdered()).isEqualByComparingTo("100");
        assertThat(savedLine.getQuantityRt()).isEqualByComparingTo("80");
        // 行级仓库从 dto(行级)取,不是头表
        assertThat(savedLine.getWarehouseId()).isEqualTo(501L);
        assertThat(savedLine.getBatchId()).isEqualTo(301L);
    }

    @Test
    @DisplayName("8. 校验失败：退货行为空")
    void testCreateFromPurOrder_emptyLines() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        req.setLines(null);

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("退货行不能为空");
    }

    @Test
    @DisplayName("8b. 校验失败：可退量已减去 DRAFT 占用,SQL 返回 quantityReturnable 已反映")
    void testCreateFromPurOrder_returnableAlreadyDeductsDraft() {
        // 模拟:总入库 100,已退/草稿占用 90,可退仅剩 10
        RtVendorFromPurOrderRequest req = buildValidRequest();
        req.getLines().get(0).setQuantityRt(new BigDecimal("11")); // 超过剩余 10
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        ReturnableBatchVO b = buildReturnableBatches().get(0);
        b.setQuantityReturned(new BigDecimal("90"));
        b.setQuantityReturnable(new BigDecimal("10"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(java.util.Arrays.asList(b));

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("累计")
            .hasMessageContaining("超过可退量");
    }

    @Test
    @DisplayName("8c. 校验失败：同一请求内多条 dto 命中同批次,累加超退")
    void testCreateFromPurOrder_inRequestAccumulationOverflow() {
        // 单批可退 90,请求里两条各 80,单条都 ≤ 90 但累加 160 超退
        RtVendorFromPurOrderRequest req = buildValidRequest();
        RtVendorFromPurOrderRequest.RtVendorLineDto l1 = new RtVendorFromPurOrderRequest.RtVendorLineDto();
        l1.setItemId(1L); l1.setWarehouseId(501L); l1.setBatchId(301L);
        l1.setPurOrderLineId(201L); l1.setQuantityRt(new BigDecimal("80"));
        RtVendorFromPurOrderRequest.RtVendorLineDto l2 = new RtVendorFromPurOrderRequest.RtVendorLineDto();
        l2.setItemId(1L); l2.setWarehouseId(501L); l2.setBatchId(301L);
        l2.setPurOrderLineId(201L); l2.setQuantityRt(new BigDecimal("80"));
        req.setLines(Arrays.asList(l1, l2));
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(buildReturnableBatches());

        assertThatThrownBy(() -> service.createRtVendorFromPurOrder(req))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("累计")
            .hasMessageContaining("超过可退量");
        verify(wmRtVendorMapper, never()).insertWmRtVendor(any());
    }

    @Test
    @DisplayName("9. batchId null 与 0 归一化匹配(同一批次 null/0 视为同批)")
    void testCreateFromPurOrder_nullBatchNormalized() {
        RtVendorFromPurOrderRequest req = buildValidRequest();
        // dto 传 batchId=null,但可退批次 batchId 也是 null(归一化后都是 0,应匹配)
        req.getLines().get(0).setBatchId(null);
        when(purOrderMapper.selectPurOrderByOrderId(100L)).thenReturn(buildPo("RECEIVING"));
        // 构造 batchId=null 的可退批次
        ReturnableBatchVO batch = buildReturnableBatches().get(0);
        batch.setBatchId(null);
        when(wmItemRecptLineMapper.selectReturnableBatchesByPurOrderId(100L)).thenReturn(Arrays.asList(batch));

        service.createRtVendorFromPurOrder(req);

        verify(wmRtVendorLineService, times(1)).insertWmRtVendorLine(any(WmRtVendorLine.class));
    }

    // ==================== writebackPoOnRtVendorPost（通过 postRtVendor 触发）====================

    @Test
    @DisplayName("10. 回写优先用 purOrderLineId 精确匹配")
    void testPostRtVendor_writebackByLineId() {
        WmRtVendor header = buildPostedHeader();
        header.setRtId(300L);
        when(wmRtVendorMapper.selectWmRtVendorByRtId(300L)).thenReturn(header);

        com.ruoyi.system.domain.mes.pur.PurOrderLine poLine = new com.ruoyi.system.domain.mes.pur.PurOrderLine();
        poLine.setLineId(201L);
        poLine.setItemId(1L);
        when(purOrderLineMapper.selectPurOrderLineList(any())).thenReturn(Arrays.asList(poLine));

        WmRtVendorLine rtLine = new WmRtVendorLine();
        rtLine.setItemId(999L); // 故意给错的 itemId,验证 lineId 优先
        rtLine.setPurOrderLineId(201L);
        rtLine.setQuantityRt(new BigDecimal("30"));
        when(wmRtVendorLineService.selectWmRtVendorLineList(any())).thenReturn(Arrays.asList(rtLine));

        service.postRtVendor(300L);

        verify(purOrderLineMapper).addQuantityReturned(eq(201L), eq(new BigDecimal("30")));
    }

    @Test
    @DisplayName("11. 回写 fallback：无 purOrderLineId 时用 itemId 匹配")
    void testPostRtVendor_writebackFallbackByItemId() {
        WmRtVendor header = buildPostedHeader();
        header.setRtId(300L);
        when(wmRtVendorMapper.selectWmRtVendorByRtId(300L)).thenReturn(header);

        com.ruoyi.system.domain.mes.pur.PurOrderLine poLine = new com.ruoyi.system.domain.mes.pur.PurOrderLine();
        poLine.setLineId(201L);
        poLine.setItemId(1L);
        when(purOrderLineMapper.selectPurOrderLineList(any())).thenReturn(Arrays.asList(poLine));

        WmRtVendorLine rtLine = new WmRtVendorLine();
        rtLine.setItemId(1L);
        rtLine.setPurOrderLineId(null);
        rtLine.setQuantityRt(new BigDecimal("5"));
        when(wmRtVendorLineService.selectWmRtVendorLineList(any())).thenReturn(Arrays.asList(rtLine));

        service.postRtVendor(300L);

        verify(purOrderLineMapper).addQuantityReturned(eq(201L), eq(new BigDecimal("5")));
    }

    @Test
    @DisplayName("12. 回写：purOrderId 为 null 时跳过,不抛异常")
    void testPostRtVendor_writebackSkippedWhenNoPo() {
        WmRtVendor header = buildPostedHeader();
        header.setRtId(300L);
        header.setPurOrderId(null);
        when(wmRtVendorMapper.selectWmRtVendorByRtId(300L)).thenReturn(header);

        service.postRtVendor(300L);

        verify(purOrderLineMapper, never()).selectPurOrderLineList(any());
        verify(purOrderLineMapper, never()).addQuantityReturned(anyLong(), any());
    }

    // ==================== 测试数据工厂 ====================

    /** 一个可退批次:itemId=1, warehouseId=501, batchId=301, 入库100-已退10=可退90, purOrderLineId=201 */
    private List<ReturnableBatchVO> buildReturnableBatches() {
        ReturnableBatchVO b = new ReturnableBatchVO();
        b.setItemId(1L);
        b.setItemCode("ITEM-A");
        b.setItemName("物料A");
        b.setSpecification("规格1");
        b.setUnitOfMeasure("PCS");
        b.setUnitName("个");
        b.setWarehouseId(501L);
        b.setWarehouseCode("WH001");
        b.setWarehouseName("主仓库");
        b.setBatchId(301L);
        b.setBatchCode("BATCH-001");
        b.setRecptId(200L);
        b.setRecptCode("RECPT-001");
        b.setPurOrderLineId(201L);
        b.setQuantityRecptTotal(new BigDecimal("100"));
        b.setQuantityReturned(new BigDecimal("10"));
        b.setQuantityReturnable(new BigDecimal("90"));
        return Arrays.asList(b);
    }

    private RtVendorFromPurOrderRequest buildValidRequest() {
        RtVendorFromPurOrderRequest req = new RtVendorFromPurOrderRequest();
        req.setPurOrderId(100L);
        req.setRtCode("RT20260718001");
        RtVendorFromPurOrderRequest.RtVendorLineDto line = new RtVendorFromPurOrderRequest.RtVendorLineDto();
        line.setItemId(1L);
        line.setWarehouseId(501L);
        line.setWarehouseCode("WH001");
        line.setWarehouseName("主仓库");
        line.setBatchId(301L);
        line.setBatchCode("BATCH-001");
        line.setPurOrderLineId(201L);
        line.setQuantityRt(new BigDecimal("80"));
        req.setLines(Arrays.asList(line));
        return req;
    }

    private PurOrderVO buildPo(String status) {
        PurOrderVO po = new PurOrderVO();
        po.setOrderId(100L);
        po.setOrderCode("PO001");
        po.setStatus(status);
        po.setVendorId(10L);
        po.setVendorCode("V001");
        po.setVendorName("供应商A");
        return po;
    }

    private WmRtVendor buildPostedHeader() {
        WmRtVendor h = new WmRtVendor();
        h.setRtId(300L);
        h.setStatus("CONFIRMED");
        h.setPurOrderId(100L);
        return h;
    }
}
