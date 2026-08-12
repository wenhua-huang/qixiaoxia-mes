package com.ruoyi.common.utils.qr;

/**
 * 二维码 payload 类型前缀（见设计文档 §3.1）。
 */
public enum QrPayloadType {
    CARD, MAT, ROLL, WO, PKG;

    public static QrPayloadType from(String name) {
        if (name == null) {
            return null;
        }
        try {
            return QrPayloadType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
