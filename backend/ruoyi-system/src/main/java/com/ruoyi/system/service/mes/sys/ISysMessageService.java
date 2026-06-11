package com.ruoyi.system.service.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysMessage;

/**
 * 系统消息Service接口
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface ISysMessageService
{
    public SysMessage selectSysMessageByMessageId(Long messageId);
    public List<SysMessage> selectSysMessageList(SysMessage sysMessage);
    public int insertSysMessage(SysMessage sysMessage);
    public int updateSysMessage(SysMessage sysMessage);
    public int markAsRead(Long[] messageIds);
    public int selectUnreadCount(Long userId);
    public int deleteSysMessageByMessageIds(Long[] messageIds);
    public int deleteSysMessageByMessageId(Long messageId);
}
