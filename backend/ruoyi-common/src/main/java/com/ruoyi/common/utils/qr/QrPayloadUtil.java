package com.ruoyi.common.utils.qr;

/**
 * 二维码 payload 构造与解析工具。格式：QXX|TYPE|CODE
 * 设计文档 §3.1。
 */
public final class QrPayloadUtil {

    private static final String PREFIX = "QXX";
    private static final String SEP = "\\|";

    private QrPayloadUtil() {
    }

    public static String build(QrPayloadType type, String code) {
        if (type == null || code == null || code.isEmpty()) {
            throw new IllegalArgumentException("type 与 code 不能为空");
        }
        return PREFIX + "|" + type.name() + "|" + code;
    }

    public static QrPayload parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(SEP, 3);
        if (parts.length < 3 || !PREFIX.equals(parts[0])) {
            return null;
        }
        QrPayloadType type = QrPayloadType.from(parts[1]);
        if (type == null) {
            return null;
        }
        String code = parts[2];
        if (code == null || code.isEmpty()) {
            return null;
        }
        return new QrPayload(type, code);
    }
}
