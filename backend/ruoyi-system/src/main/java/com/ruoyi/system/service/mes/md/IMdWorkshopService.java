package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkshop;

/**
 * 车间管理Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdWorkshopService
{
    public List<MdWorkshop> selectMdWorkshopList(MdWorkshop mdWorkshop);

    public MdWorkshop selectMdWorkshopById(Long workshopId);

    public boolean checkWorkshopCodeUnique(MdWorkshop mdWorkshop);
        public int insertMdWorkshop(MdWorkshop mdWorkshop);

    public int updateMdWorkshop(MdWorkshop mdWorkshop);

    public int deleteMdWorkshopById(Long workshopId);

    public int deleteMdWorkshopByIds(Long[] workshopIds);
}
