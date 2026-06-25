package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderParamMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessParamMapper;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;
import com.ruoyi.system.service.mes.pro.IProWorkorderParamService;

/**
 * 工单工序参数值Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProWorkorderParamServiceImpl implements IProWorkorderParamService
{
    @Autowired
    private ProWorkorderParamMapper qxxProWorkorderParamMapper;

    @Autowired
    private ProRouteProcessParamMapper qxxProRouteProcessParamMapper;

    /**
     * 查询工单工序参数值
     *
     * @param recordId 工单工序参数值主键
     * @return 工单工序参数值
     */
    @Override
    public ProWorkorderParam selectProWorkorderParamByRecordId(Long recordId)
    {
        return qxxProWorkorderParamMapper.selectProWorkorderParamByRecordId(recordId);
    }

    /**
     * 查询工单工序参数值列表
     *
     * @param proWorkorderParam 工单工序参数值
     * @return 工单工序参数值
     */
    @Override
    public List<ProWorkorderParam> selectProWorkorderParamList(ProWorkorderParam proWorkorderParam)
    {
        return qxxProWorkorderParamMapper.selectProWorkorderParamList(proWorkorderParam);
    }

    /**
     * 根据工单ID查询工序参数列表
     *
     * @param workorderId 工单ID
     * @return 工单工序参数值集合
     */
    @Override
    public List<ProWorkorderParam> selectProWorkorderParamByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderParamMapper.selectProWorkorderParamByWorkorderId(workorderId);
    }

    /**
     * 新增工单工序参数值
     *
     * @param proWorkorderParam 工单工序参数值
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProWorkorderParam(ProWorkorderParam proWorkorderParam)
    {
        proWorkorderParam.setCreateTime(DateUtils.getNowDate());
        proWorkorderParam.setCreateBy(SecurityUtils.getUsername());
        return qxxProWorkorderParamMapper.insertProWorkorderParam(proWorkorderParam);
    }

    /**
     * 修改工单工序参数值
     *
     * @param proWorkorderParam 工单工序参数值
     * @return 结果
     */
    @Override
    public int updateProWorkorderParam(ProWorkorderParam proWorkorderParam)
    {
        proWorkorderParam.setUpdateTime(DateUtils.getNowDate());
        proWorkorderParam.setUpdateBy(SecurityUtils.getUsername());
        return qxxProWorkorderParamMapper.updateProWorkorderParam(proWorkorderParam);
    }

    /**
     * 批量删除工单工序参数值
     *
     * @param recordIds 需要删除的工单工序参数值主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderParamByRecordIds(Long[] recordIds)
    {
        return qxxProWorkorderParamMapper.deleteProWorkorderParamByRecordIds(recordIds);
    }

    /**
     * 删除工单工序参数值信息
     *
     * @param recordId 工单工序参数值主键
     * @return 结果
     */
    @Override
    public int deleteProWorkorderParamByRecordId(Long recordId)
    {
        return qxxProWorkorderParamMapper.deleteProWorkorderParamByRecordId(recordId);
    }

    /**
     * 根据工单ID删除工序参数
     *
     * @param workorderId 工单ID
     * @return 结果
     */
    @Override
    public int deleteProWorkorderParamByWorkorderId(Long workorderId)
    {
        return qxxProWorkorderParamMapper.deleteProWorkorderParamByWorkorderId(workorderId);
    }

    /**
     * 从路线产品批量插入工单工序参数
     *
     * @param workorderId 工单ID
     * @param routeProductId 路线产品ID
     * @return 结果
     */
    @Override
    @Transactional
    public int batchInsertFromRouteProduct(Long workorderId, Long routeProductId)
    {
        List<ProRouteProcessParam> params = qxxProRouteProcessParamMapper.selectProRouteProcessParamByRouteProductId(routeProductId);
        if (params == null || params.isEmpty())
        {
            return 0;
        }

        int count = 0;
        for (ProRouteProcessParam rp : params)
        {
            ProWorkorderParam param = new ProWorkorderParam();
            param.setWorkorderId(workorderId);
            param.setRouteProductId(routeProductId);
            param.setTemplateId(rp.getTemplateId());
            param.setStandardValue(rp.getParamValue());
            param.setCreateTime(DateUtils.getNowDate());
            param.setCreateBy(SecurityUtils.getUsername());
            qxxProWorkorderParamMapper.insertProWorkorderParam(param);
            count++;
        }
        return count;
    }
}
