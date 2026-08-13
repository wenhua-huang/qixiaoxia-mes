package com.ruoyi.web.controller.mes.export;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.common.config.DocProperties;
import com.ruoyi.common.core.text.Convert;

/**
 * 单据 Excel 导出基类（对内版）。
 * 封装 POI 脚手架：Workbook 生命周期、公司抬头、单元格样式集合、
 * info 行三栏布局、金额/日期格式化、人民币大写。子类实现各 section 写入方法。
 *
 * @param <T> 单据数据类型
 * @author qixiaoxia
 */
public abstract class AbstractExcelExporter<T>
{
    @Autowired
    protected DocProperties docProperties;

    /** 工作表名 */
    protected abstract String sheetName();

    /** 标题后缀，如"销售订单详情" */
    protected abstract String titleSuffix();

    /** 写入各 section，返回最后行号 +1 */
    protected abstract int writeSections(Sheet sh, int row, Styles st, T data);

    /** 列宽数组 */
    protected abstract int[] columnWidths();

    /** 渲染 Excel 到输出流 */
    public void write(T data, OutputStream out) throws IOException
    {
        Workbook wb = new XSSFWorkbook();
        try
        {
            Styles st = new Styles(wb);
            Sheet sheet = wb.createSheet(sheetName());
            int row = 0;
            row = writeTitle(sheet, row, st);
            row = writeSections(sheet, row, st, data);
            setColumnWidths(sheet);
            wb.write(out);
        }
        finally
        {
            wb.close();
        }
    }

    private int writeTitle(Sheet sh, int r, Styles st)
    {
        Row row = sh.createRow(r);
        Cell c = row.createCell(0);
        c.setCellValue(withDefault(docProperties.getCompanyName(), "企小侠MES") + " · " + titleSuffix());
        c.setCellStyle(st.title);
        int cols = columnWidths().length;
        sh.addMergedRegion(new CellRangeAddress(r, r, 0, cols - 1));
        row.setHeightInPoints(26);
        return r + 2;
    }

    // ── 子类复用的 helper ──

    protected void infoRow(Sheet sh, int r, Styles st, String l1, String v1, String l2, String v2, String l3, String v3)
    {
        Row row = sh.createRow(r);
        setPair(sh, row, 0, l1, v1, st);
        setPair(sh, row, 3, l2, v2, st);
        setPair(sh, row, 6, l3, v3, st);
    }

    protected void setPair(Sheet sh, Row row, int start, String label, String val, Styles st)
    {
        Cell lc = row.createCell(start);
        lc.setCellValue(label);
        lc.setCellStyle(st.label);
        Cell vc = row.createCell(start + 1);
        vc.setCellValue(val);
        vc.setCellStyle(st.value);
        sh.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), start + 1, start + 2));
    }

    protected void txt(Row row, int col, CellStyle style, String val)
    {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    protected void money(Row row, int col, CellStyle style, BigDecimal val)
    {
        Cell c = row.createCell(col);
        if (val != null)
        {
            c.setCellValue(val.doubleValue());
        }
        c.setCellStyle(style);
    }

    protected String str(Object o) { return o == null ? "" : o.toString(); }

    protected String withDefault(String s, String dft) { return (s == null || s.isBlank()) ? dft : s; }

    protected String date(Date d) { return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(d); }

    protected String dateTime(Date d) { return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(d); }

    protected String moneyFmt(BigDecimal b) { return b == null ? "" : new DecimalFormat("#,##0.00").format(b); }

    protected String num(BigDecimal b) { return b == null ? "" : b.stripTrailingZeros().toPlainString(); }

    protected String cnyUpper(BigDecimal b)
    {
        return "人民币 " + Convert.digitUppercase(b == null ? 0d : b.doubleValue());
    }

    private void setColumnWidths(Sheet sh)
    {
        int[] w = columnWidths();
        for (int i = 0; i < w.length; i++)
        {
            sh.setColumnWidth(i, w[i]);
        }
    }

    /** 集中管理单元格样式 */
    public static class Styles
    {
        public final CellStyle title;
        public final CellStyle label;
        public final CellStyle value;
        public final CellStyle head;
        public final CellStyle cell;
        public final CellStyle money;
        public final CellStyle moneyBold;
        public final CellStyle totalLabel;

        Styles(Workbook wb)
        {
            title = wb.createCellStyle();
            title.setFont(font(wb, 16, true));
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);
            label = wb.createCellStyle();
            label.setFont(font(wb, 11, true));
            label.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            label.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            label.setAlignment(HorizontalAlignment.CENTER);
            value = wb.createCellStyle();
            value.setFont(font(wb, 11, false));
            value.setAlignment(HorizontalAlignment.LEFT);
            head = wb.createCellStyle();
            head.setFont(font(wb, 11, true));
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setAlignment(HorizontalAlignment.CENTER);
            border(head);
            cell = wb.createCellStyle();
            cell.setAlignment(HorizontalAlignment.CENTER);
            border(cell);
            money = moneyStyle(wb, false);
            moneyBold = moneyStyle(wb, true);
            totalLabel = wb.createCellStyle();
            totalLabel.setFont(font(wb, 11, true));
            totalLabel.setAlignment(HorizontalAlignment.RIGHT);
        }

        private static Font font(Workbook wb, int size, boolean bold)
        {
            Font f = wb.createFont();
            f.setFontHeightInPoints((short) size);
            f.setBold(bold);
            return f;
        }

        private static CellStyle moneyStyle(Workbook wb, boolean bold)
        {
            CellStyle s = wb.createCellStyle();
            s.setFont(font(wb, 11, bold));
            s.setAlignment(HorizontalAlignment.RIGHT);
            DataFormat df = wb.createDataFormat();
            s.setDataFormat(df.getFormat("#,##0.00"));
            border(s);
            return s;
        }

        private static void border(CellStyle s)
        {
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }
}
