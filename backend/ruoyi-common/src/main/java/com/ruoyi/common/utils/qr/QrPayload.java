package com.ruoyi.common.utils.qr;

/**
 * 二维码 payload 解析结果。
 */
public class QrPayload {

    private final QrPayloadType type;
    private final String code;

    public QrPayload(QrPayloadType type, String code) {
        this.type = type;
        this.code = code;
    }

    public QrPayloadType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }
}
