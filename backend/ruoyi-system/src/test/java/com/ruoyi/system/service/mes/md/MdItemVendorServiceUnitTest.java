package com.ruoyi.system.service.mes.md;

import java.math.BigDecimal;
import java.util.*;

import com.ruoyi.system.domain.mes.md.MdItemVendor;
import com.ruoyi.system.mapper.mes.md.MdItemVendorMapper;
import com.ruoyi.system.service.mes.md.impl.MdItemVendorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 物料供应商关系 Service 单元测试
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("物料供应商关系服务单元测试")
class MdItemVendorServiceUnitTest {

    @Mock private MdItemVendorMapper mapper;
    @InjectMocks private MdItemVendorServiceImpl service;

    private MdItemVendor testRecord;

    @BeforeEach
    void setUp() {
        testRecord = new MdItemVendor();
        testRecord.setRecordId(1L);
        testRecord.setItemId(100L); testRecord.setItemCode("MAT-001"); testRecord.setItemName("白牛皮");
        testRecord.setVendorId(200L); testRecord.setVendorCode("V-001"); testRecord.setVendorName("圣享纸业");
        testRecord.setIsPreferred("Y");
        testRecord.setMinOrderQty(new BigDecimal("100"));
        testRecord.setLeadTimeDays(7);
    }

    @Test @DisplayName("按ID查询")
    void should_selectById() {
        when(mapper.selectByRecordId(1L)).thenReturn(testRecord);
        MdItemVendor r = service.selectByRecordId(1L);
        assertThat(r).isNotNull();
        assertThat(r.getItemCode()).isEqualTo("MAT-001");
    }

    @Test @DisplayName("按供应商ID查询")
    void should_selectByVendorId() {
        when(mapper.selectByVendorId(200L)).thenReturn(Collections.singletonList(testRecord));
        List<MdItemVendor> list = service.selectByVendorId(200L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getVendorName()).isEqualTo("圣享纸业");
    }

    @Test @DisplayName("按物料ID查询")
    void should_selectByItemId() {
        when(mapper.selectByItemId(100L)).thenReturn(Collections.singletonList(testRecord));
        List<MdItemVendor> list = service.selectByItemId(100L);
        assertThat(list).hasSize(1);
    }

    @Test @DisplayName("新增物料供应商关系")
    void should_insert() {
        when(mapper.insert(any(MdItemVendor.class))).thenReturn(1);
        MdItemVendor iv = new MdItemVendor(); iv.setItemId(101L); iv.setVendorId(201L);
        int rows = service.insert(iv);
        assertThat(rows).isEqualTo(1);
        verify(mapper).insert(any(MdItemVendor.class));
    }

    @Test @DisplayName("更新首选供应商")
    void should_updatePreferred() {
        when(mapper.update(any(MdItemVendor.class))).thenReturn(1);
        MdItemVendor iv = new MdItemVendor(); iv.setRecordId(1L); iv.setIsPreferred("Y");
        int rows = service.update(iv);
        assertThat(rows).isEqualTo(1);
    }

    @Test @DisplayName("批量删除")
    void should_deleteByIds() {
        when(mapper.deleteByRecordIds(any(Long[].class))).thenReturn(2);
        int rows = service.deleteByRecordIds(new Long[]{1L, 2L});
        assertThat(rows).isEqualTo(2);
    }
}
