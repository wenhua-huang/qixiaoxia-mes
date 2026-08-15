package com.ruoyi.system.mapper.mes.wm;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;

public interface WmRollDetailMapper
{
    public List<WmRollDetail> selectWmRollDetailList(WmRollDetail entity);
    public List<WmRollDetail> selectWmRollDetailAll();
    public WmRollDetail selectWmRollDetailByRollId(Long rollId);
    public int insertWmRollDetail(WmRollDetail entity);
    public int updateWmRollDetail(WmRollDetail entity);
    public int deleteWmRollDetailByRollId(Long rollId);
    public int deleteWmRollDetailByRollIds(Long[] rollIds);

    /**
     * 条件更新：仅当母卷当前状态为 IN_STOCK 时跃迁为 OUTSOURCED（防并发重入的乐观兜底）。
     * @return 受影响行数；0 表示状态已被其他操作变更
     */
    public int markOutsourcedIfInStock(@Param("rollId") Long rollId,
                                       @Param("operator") String operator,
                                       @Param("now") Date now);

    /**
     * 按卷料码精确反查纸卷（扫码用）。roll_code 有唯一索引；factory_id 由拦截器注入。
     */
    public WmRollDetail selectWmRollDetailByRollCode(@Param("rollCode") String rollCode);
}
