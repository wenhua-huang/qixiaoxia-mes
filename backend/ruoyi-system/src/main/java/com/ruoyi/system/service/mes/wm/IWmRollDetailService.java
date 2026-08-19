package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;

public interface IWmRollDetailService
{
    public List<WmRollDetail> selectWmRollDetailList(WmRollDetail entity);
    public List<WmRollDetail> selectWmRollDetailAll();
    public WmRollDetail selectWmRollDetailByRollId(Long rollId);
    public int insertWmRollDetail(WmRollDetail entity);
    public int updateWmRollDetail(WmRollDetail entity);
    public int deleteWmRollDetailByRollId(Long rollId);
    public int deleteWmRollDetailByRollIds(Long[] rollIds);

    /**
     * 按卷料码精确反查纸卷（扫码用，roll_code 唯一）。
     *
     * @param rollCode 卷料码（会 trim）
     * @return 纸卷实体（未找到抛 ServiceException）
     */
    public WmRollDetail scanByRollCode(String rollCode);
}