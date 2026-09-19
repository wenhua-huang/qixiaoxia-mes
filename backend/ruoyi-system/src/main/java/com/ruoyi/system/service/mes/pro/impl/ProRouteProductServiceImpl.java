package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProRouteMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessParamMapper;
import com.ruoyi.system.domain.mes.pro.ProRoute;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProRouteProductBom;
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;
import com.ruoyi.system.service.mes.pro.IProRouteProductService;

@Service
public class ProRouteProductServiceImpl implements IProRouteProductService
{
    private static final String YES = "Y";

    @Autowired
    private ProRouteMapper qxxProRouteMapper;
    @Autowired
    private ProRouteProcessMapper qxxProRouteProcessMapper;
    @Autowired
    private ProRouteProductMapper qxxProRouteProductMapper;
    @Autowired
    private ProRouteProductBomMapper qxxProRouteProductBomMapper;
    @Autowired
    private ProRouteProcessParamMapper qxxProRouteProcessParamMapper;

    @Override
    public ProRouteProduct selectProRouteProductByRecordId(Long recordId) {
        return qxxProRouteProductMapper.selectProRouteProductByRecordId(recordId);
    }

    @Override
    public List<ProRouteProduct> selectProRouteProductList(ProRouteProduct p) {
        return qxxProRouteProductMapper.selectProRouteProductList(p);
    }

    @Override
    public List<ProRouteProduct> selectProRouteProductByRouteId(Long routeId) {
        return qxxProRouteProductMapper.selectProRouteProductByRouteId(routeId);
    }

    @Override
    @Transactional
    public int insertProRouteProduct(ProRouteProduct p) {
        validateSingleDefault(p);
        p.setCreateTime(DateUtils.getNowDate());
        p.setCreateBy(SecurityUtils.getUsername());
        return guardDefaultUnique(() -> qxxProRouteProductMapper.insertProRouteProduct(p), p);
    }

    @Override
    public int updateProRouteProduct(ProRouteProduct p) {
        validateSingleDefault(p);
        p.setUpdateTime(DateUtils.getNowDate());
        p.setUpdateBy(SecurityUtils.getUsername());
        return guardDefaultUnique(() -> qxxProRouteProductMapper.updateProRouteProduct(p), p);
    }

    /** 并发下 check-then-insert 同时通过时由 uk_route_product_default 兜底, 转成业务异常 */
    private int guardDefaultUnique(java.util.function.IntSupplier action, ProRouteProduct p) {
        try {
            return action.getAsInt();
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ServiceException("产品[" + p.getItemName() + "]已存在默认路线，同产品只能设一条默认路线");
        }
    }

    /** 同一产品至多一条 is_default='Y'，保证默认路线解析结果确定 */
    private void validateSingleDefault(ProRouteProduct p) {
        if (!YES.equals(p.getIsDefault()) || p.getItemId() == null) return;
        ProRouteProduct query = new ProRouteProduct();
        query.setItemId(p.getItemId());
        List<ProRouteProduct> siblings = qxxProRouteProductMapper.selectProRouteProductList(query);
        for (ProRouteProduct sib : siblings) {
            if (YES.equals(sib.getIsDefault()) && !sib.getRecordId().equals(p.getRecordId())) {
                throw new ServiceException("产品[" + p.getItemName() + "]已存在默认路线，同产品只能设一条默认路线");
            }
        }
    }

    @Override
    public int deleteProRouteProductByRecordIds(Long[] ids) {
        return qxxProRouteProductMapper.deleteProRouteProductByRecordIds(ids);
    }

    @Override
    public int deleteProRouteProductByRecordId(Long id) {
        return qxxProRouteProductMapper.deleteProRouteProductByRecordId(id);
    }

    @Override
    public int deleteProRouteProductByRouteId(Long routeId) {
        return qxxProRouteProductMapper.deleteProRouteProductByRouteId(routeId);
    }

    /**
     * 为SKU变体创建独立的工艺路线（新route + 新route_process + 新route_product + BOM + 参数）
     * 变体不与父产品共享路线，确保后续各自独立维护。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyRouteProductForSku(Long parentItemId, Long skuItemId, String skuItemCode, String skuItemName) {
        int count = 0;
        ProRouteProduct query = new ProRouteProduct();
        query.setItemId(parentItemId);
        List<ProRouteProduct> parentList = qxxProRouteProductMapper.selectProRouteProductList(query);

        for (ProRouteProduct parent : parentList) {
            Long parentRouteId = parent.getRouteId();

            // ★ 1. 复制路线本身（创建变体专属路线）
            ProRoute parentRoute = qxxProRouteMapper.selectProRouteByRouteId(parentRouteId);
            ProRoute newRoute = new ProRoute();
            // 如果父产品有多条路线，用索引区分：RT-skuCode-1, RT-skuCode-2...
            newRoute.setRouteCode("RT-" + skuItemCode + "-" + (parentList.indexOf(parent) + 1));
            newRoute.setRouteName((skuItemName != null ? skuItemName : skuItemCode) + " 工艺路线" +
                (parentList.size() > 1 ? " (" + (parentList.indexOf(parent) + 1) + ")" : ""));
            newRoute.setEnableFlag("1");
            newRoute.setRemark("SKU变体自动创建，源自路线#" + parentRouteId);
            newRoute.setCreateTime(DateUtils.getNowDate());
            newRoute.setCreateBy(SecurityUtils.getUsername());
            qxxProRouteMapper.insertProRoute(newRoute);
            Long newRouteId = newRoute.getRouteId();

            // ★ 2. 复制路线工序
            List<ProRouteProcess> parentProcesses = qxxProRouteProcessMapper.selectProRouteProcessByRouteId(parentRouteId);
            if (parentProcesses != null) {
                for (ProRouteProcess pp : parentProcesses) {
                    ProRouteProcess newProc = new ProRouteProcess();
                    newProc.setRouteId(newRouteId);
                    newProc.setProcessId(pp.getProcessId());
                    newProc.setProcessCode(pp.getProcessCode());
                    newProc.setProcessName(pp.getProcessName());
                    newProc.setOrderNum(pp.getOrderNum());
                    newProc.setLinkType(pp.getLinkType());
                    newProc.setKeyFlag(pp.getKeyFlag());
                    newProc.setIsCheck(pp.getIsCheck());
                    newProc.setNextProcessId(pp.getNextProcessId());
                    // 外发节点标志与外协厂冗余四列必须整体复制, 否则变体路线在 B2 外发硬阻断下不可用
                    newProc.setIsOutsource(pp.getIsOutsource());
                    newProc.setVendorId(pp.getVendorId());
                    newProc.setVendorCode(pp.getVendorCode());
                    newProc.setVendorName(pp.getVendorName());
                    newProc.setOutsourceFactoryId(pp.getOutsourceFactoryId());
                    newProc.setCreateTime(DateUtils.getNowDate());
                    newProc.setCreateBy(SecurityUtils.getUsername());
                    qxxProRouteProcessMapper.insertProRouteProcess(newProc);
                }
            }

            // ★ 3. 创建变体专属路线产品关联（使用新路线ID）
            ProRouteProduct skuRouteProduct = new ProRouteProduct();
            skuRouteProduct.setRouteId(newRouteId);
            skuRouteProduct.setItemId(skuItemId);
            skuRouteProduct.setItemCode(skuItemCode);
            skuRouteProduct.setItemName(skuItemName);
            skuRouteProduct.setSpecification(parent.getSpecification());
            skuRouteProduct.setUnitOfMeasure(parent.getUnitOfMeasure());
            skuRouteProduct.setUnitName(parent.getUnitName());
            skuRouteProduct.setQuantity(parent.getQuantity());
            skuRouteProduct.setProductionTime(parent.getProductionTime());
            skuRouteProduct.setTimeUnitType(parent.getTimeUnitType());
            // 继承父绑定的四维标签, 变体才能在相同订单维度下命中自己的路线(item 不同, 不触唯一索引)
            skuRouteProduct.setApplyOrderType(parent.getApplyOrderType());
            skuRouteProduct.setApplyOutsource(parent.getApplyOutsource());
            skuRouteProduct.setApplyPackage(parent.getApplyPackage());
            skuRouteProduct.setIsDefault(parent.getIsDefault());
            skuRouteProduct.setCreateTime(DateUtils.getNowDate());
            skuRouteProduct.setCreateBy(SecurityUtils.getUsername());
            qxxProRouteProductMapper.insertProRouteProduct(skuRouteProduct);
            Long newRecordId = skuRouteProduct.getRecordId();
            count++;

            // 4. 复制 BOM（指向新路线ID）
            ProRouteProductBom bomQuery = new ProRouteProductBom();
            bomQuery.setRouteId(parentRouteId);
            bomQuery.setProductId(parentItemId);
            List<ProRouteProductBom> bomList = qxxProRouteProductBomMapper.selectProRouteProductBomList(bomQuery);
            for (ProRouteProductBom bom : bomList) {
                ProRouteProductBom newBom = new ProRouteProductBom();
                newBom.setRouteId(newRouteId);
                newBom.setProcessId(bom.getProcessId());
                newBom.setProductId(skuItemId);
                newBom.setItemId(bom.getItemId());
                newBom.setItemCode(bom.getItemCode());
                newBom.setItemName(bom.getItemName());
                newBom.setSpecification(bom.getSpecification());
                newBom.setUnitOfMeasure(bom.getUnitOfMeasure());
                newBom.setUnitName(bom.getUnitName());
                newBom.setQuantity(bom.getQuantity());
                newBom.setCreateTime(DateUtils.getNowDate());
                newBom.setCreateBy(SecurityUtils.getUsername());
                qxxProRouteProductBomMapper.insertProRouteProductBom(newBom);
            }

            // 5. 复制工序参数（指向新 route_product_id）
            List<ProRouteProcessParam> paramList = qxxProRouteProcessParamMapper.selectProRouteProcessParamByRouteProductId(parent.getRecordId());
            for (ProRouteProcessParam param : paramList) {
                ProRouteProcessParam newParam = new ProRouteProcessParam();
                newParam.setRouteProductId(newRecordId);
                newParam.setProcessId(param.getProcessId());
                newParam.setTemplateId(param.getTemplateId());
                newParam.setParamValue(param.getParamValue());
                newParam.setCreateTime(DateUtils.getNowDate());
                newParam.setCreateBy(SecurityUtils.getUsername());
                qxxProRouteProcessParamMapper.insertProRouteProcessParam(newParam);
            }
        }
        return count;
    }
}
