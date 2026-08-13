package com.ruoyi.web.controller.mes.sal.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.common.enums.SalOrderStatus;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.web.controller.mes.export.AbstractPdfExporter;

/**
 * 销售订单详情 PDF 导出器（对外版）。
 * 版式：公司抬头 → 订单头信息 → 明细表 → 合计(小写+大写) → 工艺要求 → 签章栏。
 * 隐藏内部字段：已转工单量/可转量/订单来源。
 *
 * @author qixiaoxia
 */
@Component
public class SalOrderPdfExporter extends AbstractPdfExporter<SalOrder>
{
    @Override
    protected String documentTitle() { return "销售订单"; }

    @Override
    protected String buildBody(SalOrder o)
    {
        return infoGrid(o) + linesTable(o) + amountAndReq(o) + signBlock();
    }

    private String infoGrid(SalOrder o)
    {
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[]{"订单号", esc(str(o.getOrderCode()))});
        pairs.add(new String[]{"订单日期", date(o.getOrderDate())});
        pairs.add(new String[]{"订单名称", esc(str(o.getOrderName()))});
        pairs.add(new String[]{"状态", statusText(o.getStatus())});
        pairs.add(new String[]{"客户", esc(str(o.getClientName()))});
        pairs.add(new String[]{"客户PO号", esc(str(o.getClientOrderCode()))});
        pairs.add(new String[]{"业务员", esc(str(o.getSalesperson()))});
        pairs.add(new String[]{"业务线", businessLineText(o.getBusinessLine())});
        pairs.add(new String[]{"订单类型", orderTypeText(o.getOrderType())});
        pairs.add(new String[]{"付款方式", esc(str(o.getPaymentMethod()))});
        pairs.add(new String[]{"需求交期", date(o.getRequestDate())});
        StringBuilder sb = new StringBuilder("<table class='info'>");
        for (int i = 0; i < pairs.size(); i += 2)
        {
            sb.append("<tr>").append(kv(pairs.get(i)[0], pairs.get(i)[1]));
            sb.append(i + 1 < pairs.size() ? kv(pairs.get(i + 1)[0], pairs.get(i + 1)[1]) : "<td></td><td class='v'></td>");
            sb.append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    private String linesTable(SalOrder o)
    {
        StringBuilder sb = new StringBuilder("<table class='grid'><tr>");
        for (String h : new String[]{"序号", "产品编码", "产品名称", "规格/尺寸", "数量", "单位", "单价", "行金额"})
        {
            sb.append("<th>").append(h).append("</th>");
        }
        sb.append("</tr>");
        List<SalOrderLine> lines = o.getLines();
        if (lines != null)
        {
            for (SalOrderLine l : lines)
            {
                sb.append("<tr>")
                        .append(td(str(l.getLineNo())))
                        .append(td(esc(str(l.getProductCode()))))
                        .append(td(esc(str(l.getProductName()))))
                        .append(td(esc(str(l.getProductSize()))))
                        .append(td(num(l.getQuantity())))
                        .append(td(esc(str(l.getUnitName()))))
                        .append(td(money(l.getUnitPrice())))
                        .append(td(money(l.getLineAmount())))
                        .append("</tr>");
            }
        }
        return sb.append("</table>").toString();
    }

    private String amountAndReq(SalOrder o)
    {
        BigDecimal total = sumLines(o.getLines());
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='amt'>合计（小写）：￥").append(money(total))
                .append("<span class='big'>合计（大写）：").append(esc(cnyUpper(total))).append("</span></div>");
        sb.append(reqBlock(o));
        return sb.toString();
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

    private String reqBlock(SalOrder o)
    {
        StringBuilder sb = new StringBuilder();
        List<SalOrderLine> lines = o.getLines();
        if (lines != null)
        {
            for (SalOrderLine l : lines)
            {
                String req = joinReq(l);
                if (req.isEmpty())
                {
                    continue;
                }
                sb.append("<div class='req'><span class='p'>【")
                        .append(esc(str(l.getProductName()))).append("】</span>").append(req).append("</div>");
            }
        }
        if (o.getRemark() != null && !o.getRemark().isBlank())
        {
            sb.append("<div class='req'><span class='p'>订单备注：</span>").append(esc(o.getRemark())).append("</div>");
        }
        return sb.toString();
    }

    private String joinReq(SalOrderLine l)
    {
        StringBuilder sb = new StringBuilder();
        appendReq(sb, "尺寸", l.getProductSize());
        appendReq(sb, "印刷要求", l.getPrintingReq());
        appendReq(sb, "绳料规格", l.getRopeSpec());
        appendReq(sb, "包装要求", l.getPackageReq());
        appendReq(sb, "出货要求", l.getShippingReq());
        return sb.toString();
    }

    private void appendReq(StringBuilder sb, String label, String val)
    {
        if (val == null || val.isBlank())
        {
            return;
        }
        if (sb.length() > 0)
        {
            sb.append("　");
        }
        sb.append(label).append("：").append(esc(val));
    }

    private String signBlock()
    {
        return "<table class='sign'><tr>"
                + "<td>业务员（签字）：______________</td>"
                + "<td>客户确认（盖章）：______________</td>"
                + "<td>日期：______________</td>"
                + "</tr></table>";
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
}
