package com.ruoyi.system.service.mes.sys.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.sys.SysAttachment;
import com.ruoyi.system.mapper.mes.sys.SysAttachmentMapper;
import com.ruoyi.system.service.mes.sys.ISysAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通用附件Service实现
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Service
public class SysAttachmentServiceImpl implements ISysAttachmentService
{
    @Autowired
    private SysAttachmentMapper sysAttachmentMapper;

    @Override
    public SysAttachment selectSysAttachmentByAttachmentId(Long attachmentId) {
        return sysAttachmentMapper.selectSysAttachmentByAttachmentId(attachmentId);
    }

    @Override
    public List<SysAttachment> selectSysAttachmentList(SysAttachment sysAttachment) {
        return sysAttachmentMapper.selectSysAttachmentList(sysAttachment);
    }

    @Override
    public List<SysAttachment> selectSysAttachmentBySource(Long sourceDocId, String sourceDocType) {
        return sysAttachmentMapper.selectSysAttachmentBySource(sourceDocId, sourceDocType);
    }

    @Override
    public int insertSysAttachment(SysAttachment sysAttachment) {
        sysAttachment.setCreateTime(DateUtils.getNowDate());
        return sysAttachmentMapper.insertSysAttachment(sysAttachment);
    }

    @Override
    public int updateSysAttachment(SysAttachment sysAttachment) {
        sysAttachment.setUpdateTime(DateUtils.getNowDate());
        return sysAttachmentMapper.updateSysAttachment(sysAttachment);
    }

    @Override
    public int deleteSysAttachmentByAttachmentIds(Long[] attachmentIds) {
        return sysAttachmentMapper.deleteSysAttachmentByAttachmentIds(attachmentIds);
    }

    @Override
    public int deleteSysAttachmentByAttachmentId(Long attachmentId) {
        return sysAttachmentMapper.deleteSysAttachmentByAttachmentId(attachmentId);
    }

    @Override
    public int deleteSysAttachmentBySource(Long sourceDocId, String sourceDocType) {
        return sysAttachmentMapper.deleteSysAttachmentBySource(sourceDocId, sourceDocType);
    }
}
