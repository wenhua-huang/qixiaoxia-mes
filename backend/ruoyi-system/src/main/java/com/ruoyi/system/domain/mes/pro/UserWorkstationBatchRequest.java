package com.ruoyi.system.domain.mes.pro;

import java.util.List;

/**
 * 用户工作站批量绑定请求：多选人员 × 多选工位（笛卡尔积逐对处理）
 */
public class UserWorkstationBatchRequest
{
    private List<Long> userIds;
    private List<Long> workstationIds;
    private String remark;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    public List<Long> getWorkstationIds() { return workstationIds; }
    public void setWorkstationIds(List<Long> workstationIds) { this.workstationIds = workstationIds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
