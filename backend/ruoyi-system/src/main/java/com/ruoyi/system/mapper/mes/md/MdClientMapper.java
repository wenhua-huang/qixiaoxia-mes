package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdClient;

/**
 * 客户Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdClientMapper
{
    public MdClient selectMdClientByClientId(Long clientId);
    public MdClient checkClientCodeUnique(MdClient mdClient);
    public List<MdClient> selectMdClientList(MdClient mdClient);
    public List<MdClient> selectMdClientAllEnabled();
    public int insertMdClient(MdClient mdClient);
    public int updateMdClient(MdClient mdClient);
    public int deleteMdClientByClientId(Long clientId);
    public int deleteMdClientByClientIds(Long[] clientIds);
}
