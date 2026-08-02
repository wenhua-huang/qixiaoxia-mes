package com.ruoyi.system.service.mes.md.impl;

import java.util.Collections;
import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.md.MdItemTypeMapper;
import com.ruoyi.system.service.mes.md.IMdItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 物料产品Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * <p>扩展属性（纸张/纸袋/礼品盒等）已从原三张行业子表迁移为分类驱动的动态属性，
 * 存于 {@code qxx_md_item.ext_attrs}（JSON 扁平 {attrCode:value}），随主表 insert/update 一并持久化，
 * 不再需要子表联动的增删改。SPU 变体继承父物料的 ext_attrs。
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
    private MdItemTypeMapper itemTypeMapper;

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
        // 点击父分类时，展开为「自身+所有子分类」ID 列表，使子分类下的物料也能被查出
        if (mdItem.getItemTypeId() != null && mdItem.getItemTypeId() != 0)
        {
            List<Long> descendantIds = itemTypeMapper.selectDescendantIds(mdItem.getItemTypeId());
            // CTE 返回空说明该分类ID不存在，塞入-1使 IN(-1) 不命中任何记录（主键永不为负）
            mdItem.setItemTypeIds(descendantIds.isEmpty()
                ? Collections.singletonList(-1L) : descendantIds);
            mdItem.setItemTypeId(null);
        }
        return mdItemMapper.selectMdItemList(mdItem);
    }

    @Override
    public MdItem selectMdItemById(Long itemId)
    {
        // ext_attrs 作为主表 JSON 列，由 MdItemMapper 的 resultMap + JsonMapTypeHandler 自动带出，无需手动 JOIN
        return mdItemMapper.selectMdItemById(itemId);
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

        // SPU/变体继承逻辑：如果是变体（parentId>0），从父产品继承 itemType 信息 + 扩展属性
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
                // 继承扩展属性（ext_attrs 已随 selectMdItemById 自动带出）
                if (mdItem.getExtAttrs() == null)
                    mdItem.setExtAttrs(parent.getExtAttrs());
            }
        }

        return mdItemMapper.insertMdItem(mdItem);
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

    @Override
    @Transactional
    public int updateMdItem(MdItem mdItem)
    {
        resolveItemTypeId(mdItem);
        mdItem.setUpdateTime(DateUtils.getNowDate());
        return mdItemMapper.updateMdItem(mdItem);
    }

    @Override
    @Transactional
    public int deleteMdItemById(Long itemId)
    {
        // ext_attrs 在主表，主表删即全删，无需级联清理子表
        return mdItemMapper.deleteMdItemById(itemId);
    }

    @Override
    @Transactional
    public int deleteMdItemByIds(Long[] itemIds)
    {
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
