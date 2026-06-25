package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.domain.mes.md.MdItemBatchConfig;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.service.mes.md.IMdItemBatchConfigService;
import com.ruoyi.system.service.mes.md.IMdItemService;
import com.ruoyi.system.service.mes.pro.impl.ProWorkorderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 生产工单 Service 单元测试 — SKU变体创建
 * 技术栈：JUnit 5 + Mockito，不启动 Spring 容器
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("生产工单服务单元测试 — 变体创建")
class ProWorkorderServiceSkuUnitTest {

    @Mock private ProWorkorderMapper workorderMapper;
    @Mock private IProWorkorderBomService workorderBomService;
    @Mock private IProWorkorderParamService workorderParamService;
    @Mock private IMdItemService mdItemService;
    @Mock private IMdItemBatchConfigService mdItemBatchConfigService;
    @Mock private IProRouteProductService routeProductService;
    @Mock private IProWorkorderChangeService changeService;
    @Mock private com.ruoyi.system.service.mes.md.IMdProductBomService mdProductBomService;
    @InjectMocks private ProWorkorderServiceImpl workorderService;

    private ProWorkorder testWorkorder;
    private MdItem parentItem;

    @BeforeEach
    void setUp() {
        testWorkorder = new ProWorkorder();
        testWorkorder.setWorkorderCode("WO-SKU-001");
        testWorkorder.setWorkorderName("测试工单");
        testWorkorder.setProductId(201L);
        testWorkorder.setProductCode("ZD-01");
        testWorkorder.setProductName("奔趣纸袋");
        testWorkorder.setQuantity(new BigDecimal("100"));
        testWorkorder.setFactoryId(1L);

        parentItem = new MdItem();
        parentItem.setItemId(201L);
        parentItem.setItemCode("ZD-01");
        parentItem.setItemName("奔趣纸袋");
        parentItem.setUnitOfMeasure("PCS");
        parentItem.setUnitName("个");
        parentItem.setItemTypeId(10L);
        parentItem.setItemTypeCode("BAG");
        parentItem.setBatchFlag("1");
        parentItem.setPrintingReq("1色满版黑");

        // lenient: 产品BOM查询默认返回空（部分测试不需要）
        lenient().when(mdProductBomService.selectMdProductBomList(any(com.ruoyi.system.domain.mes.md.MdProductBom.class)))
                .thenReturn(new java.util.ArrayList<>());

        // lenient: 部分测试（如 testCreateSkuVariant_ParentNotFound）不会走到 insert
        lenient().doAnswer(invocation -> {
            ProWorkorder w = invocation.getArgument(0);
            w.setWorkorderId(9001L);
            return 1;
        }).when(workorderMapper).insertProWorkorder(any(ProWorkorder.class));
    }

    // ══════════════════════════════════════════════
    // 1. testCreateSkuVariant_Success
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("1. 用户选'是' → 变体 item 创建成功 + 批次配置复制 + 路线复制 + 工单 productId 替换")
    void testCreateSkuVariant_Success() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            // given: 启用变体创建
            testWorkorder.setCreateSkuVariant(true);
            testWorkorder.setSkuCode("ZD-01-V1");
            testWorkorder.setSkuName("奔趣纸袋-彩印红绳");

            when(mdItemService.selectMdItemById(201L)).thenReturn(parentItem);
            doAnswer(invocation -> {
                MdItem item = invocation.getArgument(0);
                item.setItemId(301L); // SKU itemId
                return 1;
            }).when(mdItemService).insertMdItem(any(MdItem.class));

            // Mock batch config copy
            MdItemBatchConfig parentConfig = new MdItemBatchConfig();
            parentConfig.setItemId(201L);
            parentConfig.setProduceDateFlag("1");
            parentConfig.setWorkorderFlag("1");
            parentConfig.setLotNumberFlag("1");
            when(mdItemBatchConfigService.selectMdItemBatchConfigByItemId(201L)).thenReturn(parentConfig);
            when(mdItemBatchConfigService.insertMdItemBatchConfig(any(MdItemBatchConfig.class))).thenReturn(1);

            // Mock route copy
            when(routeProductService.copyRouteProductForSku(eq(201L), eq(301L), eq("ZD-01-V1"), eq("奔趣纸袋-彩印红绳"))).thenReturn(1);

            // when
            ProWorkorder result = workorderService.createWorkorderWithBom(testWorkorder, new ArrayList<>(), new ArrayList<>());

            // then: 变体 item 被创建
            ArgumentCaptor<MdItem> itemCaptor = ArgumentCaptor.forClass(MdItem.class);
            verify(mdItemService).insertMdItem(itemCaptor.capture());
            MdItem created = itemCaptor.getValue();
            assertThat(created.getParentId()).isEqualTo(201L);
            assertThat(created.getItemCode()).isEqualTo("ZD-01-V1");
            assertThat(created.getItemName()).isEqualTo("奔趣纸袋-彩印红绳");

            // then: batch config 被复制
            ArgumentCaptor<MdItemBatchConfig> configCaptor = ArgumentCaptor.forClass(MdItemBatchConfig.class);
            verify(mdItemBatchConfigService).insertMdItemBatchConfig(configCaptor.capture());
            assertThat(configCaptor.getValue().getItemId()).isEqualTo(301L);
            assertThat(configCaptor.getValue().getProduceDateFlag()).isEqualTo("1");

            // then: workorder productId 已替换为 SKU
            assertThat(result.getProductId()).isEqualTo(301L);
            assertThat(result.getProductCode()).isEqualTo("ZD-01-V1");
            assertThat(result.getWorkorderId()).isEqualTo(9001L);

            // then: 路线复制被调用
            verify(routeProductService).copyRouteProductForSku(201L, 301L, "ZD-01-V1", "奔趣纸袋-彩印红绳");
        }
    }

    // ══════════════════════════════════════════════
    // 2. testCreateSkuVariant_Decline
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("2. 用户选'否' → 不创建变体，工单沿用原 productId")
    void testCreateSkuVariant_Decline() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            // given: createSkuVariant=false → 不创建变体
            testWorkorder.setCreateSkuVariant(false);

            // when
            ProWorkorder result = workorderService.createWorkorderWithBom(testWorkorder, new ArrayList<>(), new ArrayList<>());

            // then: mdItemService.insertMdItem 从未被调用
            verify(mdItemService, never()).insertMdItem(any(MdItem.class));

            // then: productId 保持不变
            assertThat(result.getProductId()).isEqualTo(201L);
            assertThat(result.getProductCode()).isEqualTo("ZD-01");

            // then: 路线复制从未被调用
            verify(routeProductService, never()).copyRouteProductForSku(anyLong(), anyLong(), anyString(), anyString());
        }
    }

    // ══════════════════════════════════════════════
    // 3. testCreateSkuVariant_NoFlag_NoVariant
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("3. createSkuVariant=null → 不创建变体（向后兼容）")
    void testCreateSkuVariant_NoFlag_NoVariant() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            // given: createSkuVariant 未设置（向后兼容老前端）
            testWorkorder.setCreateSkuVariant(null);

            // when
            ProWorkorder result = workorderService.createWorkorderWithBom(testWorkorder, null, null);

            // then: 不创建变体
            verify(mdItemService, never()).insertMdItem(any(MdItem.class));
            assertThat(result.getProductId()).isEqualTo(201L);
        }
    }

    // ══════════════════════════════════════════════
    // 4. testCreateSkuVariant_DiffFieldsInheritance
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("4. 变体继承父产品 type/单位；差异字段回填工单调整值")
    void testCreateSkuVariant_DiffFieldsInheritance() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            // given: 工单有印刷要求调整
            testWorkorder.setCreateSkuVariant(true);
            testWorkorder.setSkuCode("ZD-01-V2");
            testWorkorder.setSkuName("奔趣-彩印");
            testWorkorder.setPrintingReq("彩印红绳"); // 差异字段
            testWorkorder.setProductSize("30x20x10cm");

            when(mdItemService.selectMdItemById(201L)).thenReturn(parentItem);
            doAnswer(invocation -> {
                MdItem item = invocation.getArgument(0);
                item.setItemId(302L);
                return 1;
            }).when(mdItemService).insertMdItem(any(MdItem.class));
            when(mdItemBatchConfigService.selectMdItemBatchConfigByItemId(201L)).thenReturn(null);
            when(routeProductService.copyRouteProductForSku(anyLong(), anyLong(), anyString(), anyString())).thenReturn(1);

            // when
            workorderService.createWorkorderWithBom(testWorkorder, new ArrayList<>(), new ArrayList<>());

            // then: 变体 item 的 printingReq = 工单调整值（覆盖）
            ArgumentCaptor<MdItem> itemCaptor = ArgumentCaptor.forClass(MdItem.class);
            verify(mdItemService).insertMdItem(itemCaptor.capture());
            MdItem created = itemCaptor.getValue();
            assertThat(created.getPrintingReq()).isEqualTo("彩印红绳"); // 工单调整值覆盖
            assertThat(created.getProductSize()).isEqualTo("30x20x10cm"); // 工单调整值覆盖
            assertThat(created.getParentId()).isEqualTo(201L);
            // 单位继承自父产品
            assertThat(created.getUnitOfMeasure()).isEqualTo("PCS");
        }
    }

    // ══════════════════════════════════════════════
    // 5. testCreateSkuVariant_RollbackOnFailure
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("5. 路线复制失败 → 抛异常，事务回滚")
    void testCreateSkuVariant_RollbackOnFailure() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            testWorkorder.setCreateSkuVariant(true);
            testWorkorder.setSkuCode("ZD-01-V-FAIL");

            when(mdItemService.selectMdItemById(201L)).thenReturn(parentItem);
            doAnswer(invocation -> {
                MdItem item = invocation.getArgument(0);
                item.setItemId(399L);
                return 1;
            }).when(mdItemService).insertMdItem(any(MdItem.class));
            when(mdItemBatchConfigService.selectMdItemBatchConfigByItemId(201L)).thenReturn(null);

            // 路线复制抛异常
            when(routeProductService.copyRouteProductForSku(eq(201L), eq(399L), eq("ZD-01-V-FAIL"), any()))
                    .thenThrow(new ServiceException("路线复制失败"));

            // when/then: 应抛出 ServiceException
            assertThatThrownBy(() ->
                    workorderService.createWorkorderWithBom(testWorkorder, new ArrayList<>(), new ArrayList()))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("SKU变体创建失败");
        }
    }

    // ══════════════════════════════════════════════
    // 6. testCreateSkuVariant_ParentNotFound
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("6. 父产品不存在 → 抛异常")
    void testCreateSkuVariant_ParentNotFound() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            testWorkorder.setCreateSkuVariant(true);
            testWorkorder.setSkuCode("ZD-INVALID-V1");
            testWorkorder.setProductId(9999L);

            when(mdItemService.selectMdItemById(9999L)).thenReturn(null);

            assertThatThrownBy(() ->
                    workorderService.createWorkorderWithBom(testWorkorder, new ArrayList<>(), new ArrayList<>()))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("父产品不存在");
        }
    }

    // ══════════════════════════════════════════════
    // 7. testCreateSkuVariant_BomAndParamsPassedThrough
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("7. 变体创建时 BOM 和参数正常写入")
    void testCreateSkuVariant_BomAndParamsPassedThrough() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            testWorkorder.setCreateSkuVariant(true);
            testWorkorder.setSkuCode("ZD-01-V3");
            testWorkorder.setSkuName("奔趣纸袋-黑印黄绳");

            when(mdItemService.selectMdItemById(201L)).thenReturn(parentItem);
            doAnswer(invocation -> {
                MdItem item = invocation.getArgument(0);
                item.setItemId(303L);
                return 1;
            }).when(mdItemService).insertMdItem(any(MdItem.class));
            when(mdItemBatchConfigService.selectMdItemBatchConfigByItemId(201L)).thenReturn(null);
            when(routeProductService.copyRouteProductForSku(anyLong(), anyLong(), anyString(), anyString())).thenReturn(1);
            when(workorderBomService.insertProWorkorderBom(any(ProWorkorderBom.class))).thenReturn(1);
            when(workorderParamService.insertProWorkorderParam(any(ProWorkorderParam.class))).thenReturn(1);

            // BOM 和参数
            List<ProWorkorderBom> bomList = new ArrayList<>();
            ProWorkorderBom bom = new ProWorkorderBom();
            bom.setItemId(100L);
            bom.setItemCode("INK-01");
            bom.setQuantity(new BigDecimal("0.5"));
            bomList.add(bom);

            List<ProWorkorderParam> paramList = new ArrayList<>();
            ProWorkorderParam param = new ProWorkorderParam();
            param.setTemplateId(1L);
            param.setStandardValue("100");
            paramList.add(param);

            // when
            ProWorkorder result = workorderService.createWorkorderWithBom(testWorkorder, bomList, paramList);

            // then: BOM 和参数都被写入
            verify(workorderBomService).insertProWorkorderBom(any(ProWorkorderBom.class));
            verify(workorderParamService).insertProWorkorderParam(any(ProWorkorderParam.class));
            assertThat(result.getProductId()).isEqualTo(303L);
        }
    }
}
