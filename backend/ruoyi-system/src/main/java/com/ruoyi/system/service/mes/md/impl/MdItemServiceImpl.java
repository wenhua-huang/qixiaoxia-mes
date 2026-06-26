package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.domain.mes.md.MdItemAttrGiftBox;
import com.ruoyi.system.domain.mes.md.MdItemAttrPaper;
import com.ruoyi.system.domain.mes.md.MdItemAttrPaperBag;
import com.ruoyi.system.mapper.mes.md.MdItemAttrGiftBoxMapper;
import com.ruoyi.system.mapper.mes.md.MdItemAttrPaperBagMapper;
import com.ruoyi.system.mapper.mes.md.MdItemAttrPaperMapper;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.service.mes.md.IMdItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 物料产品Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdItemServiceImpl implements IMdItemService
{
    private static final Logger log = LoggerFactory.getLogger(MdItemServiceImpl.class);

    @Autowired
    private MdItemMapper mdItemMapper;
    @Autowired
    private MdItemAttrPaperMapper attrPaperMapper;
    @Autowired
    private MdItemAttrPaperBagMapper attrPaperBagMapper;
    @Autowired
    private MdItemAttrGiftBoxMapper attrGiftBoxMapper;
    @Autowired
    private com.ruoyi.system.mapper.mes.md.MdItemTypeMapper itemTypeMapper;

    @Override
    public List<MdItem> selectMdItemAllEnabled()
    {
        MdItem cond = new MdItem();
        cond.setEnableFlag("1");
        return mdItemMapper.selectMdItemList(cond);
    }

    @Override
    public List<MdItem> selectMdItemList(MdItem mdItem)
    {
        return mdItemMapper.selectMdItemList(mdItem);
    }

    @Override
    public MdItem selectMdItemById(Long itemId)
    {
        MdItem item = mdItemMapper.selectMdItemById(itemId);
        if (item != null)
        {
            // JOIN 行业子表数据
            item.setAttrPaper(attrPaperMapper.selectByItemId(itemId));
            item.setAttrPaperBag(attrPaperBagMapper.selectByItemId(itemId));
            item.setAttrGiftBox(attrGiftBoxMapper.selectByItemId(itemId));
        }
        return item;
    }

    @Override
    public boolean checkItemCodeUnique(MdItem mdItem)
    {
        MdItem item = mdItemMapper.checkItemCodeUnique(mdItem);
        Long itemId = mdItem.getItemId() == null ? -1L : mdItem.getItemId();
        if (StringUtils.isNotNull(item) && item.getItemId().longValue() != itemId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    @Transactional
    public int insertMdItem(MdItem mdItem)
    {
        mdItem.setCreateTime(DateUtils.getNowDate());

        // 兜底：根据 itemTypeCode 查询 itemTypeId（前端可能只传了 code，没传 id）
        resolveItemTypeId(mdItem);

        // SPU/变体继承逻辑：如果是变体（parentId>0），从父产品继承 itemType 信息
        // 注意：SPU 自身（parentId=0）即标准，不在此自动创建冗余"标准变体"
        if (mdItem.getParentId() != null && mdItem.getParentId() > 0)
        {
            MdItem parent = mdItemMapper.selectMdItemById(mdItem.getParentId());
            if (parent != null)
            {
                // 继承父产品的类型信息
                if (StringUtils.isEmpty(mdItem.getItemTypeCode()))
                    mdItem.setItemTypeCode(parent.getItemTypeCode());
                if (mdItem.getItemTypeId() == null || mdItem.getItemTypeId() == 0)
                    mdItem.setItemTypeId(parent.getItemTypeId());
                if (StringUtils.isEmpty(mdItem.getItemTypeName()))
                    mdItem.setItemTypeName(parent.getItemTypeName());
                // 继承单位
                if (StringUtils.isEmpty(mdItem.getUnitOfMeasure()))
                    mdItem.setUnitOfMeasure(parent.getUnitOfMeasure());
                if (StringUtils.isEmpty(mdItem.getUnitName()))
                    mdItem.setUnitName(parent.getUnitName());
            }
        }

        int rows = mdItemMapper.insertMdItem(mdItem);

        // 方案A：变体创建时，复制父产品的行业子表行到新 itemId
        if (mdItem.getParentId() != null && mdItem.getParentId() > 0 && mdItem.getItemId() != null)
        {
            copyParentAttrsToNewItem(mdItem.getParentId(), mdItem.getItemId());
        }

        // 行业子表联动：根据 item_type 判断，INSERT 对应子表
        handleSubTableInsert(mdItem);

        return rows;
    }

    /** 根据 itemTypeCode 查表补全 itemTypeId（前端可能只传了 code 没传 id） */
    private void resolveItemTypeId(MdItem item)
    {
        if ((item.getItemTypeId() == null || item.getItemTypeId() == 0)
            && StringUtils.isNotEmpty(item.getItemTypeCode()))
        {
            com.ruoyi.system.domain.mes.md.MdItemType query = new com.ruoyi.system.domain.mes.md.MdItemType();
            query.setItemTypeCode(item.getItemTypeCode());
            com.ruoyi.system.domain.mes.md.MdItemType type = itemTypeMapper.checkItemTypeCodeUnique(query);
            if (type != null)
            {
                item.setItemTypeId(type.getItemTypeId());
                if (StringUtils.isEmpty(item.getItemTypeName()))
                    item.setItemTypeName(type.getItemTypeName());
            }
        }
    }

    /** 复制父产品子表行到新变体 itemId */
    private void copyParentAttrsToNewItem(Long parentId, Long newItemId)
    {
        MdItemAttrPaper pPaper = attrPaperMapper.selectByItemId(parentId);
        if (pPaper != null)
        {
            pPaper.setAttrId(null);
            pPaper.setItemId(newItemId);
            attrPaperMapper.insert(pPaper);
        }
        MdItemAttrPaperBag pBag = attrPaperBagMapper.selectByItemId(parentId);
        if (pBag != null)
        {
            pBag.setAttrId(null);
            pBag.setItemId(newItemId);
            attrPaperBagMapper.insert(pBag);
        }
        MdItemAttrGiftBox pBox = attrGiftBoxMapper.selectByItemId(parentId);
        if (pBox != null)
        {
            pBox.setAttrId(null);
            pBox.setItemId(newItemId);
            attrGiftBoxMapper.insert(pBox);
        }
    }

    /** 根据 item 信息判断并插入对应的行业子表（按 itemOrProduct 精确匹配） */
    private void handleSubTableInsert(MdItem item)
    {
        if (item.getItemTypeCode() == null) return;
        String code = item.getItemTypeCode().toUpperCase();
        // 用 itemOrProduct 精确路由，不再依赖 itemTypeCode 的子串匹配
        String iop = item.getItemTypeCode(); // 保留原始大小写给后续检查用
        if (code.contains("PAPER_BAG") || code.equals("BAG") || code.contains("纸袋"))
        {
            MdItemAttrPaperBag bag = item.getAttrPaperBag();
            if (bag == null) bag = new MdItemAttrPaperBag();
            bag.setItemId(item.getItemId());
            bag.setCreateTime(DateUtils.getNowDate());
            attrPaperBagMapper.insert(bag);
        }
        else if (code.equals("PAPER") || code.equals("RAW-PAPER") || code.contains("纸张"))
        {
            MdItemAttrPaper paper = item.getAttrPaper();
            if (paper == null) paper = new MdItemAttrPaper();
            paper.setItemId(item.getItemId());
            paper.setCreateTime(DateUtils.getNowDate());
            attrPaperMapper.insert(paper);
        }
        else if (code.contains("GIFT") || code.equals("BOX") || code.contains("盒"))
        {
            MdItemAttrGiftBox box = item.getAttrGiftBox();
            if (box == null) box = new MdItemAttrGiftBox();
            box.setItemId(item.getItemId());
            box.setCreateTime(DateUtils.getNowDate());
            attrGiftBoxMapper.insert(box);
        }
    }

    @Override
    @Transactional
    public int updateMdItem(MdItem mdItem)
    {
        resolveItemTypeId(mdItem);
        mdItem.setUpdateTime(DateUtils.getNowDate());
        int rows = mdItemMapper.updateMdItem(mdItem);

        // 行业子表联动：upsert 对应子表
        handleSubTableUpsert(mdItem);

        return rows;
    }

    private void handleSubTableUpsert(MdItem item)
    {
        if (item.getItemTypeCode() == null)
        {
            // 不确定类型则不操作子表
            return;
        }
        String code = item.getItemTypeCode().toUpperCase();
        Long itemId = item.getItemId();

        if (code.contains("PAPER_BAG") || code.equals("BAG") || code.contains("纸袋"))
        {
            MdItemAttrPaperBag bag = item.getAttrPaperBag();
            if (bag != null)
            {
                bag.setItemId(itemId);
                bag.setUpdateTime(DateUtils.getNowDate());
                MdItemAttrPaperBag exist = attrPaperBagMapper.selectByItemId(itemId);
                if (exist != null) attrPaperBagMapper.updateByItemId(bag);
                else { bag.setCreateTime(DateUtils.getNowDate()); attrPaperBagMapper.insert(bag); }
            }
        }
        else if (code.equals("PAPER") || code.equals("RAW-PAPER") || code.contains("纸张"))
        {
            MdItemAttrPaper paper = item.getAttrPaper();
            if (paper != null)
            {
                paper.setItemId(itemId);
                paper.setUpdateTime(DateUtils.getNowDate());
                MdItemAttrPaper exist = attrPaperMapper.selectByItemId(itemId);
                if (exist != null) attrPaperMapper.updateByItemId(paper);
                else { paper.setCreateTime(DateUtils.getNowDate()); attrPaperMapper.insert(paper); }
            }
        }
        else if (code.contains("GIFT") || code.equals("BOX") || code.contains("盒"))
        {
            MdItemAttrGiftBox box = item.getAttrGiftBox();
            if (box != null)
            {
                box.setItemId(itemId);
                box.setUpdateTime(DateUtils.getNowDate());
                MdItemAttrGiftBox exist = attrGiftBoxMapper.selectByItemId(itemId);
                if (exist != null) attrGiftBoxMapper.updateByItemId(box);
                else { box.setCreateTime(DateUtils.getNowDate()); attrGiftBoxMapper.insert(box); }
            }
        }
    }

    @Override
    @Transactional
    public int deleteMdItemById(Long itemId)
    {
        // 级联删除子表行
        attrPaperMapper.deleteByItemId(itemId);
        attrPaperBagMapper.deleteByItemId(itemId);
        attrGiftBoxMapper.deleteByItemId(itemId);
        return mdItemMapper.deleteMdItemById(itemId);
    }

    @Override
    @Transactional
    public int deleteMdItemByIds(Long[] itemIds)
    {
        for (Long id : itemIds)
        {
            attrPaperMapper.deleteByItemId(id);
            attrPaperBagMapper.deleteByItemId(id);
            attrGiftBoxMapper.deleteByItemId(id);
        }
        return mdItemMapper.deleteMdItemByIds(itemIds);
    }

    @Override
    public String importItem(List<MdItem> itemList, boolean updateSupport, String operName)
    {
        if (itemList == null || itemList.isEmpty()) return "导入数据为空！";

        int success = 0, fail = 0;
        StringBuilder failMsg = new StringBuilder();

        for (MdItem item : itemList)
        {
            try
            {
                if (!checkItemCodeUnique(item))
                {
                    if (updateSupport)
                    {
                        MdItem exist = mdItemMapper.checkItemCodeUnique(item);
                        if (exist == null)
                        {
                            fail++;
                            failMsg.append("<br/>").append(item.getItemCode()).append("：编码已存在但查询失败");
                            continue;
                        }
                        item.setItemId(exist.getItemId());
                        updateMdItem(item);
                        success++;
                    }
                    else
                    {
                        fail++;
                        failMsg.append("<br/>").append(item.getItemCode()).append("：编码已存在");
                    }
                }
                else
                {
                    item.setCreateBy(operName);
                    insertMdItem(item);
                    success++;
                }
            }
            catch (Exception e)
            {
                fail++;
                failMsg.append("<br/>").append(item.getItemCode()).append("：").append(e.getMessage());
                log.error("导入物料失败: {}", item.getItemCode(), e);
            }
        }
        return "成功导入" + success + "条，失败" + fail + "条" + (fail > 0 ? failMsg.toString() : "");
    }
}
