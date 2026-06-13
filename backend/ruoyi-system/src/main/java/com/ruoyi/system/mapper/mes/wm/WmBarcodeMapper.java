package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmBarcode;

public interface WmBarcodeMapper
{
    public List<WmBarcode> selectWmBarcodeList(WmBarcode entity);
    public List<WmBarcode> selectWmBarcodeAll();
    public WmBarcode selectWmBarcodeByBarcodeId(Long barcodeId);
    public int insertWmBarcode(WmBarcode entity);
    public int updateWmBarcode(WmBarcode entity);
    public int deleteWmBarcodeByBarcodeId(Long barcodeId);
    public int deleteWmBarcodeByBarcodeIds(Long[] barcodeIds);
}