package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmTransfer;

public interface IWmTransferService
{
    public List<WmTransfer> selectWmTransferList(WmTransfer entity);
    public List<WmTransfer> selectWmTransferAll();
    public WmTransfer selectWmTransferByTransferId(Long transferId);
    public int insertWmTransfer(WmTransfer entity);
    public int updateWmTransfer(WmTransfer entity);
    public int deleteWmTransferByTransferId(Long transferId);
    public int deleteWmTransferByTransferIds(Long[] transferIds);
}