package com.ruoyi.system.service.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAttachment;

/**
 * 通用附件Service接口
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface ISysAttachmentService
{
    public SysAttachment selectSysAttachmentByAttachmentId(Long attachmentId);
    public List<SysAttachment> selectSysAttachmentList(SysAttachment sysAttachment);
    public List<SysAttachment> selectSysAttachmentBySource(Long sourceDocId, String sourceDocType);
    public int insertSysAttachment(SysAttachment sysAttachment);
    public int updateSysAttachment(SysAttachment sysAttachment);
    public int deleteSysAttachmentByAttachmentIds(Long[] attachmentIds);
    public int deleteSysAttachmentByAttachmentId(Long attachmentId);
    public int deleteSysAttachmentBySource(Long sourceDocId, String sourceDocType);
}
