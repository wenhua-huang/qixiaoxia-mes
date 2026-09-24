package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.mes.pro.ProException;
import com.ruoyi.system.domain.mes.pro.ProExceptionConstants;
import com.ruoyi.system.mapper.mes.pro.ProExceptionMapper;
import com.ruoyi.system.service.mes.pro.IProExceptionBlockService;

/**
 * 生产异常完工硬拦实现：未关闭异常计数 &gt; 0 即禁止工单自动完工（E5）。
 *
 * @author qixiaoxia
 */
@Service
public class ProExceptionBlockServiceImpl implements IProExceptionBlockService
{
    @Autowired
    private ProExceptionMapper proExceptionMapper;

    @Override
    public void assertCompletable(Long workorderId)
    {
        if (workorderId == null)
        {
            return;
        }
        List<ProException> open = proExceptionMapper.selectOpenByWorkorderId(workorderId);
        if (open == null || open.isEmpty())
        {
            return;
        }
        throw new ServiceException(buildBlockMessage(open));
    }

    @Override
    public List<ProException> listOpenByWorkorder(Long workorderId)
    {
        if (workorderId == null)
        {
            return List.of();
        }
        List<ProException> open = proExceptionMapper.selectOpenByWorkorderId(workorderId);
        return open == null ? List.of() : open;
    }

    @Override
    public Map<String, Map<String, Object>> openState(List<Long> workorderIds)
    {
        if (workorderIds == null || workorderIds.isEmpty())
        {
            return Map.of();
        }
        List<Long> distinct = workorderIds.stream()
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinct.isEmpty())
        {
            return Map.of();
        }
        if (distinct.size() > ProExceptionConstants.BATCH_STATE_MAX)
        {
            throw new ServiceException("单次最多查询 " + ProExceptionConstants.BATCH_STATE_MAX + " 个工单的异常态");
        }
        Map<Long, List<ProException>> grouped = proExceptionMapper.selectOpenByWorkorderIds(distinct)
                .stream().collect(Collectors.groupingBy(ProException::getWorkorderId));

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Long workorderId : distinct)
        {
            result.put(String.valueOf(workorderId), stateOf(grouped.get(workorderId)));
        }
        return result;
    }

    private Map<String, Object> stateOf(List<ProException> open)
    {
        int count = open == null ? 0 : open.size();
        List<String> codes = new ArrayList<>();
        if (open != null)
        {
            open.stream().limit(ProExceptionConstants.BLOCK_CODE_SAMPLE)
                    .map(ProException::getExceptionCode).forEach(codes::add);
        }
        Map<String, Object> state = new LinkedHashMap<>(2);
        state.put("openCount", count);
        state.put("sampleCodes", codes);
        return state;
    }

    private String buildBlockMessage(List<ProException> open)
    {
        String codes = open.stream().limit(ProExceptionConstants.BLOCK_CODE_SAMPLE)
                .map(ProException::getExceptionCode).collect(Collectors.joining("、"));
        String tail = open.size() > ProExceptionConstants.BLOCK_CODE_SAMPLE ? " 等" : "";
        return "工单无法完工：还有 " + open.size() + " 条异常未关闭（" + codes + tail + "），请先处理关闭";
    }
}
