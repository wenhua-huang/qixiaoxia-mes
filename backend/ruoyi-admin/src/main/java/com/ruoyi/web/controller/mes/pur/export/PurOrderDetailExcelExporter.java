package com.ruoyi.web.controller.mes.pur.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;
import com.ruoyi.common.enums.PurOrderStatus;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderDetailVO;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.web.controller.mes.export.AbstractExcelExporter;

/**
 * 采购订单详情 Excel 导出器（对内版）。
 * 版式：标题→头部信息块→明细表→合计(不含税/税额/价税合计)→备注，保留已收量。
 *
 * @author qixiaoxia
 */
@Component
public class PurOrderDetailExcelExporter extends AbstractExcelExporter<PurOrderDetailVO>
{
    @Override
    protected String sheetName() { return "采购订单"; }

    @Override
    protected String titleSuffix() { return "采购订单详情"; }

    @Override
    protected int[] columnWidths()
    {
        return new int[]{1500, 3200, 6000, 4200, 1600, 2200, 2600, 3000, 1800, 2600};
    }

    @Override
    protected int writeSections(Sheet sh, int row, Styles st, PurOrderDetailVO d)
    {
        PurOrderVO o = d.getOrder();
        row = writeInfoBlock(sh, row, st, o);
        row = writeDetailHead(sh, row, st);
        row = writeDetailLines(sh, row, st, d.getLines());
        row = writeTotal(sh, row, st, d.getLines());
        writeRemark(sh, row, st, o);
        return row;
    }

    private int writeInfoBlock(Sheet sh, int r, Styles st, PurOrderVO o)
    {
        infoRow(sh, r++, st, "订单号", str(o.getOrderCode()), "订单日期", date(o.getOrderDate()), "状态", statusText(o.getStatus()));
        infoRow(sh, r++, st, "供应商", str(o.getVendorName()), "采购员", str(o.getPurchaser()), "审批人", str(o.getApprover()));
        infoRow(sh, r++, st, "币种", str(o.getCurrency()), "交货日期", date(o.getExpectedDate()), "关联客户订单", str(o.getSourceOrderCode()));
        infoRow(sh, r++, st, "订单名称", str(o.getOrderName()), "", "", "", "");
        return r + 1;
    }

    private int writeDetailHead(Sheet sh, int r, Styles st)
    {
        Row row = sh.createRow(r);
        String[] hs = {"序号", "物料编码", "物料名称", "规格", "单位", "数量", "单价", "不含税金额", "税率", "已收量"};
        for (int i = 0; i < hs.length; i++)
        {
            txt(row, i, st.head, hs[i]);
        }
        return r + 1;
    }

    private int writeDetailLines(Sheet sh, int r, Styles st, List<PurOrderLine> lines)
    {
        if (lines == null)
        {
            return r;
        }
        for (int i = 0; i < lines.size(); i++)
        {
            PurOrderLine l = lines.get(i);
            Row row = sh.createRow(r);
            txt(row, 0, st.cell, String.valueOf(i + 1));
            txt(row, 1, st.cell, str(l.getItemCode()));
            txt(row, 2, st.cell, str(l.getItemName()));
            txt(row, 3, st.cell, str(l.getSpecification()));
            txt(row, 4, st.cell, str(l.getUnitName()));
            txt(row, 5, st.cell, num(l.getQuantityOrdered()));
            money(row, 6, st.money, l.getUnitPrice());
            money(row, 7, st.money, l.getAmount());
            txt(row, 8, st.cell, taxRate(l.getTaxRate()));
            txt(row, 9, st.cell, num(l.getQuantityReceived()));
            r++;
        }
        return r;
    }

    private int writeTotal(Sheet sh, int r, Styles st, List<PurOrderLine> lines)
    {
        BigDecimal net = sumAmount(lines);
        BigDecimal tax = sumTax(lines);
        BigDecimal total = net.add(tax);
        Row row = sh.createRow(r);
        txt(row, 0, st.totalLabel, "合计");
        sh.addMergedRegion(new CellRangeAddress(r, r, 0, 6));
        money(row, 7, st.moneyBold, total);
        txt(row, 8, st.cell, "（不含税 " + moneyFmt(net) + " + 税额 " + moneyFmt(tax) + "）　大写：" + cnyUpper(total));
        sh.addMergedRegion(new CellRangeAddress(r, r, 8, 9));
        return r + 2;
    }

    /** 不含税合计 = 明细行 amount 之和 */
    private BigDecimal sumAmount(List<PurOrderLine> lines)
    {
        BigDecimal sum = BigDecimal.ZERO;
        if (lines == null) return sum;
        for (PurOrderLine l : lines)
        {
            if (l.getAmount() != null) sum = sum.add(l.getAmount());
        }
        return sum;
    }

    /** 税额 = Σ amount × taxRate / 100 */
    private BigDecimal sumTax(List<PurOrderLine> lines)
    {
        BigDecimal sum = BigDecimal.ZERO;
        if (lines == null) return sum;
        for (PurOrderLine l : lines)
        {
            if (l.getAmount() != null && l.getTaxRate() != null)
            {
                sum = sum.add(l.getAmount().multiply(l.getTaxRate())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }
        }
        return sum;
    }

    private void writeRemark(Sheet sh, int r, Styles st, PurOrderVO o)
    {
        String remark = str(o.getRemark());
        if (remark.isEmpty())
        {
            return;
        }
        Row row = sh.createRow(r);
        txt(row, 0, st.value, "备注：" + remark);
        sh.addMergedRegion(new CellRangeAddress(r, r, 0, 9));
    }

    private String taxRate(BigDecimal b)
    {
        return b == null ? "" : b.stripTrailingZeros().toPlainString() + "%";
    }

    private String statusText(String s)
    {
        PurOrderStatus st = PurOrderStatus.fromCode(s);
        return st != null ? st.getInfo() : str(s);
    }
}
