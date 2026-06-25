package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;

/**
 * WmIssueDetailService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmIssueDetailService
{
    public WmIssueDetail selectWmIssueDetailByDetailId(Long detailId);
    public List<WmIssueDetail> selectWmIssueDetailList(WmIssueDetail e);
    public List<WmIssueDetail> selectAll();
    public int insertWmIssueDetail(WmIssueDetail e);
    public int updateWmIssueDetail(WmIssueDetail e);
    public int deleteWmIssueDetailByDetailIds(Long[] detailIds);
    public int deleteWmIssueDetailByDetailId(Long detailId);
}
