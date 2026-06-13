package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmTransferDetail;

public interface IWmTransferDetailService
{
    public List<WmTransferDetail> selectWmTransferDetailList(WmTransferDetail entity);
    public List<WmTransferDetail> selectWmTransferDetailAll();
    public WmTransferDetail selectWmTransferDetailByDetailId(Long detailId);
    public int insertWmTransferDetail(WmTransferDetail entity);
    public int updateWmTransferDetail(WmTransferDetail entity);
    public int deleteWmTransferDetailByDetailId(Long detailId);
    public int deleteWmTransferDetailByDetailIds(Long[] detailIds);
}