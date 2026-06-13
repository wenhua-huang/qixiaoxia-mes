package com.ruoyi.web.controller.mes.wm;

import com.ruoyi.BaseIntegrationTest;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.wm.tx.*;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 库存核心事务服务 — 集成测试。
 *
 * 覆盖所有 processXxx 调用方，验证 stock + transaction 一致性及幂等性。
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test,druid"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = "classpath:sql/qxx_wm_core.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class WmStorageCoreServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @Autowired
    private WmMaterialStockMapper stockMapper;

    @Autowired
    private WmTransactionMapper txMapper;

    private static final Long WAREHOUSE_ID = 99901L;
    private static final Long WAREHOUSE_ID_2 = 99902L;
    private static final Long ITEM_ID = 88801L;
    private static final Long VENDOR_ID = 77701L;
    private static final Long BATCH_ID = 66601L;
    private static final Long SOURCE_DOC_ID = 55501L;

    @BeforeAll
    void setupAuthAndWarehouses() {
        // 设置认证上下文（FactoryIdInterceptor 需要 SecurityUtils.getFactoryId()）
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setFactoryId(1L);
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @BeforeAll
    void insertTestWarehouses() {
        jdbcTemplate.update("INSERT INTO qxx_wm_warehouse (factory_id, warehouse_code, warehouse_name, enable_flag) VALUES (1, 'WH-TEST-01', '测试仓库1', '1')");
        jdbcTemplate.update("INSERT INTO qxx_wm_warehouse (factory_id, warehouse_code, warehouse_name, enable_flag) VALUES (1, 'WH-TEST-02', '测试仓库2', '1')");
    }

    @BeforeEach
    void cleanData() {
        truncateTables("qxx_wm_transaction", "qxx_wm_material_stock");
    }

    /** 获取第一个仓库的真实 ID */
    private Long wh1Id() {
        return jdbcTemplate.queryForObject("SELECT warehouse_id FROM qxx_wm_warehouse WHERE warehouse_code='WH-TEST-01'", Long.class);
    }
    private Long wh2Id() {
        return jdbcTemplate.queryForObject("SELECT warehouse_id FROM qxx_wm_warehouse WHERE warehouse_code='WH-TEST-02'", Long.class);
    }

    // ═══════════════════════════════════════════════════════════
    // 1. 物料入库 — stock + transaction
    // ═══════════════════════════════════════════════════════════

    @Test
    void processItemRecpt_shouldCreateStockAndTransaction() {
        ItemRecptTxBean bean = buildItemRecptBean(VENDOR_ID);
        storageCoreService.processItemRecpt(Collections.singletonList(bean));

        List<WmMaterialStock> stocks = stockMapper.selectWmMaterialStockAll();
        assertThat(stocks).hasSize(1);
        WmMaterialStock stock = stocks.get(0);
        assertThat(stock.getItemId()).isEqualTo(ITEM_ID);
        assertThat(stock.getWarehouseId()).isEqualTo(wh1Id());
        assertThat(stock.getVendorId()).isEqualTo(VENDOR_ID);
        assertThat(stock.getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("100"));

        List<WmTransaction> txns = txMapper.selectWmTransactionAll();
        assertThat(txns).hasSize(1);
        assertThat(txns.get(0).getTransactionType()).isEqualTo("ITEM_RECPT");
        assertThat(txns.get(0).getMaterialStockId()).isEqualTo(stock.getMaterialStockId());
    }

    @Test
    void processItemRecpt_shouldBeIdempotent() {
        ItemRecptTxBean bean = buildItemRecptBean(VENDOR_ID);
        storageCoreService.processItemRecpt(Collections.singletonList(bean));
        storageCoreService.processItemRecpt(Collections.singletonList(bean)); // retry

        assertThat(stockMapper.selectWmMaterialStockAll()).hasSize(1);
        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(new BigDecimal("100"));
        assertThat(txMapper.selectWmTransactionAll()).hasSize(1);
    }

    @Test
    void processItemRecpt_shouldAccumulateQuantity() {
        ItemRecptTxBean b1 = buildItemRecptBean(VENDOR_ID);
        ItemRecptTxBean b2 = buildItemRecptBean(VENDOR_ID);
        b2.setSourceDocLineId(SOURCE_DOC_ID + 1);
        b2.setSourceDocCode("TEST-DOC-002");

        storageCoreService.processItemRecpt(Collections.singletonList(b1));
        storageCoreService.processItemRecpt(Collections.singletonList(b2));

        assertThat(stockMapper.selectWmMaterialStockAll()).hasSize(1);
        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(new BigDecimal("200"));
        assertThat(txMapper.selectWmTransactionAll()).hasSize(2);
    }

    // ═══════════════════════════════════════════════════════════
    // 2. 杂项入库
    // ═══════════════════════════════════════════════════════════

    @Test
    void processMiscRecpt_shouldCreateStock() {
        MiscRecptTxBean bean = new MiscRecptTxBean();
        populateMisc(bean, "TEST-MISC-RECPT");
        storageCoreService.processMiscRecpt(Collections.singletonList(bean));

        assertThat(stockMapper.selectWmMaterialStockAll()).hasSize(1);
        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(new BigDecimal("100"));
    }

    // ═══════════════════════════════════════════════════════════
    // 3. 杂项出库
    // ═══════════════════════════════════════════════════════════

    @Test
    void processMiscIssue_shouldDeductStock() {
        // inbound first (vendorId=0 so misc_issue can match)
        storageCoreService.processItemRecpt(Collections.singletonList(buildItemRecptBean(0L)));

        MiscIssueTxBean out = new MiscIssueTxBean();
        populateMisc(out, "TEST-ISSUE-001");

        storageCoreService.processMiscIssue(Collections.singletonList(out));

        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(txMapper.selectWmTransactionAll()).hasSize(2);
        assertThat(txMapper.selectWmTransactionAll())
                .extracting(WmTransaction::getTransactionType)
                .contains("MISC_ISSUE");
    }

    @Test
    void processMiscIssue_shouldThrowWhenNoStock() {
        MiscIssueTxBean bean = new MiscIssueTxBean();
        populateMisc(bean, "TEST-NO-STOCK");

        assertThatThrownBy(() ->
                storageCoreService.processMiscIssue(Collections.singletonList(bean)))
                .hasMessageContaining("库存记录不存在");
    }

    // ═══════════════════════════════════════════════════════════
    // 4. 调拨转移
    // ═══════════════════════════════════════════════════════════

    @Test
    void processTransfer_shouldMoveStockBetweenWarehouses() {
        Long wh1 = wh1Id();
        Long wh2 = wh2Id();

        // inbound to wh1 (unique sourceDocLineId so transfer doesn't conflict)
        ItemRecptTxBean inBean = buildItemRecptBean(0L);
        inBean.setSourceDocLineId(SOURCE_DOC_ID);
        storageCoreService.processItemRecpt(Collections.singletonList(inBean));

        TransferTxBean t = buildTransferBean(wh1, wh2);
        t.setSourceDocLineId(SOURCE_DOC_ID + 10); // different line from inbound
        storageCoreService.processTransfer(Collections.singletonList(t));

        List<WmMaterialStock> stocks = stockMapper.selectWmMaterialStockAll();
        assertThat(stocks).hasSize(2);
        WmMaterialStock src = stocks.stream().filter(s -> wh1.equals(s.getWarehouseId())).findFirst().orElseThrow();
        WmMaterialStock dst = stocks.stream().filter(s -> wh2.equals(s.getWarehouseId())).findFirst().orElseThrow();
        assertThat(src.getQuantityOnhand()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dst.getQuantityOnhand()).isEqualByComparingTo(new BigDecimal("100"));

        assertThat(txMapper.selectWmTransactionAll()).hasSize(3);
    }

    @Test
    void processTransfer_shouldBeIdempotent() {
        Long wh1 = wh1Id();
        Long wh2 = wh2Id();

        ItemRecptTxBean inBean = buildItemRecptBean(0L);
        inBean.setSourceDocLineId(SOURCE_DOC_ID + 20);
        storageCoreService.processItemRecpt(Collections.singletonList(inBean));

        TransferTxBean t = buildTransferBean(wh1, wh2);
        t.setSourceDocLineId(SOURCE_DOC_ID + 30);
        t.setSourceDocCode("TEST-TRANS-IDEM");
        storageCoreService.processTransfer(Collections.singletonList(t));
        storageCoreService.processTransfer(Collections.singletonList(t)); // retry

        // 3 records: inbound + 1 tx_out + 1 tx_in — no duplicates
        List<WmTransaction> txns = txMapper.selectWmTransactionAll();
        assertThat(txns).hasSize(3);
        assertThat(txns).filteredOn(tx -> "TRANS_OUT".equals(tx.getTransactionType())).hasSize(1);
    }

    // ═══════════════════════════════════════════════════════════
    // 5. 多供应商库存隔离
    // ═══════════════════════════════════════════════════════════

    @Test
    void differentVendors_shouldCreateSeparateStockRecords() {
        ItemRecptTxBean v1 = buildItemRecptBean(100L);
        ItemRecptTxBean v2 = buildItemRecptBean(200L);
        v2.setSourceDocLineId(SOURCE_DOC_ID + 1);  // different line → not idempotent
        v2.setSourceDocCode("TEST-DOC-V2");
        storageCoreService.processItemRecpt(Collections.singletonList(v1));
        storageCoreService.processItemRecpt(Collections.singletonList(v2));

        List<WmMaterialStock> stocks = stockMapper.selectWmMaterialStockAll();
        assertThat(stocks).hasSize(2);
        assertThat(stocks).extracting(WmMaterialStock::getVendorId)
                .containsExactlyInAnyOrder(100L, 200L);
        assertThat(stocks).allMatch(s -> s.getQuantityOnhand().compareTo(new BigDecimal("100")) == 0);
    }

    // ═══════════════════════════════════════════════════════════
    // 6. 销售出库 + 销售退货
    // ═══════════════════════════════════════════════════════════

    @Test
    void productSales_thenRtSales_consistency() {
        storageCoreService.processItemRecpt(Collections.singletonList(buildItemRecptBean(0L)));

        ProductSalesTxBean sales = new ProductSalesTxBean();
        populateMisc(sales, "TEST-SALES");
        storageCoreService.processProductSales(Collections.singletonList(sales));
        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(BigDecimal.ZERO);

        RtSalesTxBean ret = new RtSalesTxBean();
        populateMisc(ret, "TEST-SALES-RT");
        ret.setTransactionQuantity(new BigDecimal("50"));
        storageCoreService.processRtSales(Collections.singletonList(ret));
        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(new BigDecimal("50"));

        assertThat(txMapper.selectWmTransactionAll()).hasSize(3);
    }

    // ═══════════════════════════════════════════════════════════
    // 7. 供应商退货
    // ═══════════════════════════════════════════════════════════

    @Test
    void processRtVendor_shouldDeductFromVendorStock() {
        storageCoreService.processItemRecpt(Collections.singletonList(buildItemRecptBean(VENDOR_ID)));

        RtVendorTxBean rt = new RtVendorTxBean();
        populateMisc(rt, "TEST-RTV");
        rt.setVendorId(VENDOR_ID);

        storageCoreService.processRtVendor(Collections.singletonList(rt));

        assertThat(stockMapper.selectWmMaterialStockAll().get(0).getQuantityOnhand())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ═══════════════════════════════════════════════════════════
    // helpers
    // ═══════════════════════════════════════════════════════════

    private ItemRecptTxBean buildItemRecptBean(Long vendorId) {
        ItemRecptTxBean b = new ItemRecptTxBean();
        setCommon(b);
        b.setSourceDocType("wm_item_recpt");
        b.setVendorId(vendorId);
        return b;
    }

    private TransferTxBean buildTransferBean(Long fromWh, Long toWh) {
        TransferTxBean b = new TransferTxBean();
        setCommon(b);
        b.setSourceDocType("wm_transfer");
        b.setSourceDocCode("TEST-TRANS");
        b.setFromWarehouseId(fromWh);
        b.setFromWarehouseCode("WH-TEST-01");
        b.setFromWarehouseName("测试仓库1");
        b.setToWarehouseId(toWh);
        b.setToWarehouseCode("WH-TEST-02");
        b.setToWarehouseName("测试仓库2");
        return b;
    }

    /** 填充 TxBean 公共字段（不含 sourceDocType 和 warehouse 字段，各调用方自行设置） */
    private void setCommon(Object b) {
        try {
            Class<?> c = b.getClass();
            c.getMethod("setSourceDocId", Long.class).invoke(b, SOURCE_DOC_ID);
            c.getMethod("setSourceDocCode", String.class).invoke(b, "TEST-DOC-001");
            c.getMethod("setSourceDocLineId", Long.class).invoke(b, SOURCE_DOC_ID);
            c.getMethod("setItemId", Long.class).invoke(b, ITEM_ID);
            c.getMethod("setItemCode", String.class).invoke(b, "ITEM-001");
            c.getMethod("setItemName", String.class).invoke(b, "测试物料");
            c.getMethod("setSpecification", String.class).invoke(b, "SPEC");
            c.getMethod("setUnitOfMeasure", String.class).invoke(b, "PCS");
            c.getMethod("setUnitName", String.class).invoke(b, "个");
            c.getMethod("setTransactionQuantity", BigDecimal.class).invoke(b, new BigDecimal("100"));
            c.getMethod("setBatchId", Long.class).invoke(b, BATCH_ID);
            c.getMethod("setBatchCode", String.class).invoke(b, "BATCH-001");
            // warehouse fields: callers set themselves (TransferTxBean has from/to variants)
            if (!(b instanceof TransferTxBean)) {
                c.getMethod("setWarehouseId", Long.class).invoke(b, wh1Id());
                c.getMethod("setWarehouseCode", String.class).invoke(b, "WH-TEST-01");
                c.getMethod("setWarehouseName", String.class).invoke(b, "测试仓库1");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 填充 misc 类 bean 的 sourceDocType（调用方传入） */
    private void populateMisc(Object b, String docCode) {
        setCommon(b);
        try {
            b.getClass().getMethod("setSourceDocType", String.class).invoke(b, "wm_" + docCode.toLowerCase().replace("-", "_"));
            b.getClass().getMethod("setSourceDocCode", String.class).invoke(b, docCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
