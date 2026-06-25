package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssueDetail;

/**
 * WmRtIssueDetailService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmRtIssueDetailService
{
    public WmRtIssueDetail selectWmRtIssueDetailByDetailId(Long detailId);
    public List<WmRtIssueDetail> selectWmRtIssueDetailList(WmRtIssueDetail e);
    public List<WmRtIssueDetail> selectAll();
    public int insertWmRtIssueDetail(WmRtIssueDetail e);
    public int updateWmRtIssueDetail(WmRtIssueDetail e);
    public int deleteWmRtIssueDetailByDetailIds(Long[] detailIds);
    public int deleteWmRtIssueDetailByDetailId(Long detailId);
}
