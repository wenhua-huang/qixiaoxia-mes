package com.ruoyi.system.mapper.mes.wm;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;

public interface WmIssueDetailMapper {
    WmIssueDetail selectWmIssueDetailByDetailId(Long detailId);
    List<WmIssueDetail> selectWmIssueDetailList(WmIssueDetail e);
    /** 批量按领料单ID查明明细（消除 N+1） */
    List<WmIssueDetail> selectByIssueIds(Collection<Long> issueIds);
    int insertWmIssueDetail(WmIssueDetail e);
    int updateWmIssueDetail(WmIssueDetail e);
    int deleteWmIssueDetailByDetailId(Long detailId);
    int deleteWmIssueDetailByDetailIds(Long[] detailIds);
}
