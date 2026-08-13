package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 单据导出抬头配置（销售订单 PDF/Excel 对外单据的公司信息）
 * 对应 application.yml 的 qxx.doc 配置块
 *
 * @author qixiaoxia
 */
@Component
@ConfigurationProperties(prefix = "qxx.doc")
public class DocProperties
{
    /** 公司名称 */
    private String companyName;

    /** 联系电话 */
    private String companyPhone;

    /** 公司地址 */
    private String companyAddress;

    /** Logo 资源路径（classpath: 或 file:，可选，留空则不显示 Logo） */
    private String logoPath;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
}
