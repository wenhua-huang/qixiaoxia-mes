package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;

/**
 * 工单工序参数值Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProWorkorderParamService
{
    /**
     * 查询工单工序参数值
     *
     * @param recordId 工单工序参数值主键
     * @return 工单工序参数值
     */
    public ProWorkorderParam selectProWorkorderParamByRecordId(Long recordId);

    /**
     * 查询工单工序参数值列表
     *
     * @param proWorkorderParam 工单工序参数值
     * @return 工单工序参数值集合
     */
    public List<ProWorkorderParam> selectProWorkorderParamList(ProWorkorderParam proWorkorderParam);

    /**
     * 根据工单ID查询工序参数列表
     *
     * @param workorderId 工单ID
     * @return 工单工序参数值集合
     */
    public List<ProWorkorderParam> selectProWorkorderParamByWorkorderId(Long workorderId);

    /**
     * 新增工单工序参数值
     *
     * @param proWorkorderParam 工单工序参数值
     * @return 结果
     */
    public int insertProWorkorderParam(ProWorkorderParam proWorkorderParam);

    /**
     * 修改工单工序参数值
     *
     * @param proWorkorderParam 工单工序参数值
     * @return 结果
     */
    public int updateProWorkorderParam(ProWorkorderParam proWorkorderParam);

    /**
     * 批量删除工单工序参数值
     *
     * @param recordIds 需要删除的工单工序参数值主键集合
     * @return 结果
     */
    public int deleteProWorkorderParamByRecordIds(Long[] recordIds);

    /**
     * 删除工单工序参数值信息
     *
     * @param recordId 工单工序参数值主键
     * @return 结果
     */
    public int deleteProWorkorderParamByRecordId(Long recordId);

    /**
     * 根据工单ID删除工序参数
     *
     * @param workorderId 工单ID
     * @return 结果
     */
    public int deleteProWorkorderParamByWorkorderId(Long workorderId);

    /**
     * 从路线产品批量插入工单工序参数
     *
     * @param workorderId 工单ID
     * @param routeProductId 路线产品ID
     * @return 结果
     */
    public int batchInsertFromRouteProduct(Long workorderId, Long routeProductId);
}
