package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdItem;

/**
 * 物料产品Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdItemService
{
    public List<MdItem> selectMdItemAllEnabled();

    public List<MdItem> selectMdItemList(MdItem mdItem);

    /** 查询物料详情（含行业子表数据） */
    public MdItem selectMdItemById(Long itemId);

    public boolean checkItemCodeUnique(MdItem mdItem);

    /** 新增物料（含行业子表联动 + 变体继承逻辑） */
    public int insertMdItem(MdItem mdItem);

    /** 更新物料（含行业子表联动） */
    public int updateMdItem(MdItem mdItem);

    public int deleteMdItemById(Long itemId);

    public int deleteMdItemByIds(Long[] itemIds);

    /** 导入物料数据 */
    public String importItem(List<MdItem> itemList, boolean updateSupport, String operName);
}
