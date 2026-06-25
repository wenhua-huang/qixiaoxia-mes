package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssueDetail;

public interface WmRtIssueDetailMapper {
    WmRtIssueDetail selectWmRtIssueDetailByDetailId(Long detailId);
    List<WmRtIssueDetail> selectWmRtIssueDetailList(WmRtIssueDetail e);
    int insertWmRtIssueDetail(WmRtIssueDetail e);
    int updateWmRtIssueDetail(WmRtIssueDetail e);
    int deleteWmRtIssueDetailByDetailId(Long detailId);
    int deleteWmRtIssueDetailByDetailIds(Long[] detailIds);
}
