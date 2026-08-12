package com.ruoyi.common.utils.qr;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class QrPayloadUtilTest {

    @Test
    void build_card() {
        assertThat(QrPayloadUtil.build(QrPayloadType.CARD, "CRD20260620001"))
                .isEqualTo("QXX|CARD|CRD20260620001");
    }

    @Test
    void parse_card() {
        QrPayload p = QrPayloadUtil.parse("QXX|CARD|CRD20260620001");
        assertThat(p).isNotNull();
        assertThat(p.getType()).isEqualTo(QrPayloadType.CARD);
        assertThat(p.getCode()).isEqualTo("CRD20260620001");
    }

    @Test
    void parse_nonQxx_or_bareCode_returnsNull() {
        assertThat(QrPayloadUtil.parse("https://example.com")).isNull();
        assertThat(QrPayloadUtil.parse("CRD20260620001")).isNull(); // 裸卡号
        assertThat(QrPayloadUtil.parse(null)).isNull();
    }

    @Test
    void parse_unknownType_returnsNull() {
        assertThat(QrPayloadUtil.parse("QXX|FOO|bar")).isNull();
    }

    @Test
    void parse_emptyCode_returnsNull() {
        assertThat(QrPayloadUtil.parse("QXX|CARD|")).isNull();
    }

    @Test
    void build_nullArgs_throws() {
        assertThatThrownBy(() -> QrPayloadUtil.build(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QrPayloadUtil.build(QrPayloadType.CARD, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundtrip_allTypes() {
        for (QrPayloadType t : QrPayloadType.values()) {
            QrPayload p = QrPayloadUtil.parse(QrPayloadUtil.build(t, "CODE123"));
            assertThat(p).isNotNull();
            assertThat(p.getType()).isEqualTo(t);
            assertThat(p.getCode()).isEqualTo("CODE123");
        }
    }
}
