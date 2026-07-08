package com.ruoyi.system.domain.mes.wm;

import java.util.List;

/**
 * 移动端收货一键提交请求体 — 头 + 行一次性传入，后端原子化创建+确认。
 *
 * 原则：前端只调一个接口，事务由后端保证。
 */
public class ItemRecptReceiveBody {

    /** 入库单头信息 */
    private WmItemRecpt header;

    /** 入库单行列表 */
    private List<WmItemRecptLine> lines;

    public WmItemRecpt getHeader() { return header; }
    public void setHeader(WmItemRecpt header) { this.header = header; }

    public List<WmItemRecptLine> getLines() { return lines; }
    public void setLines(List<WmItemRecptLine> lines) { this.lines = lines; }
}
