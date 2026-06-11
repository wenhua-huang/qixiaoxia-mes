package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkshop;

/**
 * 车间管理Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdWorkshopMapper
{
    public List<MdWorkshop> selectMdWorkshopList(MdWorkshop mdWorkshop);

    public MdWorkshop selectMdWorkshopById(Long workshopId);

    public MdWorkshop checkWorkshopCodeUnique(MdWorkshop mdWorkshop);

    public int insertMdWorkshop(MdWorkshop mdWorkshop);

    public int updateMdWorkshop(MdWorkshop mdWorkshop);

    public int deleteMdWorkshopById(Long workshopId);

    public int deleteMdWorkshopByIds(Long[] workshopIds);
}
