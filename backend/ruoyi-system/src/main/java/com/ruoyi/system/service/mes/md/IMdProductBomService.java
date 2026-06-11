package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdProductBom;

/**
 * 产品BOMService接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdProductBomService
{
    public MdProductBom selectMdProductBomByBomId(Long bomId);
    public List<MdProductBom> selectMdProductBomList(MdProductBom mdProductBom);
    public List<MdProductBom> selectAll();
    public int insertMdProductBom(MdProductBom mdProductBom);
    public int updateMdProductBom(MdProductBom mdProductBom);
    public int deleteMdProductBomByBomIds(Long[] bomIds);
    public int deleteMdProductBomByBomId(Long bomId);
    /** 检测新增 BOM 关系是否产生循环引用 */
    public boolean checkBomCycle(MdProductBom mdProductBom);
}
