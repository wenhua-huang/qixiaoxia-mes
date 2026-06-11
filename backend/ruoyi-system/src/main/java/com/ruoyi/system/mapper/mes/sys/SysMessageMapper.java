package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.sys.SysMessage;

/**
 * 系统消息Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysMessageMapper
{
    public SysMessage selectSysMessageByMessageId(Long messageId);
    public List<SysMessage> selectSysMessageList(SysMessage sysMessage);
    public int insertSysMessage(SysMessage sysMessage);
    public int updateSysMessage(SysMessage sysMessage);
    public int updateSysMessageReadStatus(@Param("messageIds") Long[] messageIds, @Param("readFlag") String readFlag);
    public int selectUnreadCount(@Param("userId") Long userId);
    public int deleteSysMessageByMessageId(Long messageId);
    public int deleteSysMessageByMessageIds(Long[] messageIds);
}
