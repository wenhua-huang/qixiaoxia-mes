package com.ruoyi.system.service.mes.wm;

import com.ruoyi.common.enums.WmWarehouseConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.mapper.mes.wm.WmWarehouseMapper;
import com.ruoyi.system.service.mes.wm.impl.WmWarehouseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 仓库归属归一化单元测试：normalizeOwnership
 * 覆盖三种归属分支 + 归属转换时清残留归属（Known Issue 1 回归锁）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("仓库归属归一化")
class WmWarehouseOwnerNormalizeTest {

    @Mock
    private WmWarehouseMapper wmWarehouseMapper;
    @InjectMocks
    private WmWarehouseServiceImpl service;

    private void normalize(WmWarehouse w) {
        ReflectionTestUtils.invokeMethod(service, "normalizeOwnership", w);
    }

    @Test
    @DisplayName("客户仓：必填 clientId，vendorId 被清空")
    void customer_setsClient_clearsVendor() {
        WmWarehouse w = new WmWarehouse();
        w.setOwnershipType(WmWarehouseConstants.OWNER_CUSTOMER);
        w.setClientId(201L);
        w.setVendorId(999L);  // 模拟旧数据残留

        normalize(w);

        assertThat(w.getClientId()).isEqualTo(201L);
        assertThat(w.getVendorId()).isNull();
    }

    @Test
    @DisplayName("客户仓：缺 clientId 抛异常")
    void customer_missingClient_throws() {
        WmWarehouse w = new WmWarehouse();
        w.setOwnershipType(WmWarehouseConstants.OWNER_CUSTOMER);

        assertThatThrownBy(() -> normalize(w))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("客户仓必须填写归属客户");
    }

    @Test
    @DisplayName("供应商仓：必填 vendorId，clientId 被清空")
    void supplier_setsVendor_clearsClient() {
        WmWarehouse w = new WmWarehouse();
        w.setOwnershipType(WmWarehouseConstants.OWNER_SUPPLIER);
        w.setVendorId(202L);
        w.setClientId(888L);  // 模拟旧数据残留

        normalize(w);

        assertThat(w.getVendorId()).isEqualTo(202L);
        assertThat(w.getClientId()).isNull();
    }

    @Test
    @DisplayName("供应商仓：缺 vendorId 抛异常")
    void supplier_missingVendor_throws() {
        WmWarehouse w = new WmWarehouse();
        w.setOwnershipType(WmWarehouseConstants.OWNER_SUPPLIER);

        assertThatThrownBy(() -> normalize(w))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("供应商仓必须填写归属供应商");
    }

    @Test
    @DisplayName("公共仓：clientId/vendorId 都被清空（归属转换回公共仓不残留）")
    void public_clearsBoth() {
        WmWarehouse w = new WmWarehouse();
        w.setOwnershipType(WmWarehouseConstants.OWNER_PUBLIC);
        w.setClientId(201L);
        w.setVendorId(202L);

        normalize(w);

        assertThat(w.getClientId()).isNull();
        assertThat(w.getVendorId()).isNull();
    }

    @Test
    @DisplayName("归属类型为 null 时默认 PUBLIC，清空 clientId/vendorId")
    void nullOwnership_defaultsToPublic() {
        WmWarehouse w = new WmWarehouse();
        w.setClientId(201L);
        w.setVendorId(202L);

        normalize(w);

        assertThat(w.getOwnershipType()).isEqualTo(WmWarehouseConstants.OWNER_PUBLIC);
        assertThat(w.getClientId()).isNull();
        assertThat(w.getVendorId()).isNull();
    }

    @Test
    @DisplayName("归属从 CUSTOMER 转 SUPPLIER：旧 clientId 被清，新 vendorId 保留")
    void convertCustomerToSupplier_clearsStaleClient() {
        WmWarehouse w = new WmWarehouse();
        w.setOwnershipType(WmWarehouseConstants.OWNER_SUPPLIER);
        w.setVendorId(202L);
        w.setClientId(201L);  // 转换前残留

        normalize(w);

        assertThat(w.getVendorId()).isEqualTo(202L);
        assertThat(w.getClientId()).isNull();
    }
}
