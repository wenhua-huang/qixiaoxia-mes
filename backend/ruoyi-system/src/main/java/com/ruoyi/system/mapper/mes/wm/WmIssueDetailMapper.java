package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;

public interface WmIssueDetailMapper {
    WmIssueDetail selectWmIssueDetailByDetailId(Long detailId);
    List<WmIssueDetail> selectWmIssueDetailList(WmIssueDetail e);
    int insertWmIssueDetail(WmIssueDetail e);
    int updateWmIssueDetail(WmIssueDetail e);
    int deleteWmIssueDetailByDetailId(Long detailId);
    int deleteWmIssueDetailByDetailIds(Long[] detailIds);
}
