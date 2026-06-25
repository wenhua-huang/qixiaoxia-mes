package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;

/**
 * 工单BOM组成Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProWorkorderBomService
{
    /**
     * 查询工单BOM组成
     *
     * @param lineId 工单BOM组成主键
     * @return 工单BOM组成
     */
    public ProWorkorderBom selectProWorkorderBomByLineId(Long lineId);

    /**
     * 查询工单BOM组成列表
     *
     * @param proWorkorderBom 工单BOM组成
     * @return 工单BOM组成集合
     */
    public List<ProWorkorderBom> selectProWorkorderBomList(ProWorkorderBom proWorkorderBom);

    /**
     * 根据工单ID查询BOM组成列表
     *
     * @param workorderId 工单ID
     * @return 工单BOM组成集合
     */
    public List<ProWorkorderBom> selectProWorkorderBomByWorkorderId(Long workorderId);

    /**
     * 新增工单BOM组成
     *
     * @param proWorkorderBom 工单BOM组成
     * @return 结果
     */
    public int insertProWorkorderBom(ProWorkorderBom proWorkorderBom);

    /**
     * 修改工单BOM组成
     *
     * @param proWorkorderBom 工单BOM组成
     * @return 结果
     */
    public int updateProWorkorderBom(ProWorkorderBom proWorkorderBom);

    /**
     * 批量删除工单BOM组成
     *
     * @param lineIds 需要删除的工单BOM组成主键集合
     * @return 结果
     */
    public int deleteProWorkorderBomByLineIds(Long[] lineIds);

    /**
     * 删除工单BOM组成信息
     *
     * @param lineId 工单BOM组成主键
     * @return 结果
     */
    public int deleteProWorkorderBomByLineId(Long lineId);

    /**
     * 根据工单ID删除BOM组成
     *
     * @param workorderId 工单ID
     * @return 结果
     */
    public int deleteProWorkorderBomByWorkorderId(Long workorderId);
}
