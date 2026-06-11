package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstation;

/**
 * 工作站Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdWorkstationMapper
{
    public MdWorkstation selectMdWorkstationByWorkstationId(Long workstationId);
    public MdWorkstation checkWorkstationCodeUnique(MdWorkstation mdWorkstation);
    public List<MdWorkstation> selectMdWorkstationList(MdWorkstation mdWorkstation);
    public int insertMdWorkstation(MdWorkstation mdWorkstation);
    public int updateMdWorkstation(MdWorkstation mdWorkstation);
    public int deleteMdWorkstationByWorkstationId(Long workstationId);
    public int deleteMdWorkstationByWorkstationIds(Long[] workstationIds);
}
