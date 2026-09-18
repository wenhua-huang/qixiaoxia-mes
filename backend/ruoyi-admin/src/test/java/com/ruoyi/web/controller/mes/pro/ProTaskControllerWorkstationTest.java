package com.ruoyi.web.controller.mes.pro;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.service.mes.pro.IProTaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务机台入参校验：外协 VENDOR 占位（workstationId=0 + code=VENDOR）不占厂内机台，
 * 手工新增/编辑外协任务必须放行，与下发/报工等机台闸门口径一致。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("生产任务机台校验 — 外协 VENDOR 占位放行")
class ProTaskControllerWorkstationTest
{
    @Mock
    private IProTaskService proTaskService;

    @InjectMocks
    private ProTaskController controller;

    private ProTask vendorTask()
    {
        ProTask t = new ProTask();
        t.setWorkstationId(0L);
        t.setWorkstationCode("VENDOR");
        t.setWorkstationName("外协");
        return t;
    }

    @Test
    @DisplayName("edit：外协 VENDOR 占位(id=0)放行，正常更新")
    void edit_vendorPlaceholder_passes()
    {
        ProTask t = vendorTask();
        t.setTaskId(549L);
        when(proTaskService.updateProTask(any())).thenReturn(1);

        AjaxResult result = controller.edit(t);

        assertThat(result.get("code")).isEqualTo(200);
        verify(proTaskService).updateProTask(t);
    }

    @Test
    @DisplayName("edit：厂内任务传 id=0 且无 VENDOR 标记，拦截")
    void edit_inhouseWithZeroId_blocked()
    {
        ProTask t = new ProTask();
        t.setTaskId(1L);
        t.setWorkstationId(0L);

        AjaxResult result = controller.edit(t);

        assertThat(result.get("msg")).isEqualTo("请选择机台");
        verify(proTaskService, never()).updateProTask(any());
    }

    @Test
    @DisplayName("edit：部分更新不传机台(id=null)放行，保留原机台")
    void edit_nullWorkstation_passes()
    {
        ProTask t = new ProTask();
        t.setTaskId(1L);
        when(proTaskService.updateProTask(any())).thenReturn(1);

        AjaxResult result = controller.edit(t);

        assertThat(result.get("code")).isEqualTo(200);
        verify(proTaskService).updateProTask(t);
    }

    @Test
    @DisplayName("add：外协 VENDOR 占位(id=0)放行，正常新增")
    void add_vendorPlaceholder_passes()
    {
        ProTask t = vendorTask();
        t.setTaskCode("TASK-TEST-VENDOR");
        t.setQuantity(new BigDecimal("3"));

        AjaxResult result = controller.add(t);

        assertThat(result.get("code")).isEqualTo(200);
        verify(proTaskService).insertProTask(t);
    }

    @Test
    @DisplayName("add：无机台且无 VENDOR 标记，拦截")
    void add_withoutWorkstation_blocked()
    {
        ProTask t = new ProTask();
        t.setTaskCode("TASK-TEST-NO-WS");
        t.setQuantity(new BigDecimal("3"));

        AjaxResult result = controller.add(t);

        assertThat(result.get("msg")).isEqualTo("请选择机台");
        verify(proTaskService, never()).insertProTask(any());
    }
}
