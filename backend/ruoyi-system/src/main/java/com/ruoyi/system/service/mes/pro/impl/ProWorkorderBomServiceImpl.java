package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderBomMapper;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.service.mes.pro.IProWorkorderBomService;

/**
 * 工单BOM组成Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProWorkorderBomServiceImpl implements IProWorkorderBomService
{
    @Autowired
    private ProWorkorderBomMapper qxxProWorkorderBomMapper;

    /**
     * 查询工单BOM组成
     *
     * @param lineId 工单BOM组成主键
     * @return 工单BOM组成
     */
    @Override
    public ProWorkorderBom selectProWorkorderBomByLineId(Long lineId)
    {
        return qxxProWorkorderBomMapper.selectProWorkorderBomByLineId(lineId);
    }

    /**
     * 查询工单BOM组成列表
     *
     * @param proWorkorderBom 工单BOM组成
     * @return 工单BOM组成
     */
    @Override
    public List<ProWorkorderBom> selectProWorkorderBomList(ProWorkorderBom proWorkorderBom)
    {
        return qxxProWorkorderBomMapper.selectProWorkorderBomList(proWorkorderBom);
    }

    /**
     * 根据工单ID查询BOM组成列表
     *
     * @param workorderId 工单ID
     * @return 工单BOM组成集合
     */
    @Override
    public List<ProWorkorderBom> selectProWorkorderBomByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderBomMapper.selectProWorkorderBomByWorkorderId(workorderId);
    }

    /**
     * 新增工单BOM组成
     *
     * @param proWorkorderBom 工单BOM组成
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProWorkorderBom(ProWorkorderBom proWorkorderBom)
    {
        proWorkorderBom.setCreateTime(DateUtils.getNowDate());
        proWorkorderBom.setCreateBy(SecurityUtils.getUsername());
        return qxxProWorkorderBomMapper.insertProWorkorderBom(proWorkorderBom);
    }

    /**
     * 修改工单BOM组成
     *
     * @param proWorkorderBom 工单BOM组成
     * @return 结果
     */
    @Override
    public int updateProWorkorderBom(ProWorkorderBom proWorkorderBom)
    {
        proWorkorderBom.setUpdateTime(DateUtils.getNowDate());
        proWorkorderBom.setUpdateBy(SecurityUtils.getUsername());
        return qxxProWorkorderBomMapper.updateProWorkorderBom(proWorkorderBom);
    }

    /**
     * 批量删除工单BOM组成
     *
     * @param lineIds 需要删除的工单BOM组成主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderBomByLineIds(Long[] lineIds)
    {
        return qxxProWorkorderBomMapper.deleteProWorkorderBomByLineIds(lineIds);
    }

    /**
     * 删除工单BOM组成信息
     *
     * @param lineId 工单BOM组成主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderBomByLineId(Long lineId)
    {
        return qxxProWorkorderBomMapper.deleteProWorkorderBomByLineId(lineId);
    }

    /**
     * 根据工单ID删除BOM组成
     *
     * @param workorderId 工单ID
     * @return 结果
     */
    @Override
    public int deleteProWorkorderBomByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderBomMapper.deleteProWorkorderBomByWorkorderId(workorderId);
    }
}
