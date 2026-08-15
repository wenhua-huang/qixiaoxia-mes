package com.ruoyi.system.service.mes.wm.impl;

import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.vo.WmStockWarehouseSummary;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 出库草稿"工单反查精确制导"单元测试：mapOrderLinesToSalesLines
 * 验证销售行 SPU → 工单反查变体 SKU → 出库行 item_id 指向变体库存。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("出库工单反查变体库存")
class WmProductSalesVariantStockMatchTest {

    @Mock
    private ProWorkorderMapper proWorkorderMapper;
    @Mock
    private WmMaterialStockMapper wmMaterialStockMapper;
    @Mock
    private MdItemMapper mdItemMapper;
    @InjectMocks
    private WmProductSalesServiceImpl service;

    private SalOrderLine orderLine(Long lineId, Long productId, String productCode, String productName, BigDecimal qty) {
        SalOrderLine ol = new SalOrderLine();
        ol.setLineId(lineId);
        ol.setProductId(productId);
        ol.setProductCode(productCode);
        ol.setProductName(productName);
        ol.setQuantity(qty);
        return ol;
    }

    private MdItem mdItem(Long id, String code, String name) {
        MdItem item = new MdItem();
        item.setItemId(id);
        item.setItemCode(code);
        item.setItemName(name);
        item.setSpecification("规格-" + code);
        item.setUnitOfMeasure("个");
        item.setUnitName("个");
        return item;
    }

    private WmStockWarehouseSummary stock(Long itemId, String itemCode, String itemName,
                                          Long whId, String whCode, BigDecimal avail) {
        WmStockWarehouseSummary s = new WmStockWarehouseSummary();
        s.setItemId(itemId);
        s.setItemCode(itemCode);
        s.setItemName(itemName);
        s.setSpecification("规格-" + itemCode);
        s.setUnitOfMeasure("个");
        s.setUnitName("个");
        s.setWarehouseId(whId);
        s.setWarehouseCode(whCode);
        s.setWarehouseName("仓" + whCode);
        s.setQuantityAvailable(avail);
        return s;
    }

    @SuppressWarnings("unchecked")
    private List<WmProductSalesLine> map(List<SalOrderLine> lines) {
        return (List<WmProductSalesLine>) ReflectionTestUtils.invokeMethod(service, "mapOrderLinesToSalesLines", lines);
    }

    // ════════════════════ 核心场景 ════════════════════

    @Test
    @DisplayName("工单产出变体 A-V1：出库行 item_id = A-V1（生产什么出什么）")
    void workorderVariant_shipsVariantStock() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("500"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.singletonList(101L));
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.singletonList(
                        stock(101L, "FIN-A-V1", "产品A-V1", 30L, "WH01", new BigDecimal("500"))));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(101L);
        assertThat(result.get(0).getItemCode()).isEqualTo("FIN-A-V1");
        assertThat(result.get(0).getSpecification()).isEqualTo("规格-FIN-A-V1");
        assertThat(result.get(0).getQuantitySales()).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("无工单：回退到 SPU，出库行 item_id = SPU（MTS/未转工单场景）")
    void noWorkorder_fallsBackToSpu() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("300"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.emptyList());
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.singletonList(
                        stock(100L, "FIN-A", "产品A", 30L, "WH01", new BigDecimal("300"))));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("CANCEL 工单被排除：只取正常工单的 product_id")
    void cancelledWorkorderExcluded() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("200"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.singletonList(101L));
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.singletonList(
                        stock(101L, "FIN-A-V1", "产品A-V1", 30L, "WH01", new BigDecimal("200"))));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result.get(0).getItemId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("无库存但工单产出变体：红标行仍指变体 itemId（不回退 SPU）")
    void noStock_keepsVariantItemId() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("100"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.singletonList(101L));
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.emptyList());
        // 无库存时查 md_item 取变体编码/名称用于展示
        when(mdItemMapper.selectMdItemById(eq(101L)))
                .thenReturn(mdItem(101L, "FIN-A-V1", "产品A-V1"));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(101L);        // 变体，不是 SPU
        assertThat(result.get(0).getItemCode()).isEqualTo("FIN-A-V1");
        assertThat(result.get(0).getWarehouseId()).isNull();          // 红标
    }

    @Test
    @DisplayName("无库存且无工单：红标行指 SPU（MTS 无库存场景）")
    void noStock_noWorkorder_fallsBackToSpu() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("100"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.emptyList());
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.emptyList());
        // 候选 = SPU 100，查 md_item
        when(mdItemMapper.selectMdItemById(eq(100L)))
                .thenReturn(mdItem(100L, "FIN-A", "产品A"));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);
        assertThat(result.get(0).getWarehouseId()).isNull();
    }

    @Test
    @DisplayName("无库存且 md_item 丢失：回退销售行 SPU 快照（防御孤儿数据）")
    void noStock_mdItemMissing_fallsBackToOrderLine() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("100"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.singletonList(999L));
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.emptyList());
        when(mdItemMapper.selectMdItemById(eq(999L))).thenReturn(null);

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(100L);        // 回退 SPU
        assertThat(result.get(0).getItemCode()).isEqualTo("FIN-A");
        assertThat(result.get(0).getWarehouseId()).isNull();
    }

    @Test
    @DisplayName("多工单多变体：A-V1 + A-V2 都进 FIFO 候选")
    void multipleWorkorderVariants_bothCandidates() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("800"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Arrays.asList(101L, 102L));
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Arrays.asList(
                        stock(101L, "FIN-A-V1", "产品A-V1", 30L, "WH01", new BigDecimal("300")),
                        stock(102L, "FIN-A-V2", "产品A-V2", 31L, "WH02", new BigDecimal("500"))));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemId()).isEqualTo(101L);
        assertThat(result.get(0).getQuantitySales()).isEqualByComparingTo("300");
        assertThat(result.get(1).getItemId()).isEqualTo(102L);
        assertThat(result.get(1).getQuantitySales()).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("库存不足：可用量行 + 缺口行（红标），都指变体")
    void insufficientStock_remainderOnLastLine() {
        SalOrderLine ol = orderLine(10L, 100L, "FIN-A", "产品A", new BigDecimal("1000"));
        when(proWorkorderMapper.selectProductIdsBySalesOrderLineId(eq(10L)))
                .thenReturn(Collections.singletonList(101L));
        when(wmMaterialStockMapper.selectStockWarehouseSummary(anyList()))
                .thenReturn(Collections.singletonList(
                        stock(101L, "FIN-A-V1", "产品A-V1", 30L, "WH01", new BigDecimal("400"))));

        List<WmProductSalesLine> result = map(Collections.singletonList(ol));

        // 需 1000，可用 400 → 拆 2 行：第一行出 400（满），第二行缺 600（红标）
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemId()).isEqualTo(101L);
        assertThat(result.get(0).getQuantitySales()).isEqualByComparingTo("400");
        assertThat(result.get(1).getItemId()).isEqualTo(101L);
        assertThat(result.get(1).getQuantitySales()).isEqualByComparingTo("600");
        assertThat(result.get(1).getAvailableQty()).isEqualByComparingTo("400");
    }
}
