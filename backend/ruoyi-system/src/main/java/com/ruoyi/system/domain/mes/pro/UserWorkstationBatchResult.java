package com.ruoyi.system.domain.mes.pro;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量绑定结果统计（跳过明细为 "用户名 / 工位名"）
 */
public class UserWorkstationBatchResult
{
    private int successCount;
    private int reactivatedCount;
    private int skipCount;
    private final List<String> skips = new ArrayList<>();

    public void incSuccess() { successCount++; }
    public void incReactivated() { reactivatedCount++; }
    public void incSkip() { skipCount++; }
    public void addSkip(String detail) { skips.add(detail); }

    public int getSuccessCount() { return successCount; }
    public int getReactivatedCount() { return reactivatedCount; }
    public int getSkipCount() { return skipCount; }
    public List<String> getSkips() { return skips; }
}
