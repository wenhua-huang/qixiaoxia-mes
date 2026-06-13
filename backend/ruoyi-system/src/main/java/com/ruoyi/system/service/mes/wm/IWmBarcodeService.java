package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmBarcode;

public interface IWmBarcodeService
{
    public List<WmBarcode> selectWmBarcodeList(WmBarcode entity);
    public List<WmBarcode> selectWmBarcodeAll();
    public WmBarcode selectWmBarcodeByBarcodeId(Long barcodeId);
    public int insertWmBarcode(WmBarcode entity);
    public int updateWmBarcode(WmBarcode entity);
    public int deleteWmBarcodeByBarcodeId(Long barcodeId);
    public int deleteWmBarcodeByBarcodeIds(Long[] barcodeIds);
}