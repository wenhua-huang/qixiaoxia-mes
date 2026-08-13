package com.ruoyi.web.controller.mes.pur.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.common.enums.PurOrderStatus;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderDetailVO;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.web.controller.mes.export.AbstractPdfExporter;

/**
 * 采购订单详情 PDF 导出器（对外版）。
 * 版式：公司抬头 → 订单头信息 → 明细表(含税率) → 合计(不含税/税额/价税合计+大写) → 备注 → 签章栏。
 *
 * @author qixiaoxia
 */
@Component
public class PurOrderPdfExporter extends AbstractPdfExporter<PurOrderDetailVO>
{
    @Override
    protected String documentTitle() { return "采购订单"; }

    @Override
    protected String buildBody(PurOrderDetailVO d)
    {
        PurOrderVO o = d.getOrder();
        return infoGrid(o) + linesTable(d.getLines()) + amountAndReq(o, d.getLines()) + signBlock();
    }

    private String infoGrid(PurOrderVO o)
    {
        var pairs = new java.util.ArrayList<String[]>();
        pairs.add(new String[]{"订单号", esc(str(o.getOrderCode()))});
        pairs.add(new String[]{"订单日期", date(o.getOrderDate())});
        pairs.add(new String[]{"供应商", esc(str(o.getVendorName()))});
        pairs.add(new String[]{"状态", statusText(o.getStatus())});
        pairs.add(new String[]{"采购员", esc(str(o.getPurchaser()))});
        pairs.add(new String[]{"审批人", esc(str(o.getApprover()))});
        pairs.add(new String[]{"币种", esc(str(o.getCurrency()))});
        pairs.add(new String[]{"交货日期", date(o.getExpectedDate())});
        pairs.add(new String[]{"关联客户订单", esc(str(o.getSourceOrderCode()))});
        StringBuilder sb = new StringBuilder("<table class='info'>");
        for (int i = 0; i < pairs.size(); i += 2)
        {
            sb.append("<tr>").append(kv(pairs.get(i)[0], pairs.get(i)[1]));
            sb.append(i + 1 < pairs.size() ? kv(pairs.get(i + 1)[0], pairs.get(i + 1)[1]) : "<td></td><td class='v'></td>");
            sb.append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    private String linesTable(List<PurOrderLine> lines)
    {
        StringBuilder sb = new StringBuilder("<table class='grid'><tr>");
        for (String h : new String[]{"序号", "物料编码", "物料名称", "规格", "单位", "数量", "单价", "不含税金额", "税率"})
        {
            sb.append("<th>").append(h).append("</th>");
        }
        sb.append("</tr>");
        if (lines != null)
        {
            for (int i = 0; i < lines.size(); i++)
            {
                PurOrderLine l = lines.get(i);
                sb.append("<tr>")
                        .append(td(String.valueOf(i + 1)))
                        .append(td(esc(str(l.getItemCode()))))
                        .append(td(esc(str(l.getItemName()))))
                        .append(td(esc(str(l.getSpecification()))))
                        .append(td(esc(str(l.getUnitName()))))
                        .append(td(num(l.getQuantityOrdered())))
                        .append(td(money(l.getUnitPrice())))
                        .append(td(money(l.getAmount())))
                        .append(td(taxRate(l.getTaxRate())))
                        .append("</tr>");
            }
        }
        return sb.append("</table>").toString();
    }

    private String amountAndReq(PurOrderVO o, List<PurOrderLine> lines)
    {
        BigDecimal net = sumAmount(lines);
        BigDecimal tax = sumTax(lines);
        BigDecimal total = net.add(tax);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='amt'>不含税合计：￥").append(money(net))
                .append("<span class='big'>税额：￥").append(money(tax)).append("</span>")
                .append("<span class='big'>价税合计：￥").append(money(total)).append("</span></div>");
        sb.append("<div class='amt'>价税合计（大写）：").append(esc(cnyUpper(total))).append("</div>");
        if (o.getRemark() != null && !o.getRemark().isBlank())
        {
            sb.append("<div class='req'><span class='p'>备注：</span>").append(esc(o.getRemark())).append("</div>");
        }
        return sb.toString();
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

    private String signBlock()
    {
        return "<table class='sign'><tr>"
                + "<td>采购员（签字）：______________</td>"
                + "<td>审批人（签字）：______________</td>"
                + "<td>供应商确认（盖章）：______________</td>"
                + "<td>日期：______________</td>"
                + "</tr></table>";
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
