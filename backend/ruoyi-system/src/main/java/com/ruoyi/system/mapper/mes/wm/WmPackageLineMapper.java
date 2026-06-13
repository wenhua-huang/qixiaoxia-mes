package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmPackageLine;

public interface WmPackageLineMapper
{
    public List<WmPackageLine> selectWmPackageLineList(WmPackageLine entity);
    public List<WmPackageLine> selectWmPackageLineAll();
    public WmPackageLine selectWmPackageLineByLineId(Long lineId);
    public int insertWmPackageLine(WmPackageLine entity);
    public int updateWmPackageLine(WmPackageLine entity);
    public int deleteWmPackageLineByLineId(Long lineId);
    public int deleteWmPackageLineByLineIds(Long[] lineIds);
}