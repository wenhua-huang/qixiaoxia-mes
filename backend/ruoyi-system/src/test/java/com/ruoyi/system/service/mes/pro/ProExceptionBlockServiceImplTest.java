package com.ruoyi.system.service.mes.pro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.pro.ProException;
import com.ruoyi.system.mapper.mes.pro.ProExceptionMapper;
import com.ruoyi.system.service.mes.pro.impl.ProExceptionBlockServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异常完工硬拦服务单元测试（E5）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("异常完工硬拦服务")
class ProExceptionBlockServiceImplTest {

    @Mock
    private ProExceptionMapper proExceptionMapper;

    @InjectMocks
    private ProExceptionBlockServiceImpl blockService;

    @Test
    @DisplayName("无未关闭异常时放行完工")
    void should_pass_when_no_open_exception() {
        when(proExceptionMapper.selectOpenByWorkorderId(1L)).thenReturn(List.of());

        assertThatCode(() -> blockService.assertCompletable(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("存在 1 条未关闭异常时硬拦，文案带异常单号")
    void should_block_when_one_open_exception() {
        when(proExceptionMapper.selectOpenByWorkorderId(1L)).thenReturn(List.of(exception("EX-001")));

        assertThatThrownBy(() -> blockService.assertCompletable(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("1 条异常")
                .hasMessageContaining("EX-001");
    }

    @Test
    @DisplayName("存在多条未关闭异常时硬拦，文案带数量、示例与等字")
    void should_block_when_many_open_exceptions() {
        List<ProException> open = new ArrayList<>();
        for (int i = 1; i <= 5; i++) open.add(exception("EX-00" + i));
        when(proExceptionMapper.selectOpenByWorkorderId(1L)).thenReturn(open);

        assertThatThrownBy(() -> blockService.assertCompletable(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("5 条异常")
                .hasMessageContaining("EX-001")
                .hasMessageContaining("EX-003")
                .hasMessageContaining("等")
                .hasMessageNotContaining("EX-004");
    }

    @Test
    @DisplayName("openState 按工单分组返回计数与单号样例")
    void should_group_open_state_by_workorder() {
        ProException ex2 = exception("EX-002");
        ex2.setWorkorderId(2L);
        when(proExceptionMapper.selectOpenByWorkorderIds(anyList()))
                .thenReturn(List.of(exception("EX-001"), ex2));

        Map<String, Map<String, Object>> state = blockService.openState(List.of(1L, 2L));

        @SuppressWarnings("unchecked")
        List<String> codes1 = (List<String>) state.get("1").get("sampleCodes");
        assertThat(state.get("1").get("openCount")).isEqualTo(1);
        assertThat(codes1).containsExactly("EX-001");
        assertThat(state.get("2").get("openCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("超过批量上限抛业务异常")
    void should_reject_oversized_batch() {
        List<Long> ids = new ArrayList<>();
        for (long i = 0; i < 101; i++) ids.add(i);
        assertThatThrownBy(() -> blockService.openState(ids))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("openState：null 工单过滤、重复工单只查一次且结果不出现 null 键")
    void should_skip_null_and_dedupe_workorder_ids() {
        ArgumentCaptor<List<Long>> cap = ArgumentCaptor.forClass(List.class);
        when(proExceptionMapper.selectOpenByWorkorderIds(anyList()))
                .thenReturn(List.of());

        Map<String, Map<String, Object>> state =
                blockService.openState(Arrays.asList(1L, null, 1L));

        verify(proExceptionMapper).selectOpenByWorkorderIds(cap.capture());
        assertThat(cap.getValue()).containsExactly(1L);
        assertThat(state.keySet()).containsExactly("1");
    }

    private ProException exception(String code) {
        ProException ex = new ProException();
        ex.setExceptionCode(code);
        ex.setWorkorderId(1L);
        return ex;
    }
}
