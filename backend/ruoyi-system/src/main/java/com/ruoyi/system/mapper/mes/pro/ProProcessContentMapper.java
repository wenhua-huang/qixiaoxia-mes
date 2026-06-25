package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProProcessContent;

/**
 * 工序作业内容Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProProcessContentMapper
{
    public ProProcessContent selectProProcessContentByContentId(Long contentId);

    public List<ProProcessContent> selectProProcessContentList(ProProcessContent proProcessContent);

    public List<ProProcessContent> selectProProcessContentByProcessId(Long processId);

    public int insertProProcessContent(ProProcessContent proProcessContent);

    public int updateProProcessContent(ProProcessContent proProcessContent);

    public int deleteProProcessContentByContentId(Long contentId);

    public int deleteProProcessContentByContentIds(Long[] contentIds);

    public int deleteProProcessContentByProcessId(Long processId);
}
