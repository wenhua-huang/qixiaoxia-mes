package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmPackage;

public interface IWmPackageService
{
    public List<WmPackage> selectWmPackageList(WmPackage entity);
    public List<WmPackage> selectWmPackageAll();
    public WmPackage selectWmPackageByPackageId(Long packageId);
    public int insertWmPackage(WmPackage entity);
    public int updateWmPackage(WmPackage entity);
    public int deleteWmPackageByPackageId(Long packageId);
    public int deleteWmPackageByPackageIds(Long[] packageIds);
}