package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdClient;

/**
 * 客户Service接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdClientService
{
    public MdClient selectMdClientByClientId(Long clientId);
    public boolean checkClientCodeUnique(MdClient mdClient);
    public List<MdClient> selectMdClientList(MdClient mdClient);
    public List<MdClient> selectMdClientAllEnabled();
        public int insertMdClient(MdClient mdClient);    public int updateMdClient(MdClient mdClient);
    public int deleteMdClientByClientIds(Long[] clientIds);
    public int deleteMdClientByClientId(Long clientId);
}
