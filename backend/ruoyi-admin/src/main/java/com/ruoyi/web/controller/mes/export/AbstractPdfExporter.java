package com.ruoyi.web.controller.mes.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import com.ruoyi.common.config.DocProperties;
import com.ruoyi.common.core.text.Convert;

/**
 * 单据 PDF 导出基类（对外版）。
 * 封装 openhtmltopdf 渲染脚手架：中文字体、公司抬头(Logo/名称/联系方式)、
 * HTML 转义、金额/日期格式化、人民币大写。子类只需提供 documentTitle() 和 buildBody()。
 *
 * @param <T> 单据数据类型（SalOrder / PurOrderDetailVO 等）
 * @author qixiaoxia
 */
public abstract class AbstractPdfExporter<T>
{
    @Autowired
    protected DocProperties docProperties;

    static { XRLog.setLoggingEnabled(false); }

    /** 单据标题后缀，如"销售订单"、"采购订单" */
    protected abstract String documentTitle();

    /** 子类构造 body HTML（信息区 + 明细表 + 金额 + 签章等），不含 body 标签 */
    protected abstract String buildBody(T data);

    /** 渲染 PDF 到输出流 */
    public void write(T data, OutputStream out) throws Exception
    {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useFont(this::openFontStream, "NotoSansSC");
        builder.withHtmlContent(buildHtml(data), null);
        builder.toStream(out);
        builder.run();
    }

    private String buildHtml(T data)
    {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'/>" + style()
                + "</head><body>" + headerBlock() + buildBody(data) + "</body></html>";
    }

    /** 加载 classpath 中文字体（FSSupplier.supply 不声明受检异常，故在此捕获） */
    private InputStream openFontStream()
    {
        try
        {
            return new ClassPathResource("fonts/NotoSansSC-Regular.ttf").getInputStream();
        }
        catch (IOException e)
        {
            throw new RuntimeException("无法加载 PDF 中文字体 NotoSansSC-Regular.ttf", e);
        }
    }

    protected String style()
    {
        return """
                <style>
                  @page { size: A4; margin: 14mm; }
                  body { font-family: 'NotoSansSC'; color: #222; font-size: 12px; }
                  .title { text-align: center; font-size: 20px; font-weight: bold; }
                  .sub { text-align: center; color: #666; font-size: 11px; margin-top: 2px; }
                  table.info { width: 100%; margin-top: 10px; }
                  table.info td { padding: 3px 6px; font-size: 12px; vertical-align: top; }
                  table.info td.k { color: #888; width: 13%; }
                  table.info td.v { width: 37%; }
                  table.grid { width: 100%; margin-top: 12px; border-collapse: collapse; }
                  table.grid th, table.grid td { border: 1px solid #bbb; padding: 5px 6px; font-size: 11px; text-align: center; }
                  table.grid th { background: #f0f2f5; }
                  .amt { text-align: right; margin: 10px 0; font-size: 13px; }
                  .amt .big { font-size: 12px; color: #555; margin-left: 18px; }
                  .req { margin: 6px 0; font-size: 11px; line-height: 1.8; }
                  .req .p { color: #888; }
                  table.sign { width: 100%; margin-top: 46px; }
                  table.sign td { padding: 10px 6px; font-size: 12px; text-align: center; }
                </style>
                """;
    }

    protected String headerBlock()
    {
        StringBuilder sb = new StringBuilder();
        String logo = logoImg();
        if (logo != null)
        {
            sb.append("<div style='float:right'>").append(logo).append("</div>");
        }
        String name = esc(withDefault(docProperties.getCompanyName(), "企小侠MES"));
        sb.append("<div class='title'>").append(name).append(" · ").append(esc(documentTitle())).append("</div>");
        sb.append("<div class='sub'>").append(contactLine()).append("</div>");
        sb.append("<hr style='border:0;border-top:1px solid #ccc;margin:10px 0'/>");
        return sb.toString();
    }

    private String logoImg()
    {
        String path = docProperties.getLogoPath();
        if (path == null || path.isBlank())
        {
            return null;
        }
        try
        {
            String loc = path.startsWith("classpath:") ? path.substring("classpath:".length()) : path;
            byte[] bytes = StreamUtils.copyToByteArray(new ClassPathResource(loc).getInputStream());
            return "<img src='data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(bytes) + "' style='height:48px'/>";
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String contactLine()
    {
        StringBuilder sb = new StringBuilder();
        String phone = docProperties.getCompanyPhone();
        String addr = docProperties.getCompanyAddress();
        if (phone != null && !phone.isBlank())
        {
            sb.append("电话：").append(esc(phone));
        }
        if (addr != null && !addr.isBlank())
        {
            sb.append(sb.length() > 0 ? "　·　" : "").append("地址：").append(esc(addr));
        }
        return sb.toString();
    }

    // ── 通用格式化工具（子类直接复用） ──

    protected String kv(String label, String val)
    {
        return "<td class='k'>" + label + "</td><td class='v'>" + val + "</td>";
    }

    protected String td(String v) { return "<td>" + v + "</td>"; }

    protected String str(Object o) { return o == null ? "" : o.toString(); }

    protected String withDefault(String s, String dft) { return (s == null || s.isBlank()) ? dft : s; }

    protected String date(Date d) { return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(d); }

    protected String money(BigDecimal b) { return b == null ? "" : new DecimalFormat("#,##0.00").format(b); }

    protected String num(BigDecimal b)
    {
        return b == null ? "" : b.stripTrailingZeros().toPlainString();
    }

    protected String cnyUpper(BigDecimal b)
    {
        double v = b == null ? 0d : b.doubleValue();
        return "人民币 " + Convert.digitUppercase(v);
    }

    protected String esc(String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
