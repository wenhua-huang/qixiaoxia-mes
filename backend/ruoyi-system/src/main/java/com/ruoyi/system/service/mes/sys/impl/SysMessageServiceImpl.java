package com.ruoyi.system.service.mes.sys.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.sys.SysMessage;
import com.ruoyi.system.mapper.mes.sys.SysMessageMapper;
import com.ruoyi.system.service.mes.sys.ISysMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统消息Service实现
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Service
public class SysMessageServiceImpl implements ISysMessageService
{
    @Autowired
    private SysMessageMapper sysMessageMapper;

    @Override
    public SysMessage selectSysMessageByMessageId(Long messageId) {
        return sysMessageMapper.selectSysMessageByMessageId(messageId);
    }

    @Override
    public List<SysMessage> selectSysMessageList(SysMessage sysMessage) {
        return sysMessageMapper.selectSysMessageList(sysMessage);
    }

    @Override
    public int insertSysMessage(SysMessage sysMessage) {
        sysMessage.setCreateTime(DateUtils.getNowDate());
        sysMessage.setReadFlag("0");
        return sysMessageMapper.insertSysMessage(sysMessage);
    }

    @Override
    public int updateSysMessage(SysMessage sysMessage) {
        sysMessage.setUpdateTime(DateUtils.getNowDate());
        return sysMessageMapper.updateSysMessage(sysMessage);
    }

    @Override
    public int markAsRead(Long[] messageIds) {
        return sysMessageMapper.updateSysMessageReadStatus(messageIds, "1");
    }

    @Override
    public int selectUnreadCount(Long userId) {
        return sysMessageMapper.selectUnreadCount(userId);
    }

    @Override
    public int deleteSysMessageByMessageIds(Long[] messageIds) {
        return sysMessageMapper.deleteSysMessageByMessageIds(messageIds);
    }

    @Override
    public int deleteSysMessageByMessageId(Long messageId) {
        return sysMessageMapper.deleteSysMessageByMessageId(messageId);
    }
}
