package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdProductBom;

/**
 * 产品BOMMapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdProductBomMapper
{
    public MdProductBom selectMdProductBomByBomId(Long bomId);
    public List<MdProductBom> selectMdProductBomList(MdProductBom mdProductBom);
    public List<MdProductBom> selectAll();
    public int insertMdProductBom(MdProductBom mdProductBom);
    public int updateMdProductBom(MdProductBom mdProductBom);
    public int deleteMdProductBomByBomId(Long bomId);
    public int deleteMdProductBomByBomIds(Long[] bomIds);
}
