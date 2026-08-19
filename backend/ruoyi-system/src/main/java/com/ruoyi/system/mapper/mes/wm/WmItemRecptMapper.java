package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;

public interface WmItemRecptMapper
{
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity);
    public List<WmItemRecpt> selectWmItemRecptAll();
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId);

    /**
     * 按入库单号精确查询（factory_id 由 FactoryIdInterceptor 自动注入，按工厂隔离）。
     * 用于新增时查重，防止同号重复落库。
     */
    public WmItemRecpt selectByRecptCode(@Param("recptCode") String recptCode);

    public int insertWmItemRecpt(WmItemRecpt entity);
    public int updateWmItemRecpt(WmItemRecpt entity);
    public int deleteWmItemRecptByRecptId(Long recptId);
    public int deleteWmItemRecptByRecptIds(Long[] recptIds);

    /**
     * 仅回写 IQC 挂点两列（质检工厂生成检验单后回填，两列专用 UPDATE 避免并发覆盖整头；
     * factory_id 由 FactoryIdInterceptor 自动注入）
     */
    public int updateWmItemRecptHeaderRefs(@Param("recptId") Long recptId, @Param("iqcId") Long iqcId, @Param("iqcCode") String iqcCode);
}