package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProProcessContentMapper;
import com.ruoyi.system.domain.mes.pro.ProProcessContent;
import com.ruoyi.system.service.mes.pro.IProProcessContentService;

/**
 * 工序作业内容Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProProcessContentServiceImpl implements IProProcessContentService
{
    @Autowired
    private ProProcessContentMapper qxxProProcessContentMapper;

    @Override
    public ProProcessContent selectProProcessContentByContentId(Long contentId)
    {
        return qxxProProcessContentMapper.selectProProcessContentByContentId(contentId);
    }

    @Override
    public List<ProProcessContent> selectProProcessContentList(ProProcessContent proProcessContent)
    {
        return qxxProProcessContentMapper.selectProProcessContentList(proProcessContent);
    }

    @Override
    public List<ProProcessContent> selectProProcessContentByProcessId(Long processId)
    {
        return qxxProProcessContentMapper.selectProProcessContentByProcessId(processId);
    }

    @Override
    @Transactional
    public int insertProProcessContent(ProProcessContent proProcessContent)
    {
        proProcessContent.setCreateTime(DateUtils.getNowDate());
        proProcessContent.setCreateBy(SecurityUtils.getUsername());
        if (proProcessContent.getIsCheck() == null) {
            proProcessContent.setIsCheck("N");
        }
        return qxxProProcessContentMapper.insertProProcessContent(proProcessContent);
    }

    @Override
    public int updateProProcessContent(ProProcessContent proProcessContent)
    {
        proProcessContent.setUpdateTime(DateUtils.getNowDate());
        proProcessContent.setUpdateBy(SecurityUtils.getUsername());
        return qxxProProcessContentMapper.updateProProcessContent(proProcessContent);
    }

    @Override
    public int deleteProProcessContentByContentIds(Long[] contentIds)
    {
        return qxxProProcessContentMapper.deleteProProcessContentByContentIds(contentIds);
    }

    @Override
    public int deleteProProcessContentByContentId(Long contentId)
    {
        return qxxProProcessContentMapper.deleteProProcessContentByContentId(contentId);
    }

    @Override
    public int deleteProProcessContentByProcessId(Long processId)
    {
        return qxxProProcessContentMapper.deleteProProcessContentByProcessId(processId);
    }
}
