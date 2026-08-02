package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdAttrDef;
import com.ruoyi.system.domain.mes.md.MdItemTypeAttr;
import com.ruoyi.system.mapper.mes.md.MdAttrDefMapper;
import com.ruoyi.system.mapper.mes.md.MdItemTypeAttrMapper;
import com.ruoyi.system.service.mes.md.IMdItemTypeAttrService;

/**
 * 物料分类-扩展属性绑定Service业务层
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
@Service
public class MdItemTypeAttrServiceImpl implements IMdItemTypeAttrService
{
    @Autowired
    private MdItemTypeAttrMapper mdItemTypeAttrMapper;
    @Autowired
    private MdAttrDefMapper mdAttrDefMapper;

    @Override
    public List<MdItemTypeAttr> selectBindByTypeId(Long itemTypeId)
    {
        MdItemTypeAttr q = new MdItemTypeAttr();
        q.setFactoryId(SecurityUtils.getFactoryId());
        q.setItemTypeId(itemTypeId);
        return mdItemTypeAttrMapper.selectBindByTypeId(q);
    }

    @Override
    public List<MdItemTypeAttr> selectEffAttrSchema(Long factoryId, Long itemTypeId)
    {
        MdItemTypeAttr q = new MdItemTypeAttr();
        q.setFactoryId(factoryId);
        q.setItemTypeId(itemTypeId);
        return mdItemTypeAttrMapper.selectEffAttrSchema(q);
    }

    /** 全量替换某分类的绑定：先删后插，保证与前端提交一致 */
    @Override
    @Transactional
    public int saveBind(Long factoryId, Long itemTypeId, List<MdItemTypeAttr> binds)
    {
        MdItemTypeAttr del = new MdItemTypeAttr();
        del.setFactoryId(factoryId);
        del.setItemTypeId(itemTypeId);
        mdItemTypeAttrMapper.deleteBindByTypeId(del);
        if (binds == null || binds.isEmpty())
        {
            return 0;
        }
        String operName = SecurityUtils.getUsername();
        int rows = 0;
        for (MdItemTypeAttr bind : binds)
        {
            bind.setFactoryId(factoryId);
            bind.setItemTypeId(itemTypeId);
            bind.setCreateBy(operName);
            bind.setCreateTime(DateUtils.getNowDate());
            if (bind.getEnableFlag() == null) bind.setEnableFlag("1");
            if (bind.getRequired() == null) bind.setRequired("0");
            rows += mdItemTypeAttrMapper.insertItemTypeAttr(bind);
        }
        return rows;
    }

    /**
     * 新建属性并绑定到分类（隐式字典）：
     * attr_code 已存在 → 复用该字典记录（不重复创建，保证编码全局统一）；
     * 不存在 → 建 attr_def（factory_id=0 全局）。然后绑定到 item_type_attr。
     */
    @Override
    @Transactional
    public MdItemTypeAttr createAttrAndBind(Long factoryId, Long itemTypeId, MdAttrDef attrDef, boolean required)
    {
        if (attrDef == null || StringUtils.isEmpty(attrDef.getAttrCode()))
        {
            throw new com.ruoyi.common.exception.ServiceException("属性编码不能为空");
        }
        if (StringUtils.isEmpty(attrDef.getAttrName()))
        {
            throw new com.ruoyi.common.exception.ServiceException("属性名称不能为空");
        }
        // 1. 字典 upsert：按 attr_code 查，存在则复用 attrId，不存在则建
        MdAttrDef exist = mdAttrDefMapper.checkAttrCodeUnique(attrDef);
        Long attrId;
        if (exist != null)
        {
            attrId = exist.getAttrId();
        }
        else
        {
            attrDef.setFactoryId(0L); // 全局共享
            if (StringUtils.isEmpty(attrDef.getAttrType())) attrDef.setAttrType("TEXT");
            if (attrDef.getEnableFlag() == null) attrDef.setEnableFlag("1");
            if (attrDef.getSortOrder() == null) attrDef.setSortOrder(0);
            attrDef.setCreateTime(DateUtils.getNowDate());
            mdAttrDefMapper.insertMdAttrDef(attrDef);
            attrId = attrDef.getAttrId();
        }

        // 2. 防重复绑定：若该分类已绑定此 attr，直接返回已有绑定
        MdItemTypeAttr existQuery = new MdItemTypeAttr();
        existQuery.setFactoryId(factoryId);
        existQuery.setItemTypeId(itemTypeId);
        List<MdItemTypeAttr> existed = mdItemTypeAttrMapper.selectBindByTypeId(existQuery);
        for (MdItemTypeAttr b : existed)
        {
            if (attrId.equals(b.getAttrId())) return b;
        }

        // 3. 绑定到分类
        MdItemTypeAttr bind = new MdItemTypeAttr();
        bind.setFactoryId(factoryId);
        bind.setItemTypeId(itemTypeId);
        bind.setAttrId(attrId);
        bind.setRequired(required ? "1" : "0");
        bind.setSortOrder(existed.size());
        bind.setEnableFlag("1");
        bind.setCreateBy(SecurityUtils.getUsername());
        bind.setCreateTime(DateUtils.getNowDate());
        mdItemTypeAttrMapper.insertItemTypeAttr(bind);

        // 回填关联字段供前端展示
        bind.setAttrCode(attrDef.getAttrCode());
        bind.setAttrName(attrDef.getAttrName());
        bind.setAttrType(attrDef.getAttrType());
        bind.setAttrUnit(attrDef.getAttrUnit());
        bind.setOptionsJson(attrDef.getOptionsJson());
        bind.setInheritDepth(0);
        return bind;
    }
}
