package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstation;

/**
 * 工作站Service接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdWorkstationService
{
    public MdWorkstation selectMdWorkstationByWorkstationId(Long workstationId);
    public boolean checkWorkstationCodeUnique(MdWorkstation mdWorkstation);
    public List<MdWorkstation> selectMdWorkstationList(MdWorkstation mdWorkstation);
        public int insertMdWorkstation(MdWorkstation mdWorkstation);    public int updateMdWorkstation(MdWorkstation mdWorkstation);
    public int deleteMdWorkstationByWorkstationIds(Long[] workstationIds);
    public int deleteMdWorkstationByWorkstationId(Long workstationId);
}
