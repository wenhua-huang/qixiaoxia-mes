package com.ruoyi.web.controller.mes.sal.export;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;
import com.ruoyi.common.enums.SalOrderStatus;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.web.controller.mes.export.AbstractExcelExporter;

/**
 * 销售订单详情 Excel 导出器（对内版）。
 * 版式：标题→头部信息块(含审核人)→明细表→合计→备注，保留内部字段(已转量/可转量/来源)。
 *
 * @author qixiaoxia
 */
@Component
public class SalOrderDetailExcelExporter extends AbstractExcelExporter<SalOrder>
{
    @Override
    protected String sheetName() { return "销售订单"; }

    @Override
    protected String titleSuffix() { return "销售订单详情"; }

    @Override
    protected int[] columnWidths()
    {
        return new int[]{1800, 3200, 6000, 4200, 2200, 1600, 2600, 3000, 2600, 2600};
    }

    @Override
    protected int writeSections(Sheet sh, int row, Styles st, SalOrder o)
    {
        row = writeInfoBlock(sh, row, st, o);
        row = writeDetailHead(sh, row, st);
        row = writeDetailLines(sh, row, st, o);
        row = writeTotal(sh, row, st, o);
        writeRemark(sh, row, st, o);
        return row;
    }

    private int writeInfoBlock(Sheet sh, int r, Styles st, SalOrder o)
    {
        infoRow(sh, r++, st, "订单号", str(o.getOrderCode()), "订单日期", date(o.getOrderDate()), "状态", statusText(o.getStatus()));
        infoRow(sh, r++, st, "客户", str(o.getClientName()), "客户PO号", str(o.getClientOrderCode()), "业务员", str(o.getSalesperson()));
        infoRow(sh, r++, st, "业务线", businessLineText(o.getBusinessLine()), "付款方式", str(o.getPaymentMethod()), "需求交期", date(o.getRequestDate()));
        infoRow(sh, r++, st, "订单类型", orderTypeText(o.getOrderType()), "样品", yesNo(o.getSampleFlag()), "来源", sourceText(o.getSource()));
        infoRow(sh, r++, st, "审核人", str(o.getApproveBy()), "审核时间", dateTime(o.getApproveTime()), "订单名称", str(o.getOrderName()));
        return r + 1;
    }

    private int writeDetailHead(Sheet sh, int r, Styles st)
    {
        Row row = sh.createRow(r);
        String[] hs = {"序号", "产品编码", "产品名称", "规格/尺寸", "数量", "单位", "单价", "行金额", "已转量", "可转量"};
        for (int i = 0; i < hs.length; i++)
        {
            txt(row, i, st.head, hs[i]);
        }
        return r + 1;
    }

    private int writeDetailLines(Sheet sh, int r, Styles st, SalOrder o)
    {
        List<SalOrderLine> lines = o.getLines();
        if (lines == null)
        {
            return r;
        }
        for (SalOrderLine l : lines)
        {
            Row row = sh.createRow(r);
            txt(row, 0, st.cell, str(l.getLineNo()));
            txt(row, 1, st.cell, str(l.getProductCode()));
            txt(row, 2, st.cell, str(l.getProductName()));
            txt(row, 3, st.cell, str(l.getProductSize()));
            txt(row, 4, st.cell, num(l.getQuantity()));
            txt(row, 5, st.cell, str(l.getUnitName()));
            money(row, 6, st.money, l.getUnitPrice());
            money(row, 7, st.money, l.getLineAmount());
            txt(row, 8, st.cell, num(l.getQuantityProduced()));
            txt(row, 9, st.cell, num(l.getQuantityConvertible()));
            r++;
        }
        return r;
    }

    private int writeTotal(Sheet sh, int r, Styles st, SalOrder o)
    {
        Row row = sh.createRow(r);
        txt(row, 0, st.totalLabel, "合计");
        sh.addMergedRegion(new CellRangeAddress(r, r, 0, 6));
        BigDecimal total = sumLines(o.getLines());
        money(row, 7, st.moneyBold, total);
        txt(row, 8, st.cell, "大写：" + cnyUpper(total));
        sh.addMergedRegion(new CellRangeAddress(r, r, 8, 9));
        return r + 2;
    }

    /** 合计金额取明细行金额之和（比订单头 totalAmount 更可靠，后者可能未回填） */
    private BigDecimal sumLines(List<SalOrderLine> lines)
    {
        BigDecimal sum = BigDecimal.ZERO;
        if (lines == null)
        {
            return sum;
        }
        for (SalOrderLine l : lines)
        {
            if (l.getLineAmount() != null)
            {
                sum = sum.add(l.getLineAmount());
            }
        }
        return sum;
    }

    private void writeRemark(Sheet sh, int r, Styles st, SalOrder o)
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

    private String statusText(String s)
    {
        SalOrderStatus st = SalOrderStatus.fromCode(s);
        return st != null ? st.getInfo() : str(s);
    }

    private String businessLineText(String s)
    {
        return switch (s == null ? "" : s)
        {
            case "DOMESTIC" -> "内贸";
            case "FOREIGN" -> "外贸";
            case "SPOT" -> "现货";
            default -> str(s);
        };
    }

    private String orderTypeText(String s)
    {
        return switch (s == null ? "" : s)
        {
            case "NEW" -> "新单";
            case "REPEAT" -> "返单";
            default -> str(s);
        };
    }

    private String yesNo(String s) { return "Y".equals(s) ? "是" : "N".equals(s) ? "否" : str(s); }

    private String sourceText(Integer s)
    {
        if (s == null) return "";
        return switch (s) { case 1 -> "直接新增"; case 2 -> "CRM系统"; default -> str(s); };
    }
}
