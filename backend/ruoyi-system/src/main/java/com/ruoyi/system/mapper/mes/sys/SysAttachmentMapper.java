package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import com.ruoyi.system.domain.mes.sys.SysAttachment;

/**
 * 通用附件Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysAttachmentMapper
{
    public SysAttachment selectSysAttachmentByAttachmentId(Long attachmentId);
    public List<SysAttachment> selectSysAttachmentList(SysAttachment sysAttachment);
    public List<SysAttachment> selectSysAttachmentBySource(Long sourceDocId, String sourceDocType);
    public int insertSysAttachment(SysAttachment sysAttachment);
    public int updateSysAttachment(SysAttachment sysAttachment);
    public int deleteSysAttachmentByAttachmentId(Long attachmentId);
    public int deleteSysAttachmentByAttachmentIds(Long[] attachmentIds);
    public int deleteSysAttachmentBySource(Long sourceDocId, String sourceDocType);
}
