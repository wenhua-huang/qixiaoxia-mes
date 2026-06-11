package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 供应商对象 qxx_md_vendor
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdVendor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long vendorId;
    private Long factoryId;

    @Excel(name = "供应商编码")
    private String vendorCode;

    @Excel(name = "供应商全称")
    private String vendorName;

    private Long outsourceFactoryId;

    @Excel(name = "供应商简称")
    private String vendorNick;

    @Excel(name = "英文名称")
    private String vendorEn;

    @Excel(name = "供应商类型", readConverterExp = "MATERIAL=原材料供应商,OUTSOURCE=外协加工商,BOTH=两者皆是")
    private String vendorType;

    @Excel(name = "简介")
    private String vendorDes;

    private String vendorLogo;

    @Excel(name = "供应商等级")
    private String vendorLevel;

    private Integer vendorScore;

    @Excel(name = "地址")
    private String address;

    private String website;
    private String email;
    private String tel;

    @Excel(name = "联系人")
    private String contact1;

    private String contact1Tel;
    private String contact1Email;

    @Excel(name = "备用联系人")
    private String contact2;

    private String contact2Tel;
    private String contact2Email;

    @Excel(name = "社会信用代码")
    private String creditCode;

    @Excel(name = "结算方式")
    private String settlementType;

    @Excel(name = "付款条件")
    private String paymentTerms;

    @Excel(name = "合作状态", readConverterExp = "ACTIVE=合作中,INACTIVE=暂停,PENDING=待审核,STOPPED=终止")
    private String coopStatus;

    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    // getters & setters
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public Long getOutsourceFactoryId() { return outsourceFactoryId; }
    public void setOutsourceFactoryId(Long v) { this.outsourceFactoryId = v; }
    public String getVendorNick() { return vendorNick; }
    public void setVendorNick(String v) { this.vendorNick = v; }
    public String getVendorEn() { return vendorEn; }
    public void setVendorEn(String v) { this.vendorEn = v; }
    public String getVendorType() { return vendorType; }
    public void setVendorType(String v) { this.vendorType = v; }
    public String getVendorDes() { return vendorDes; }
    public void setVendorDes(String v) { this.vendorDes = v; }
    public String getVendorLogo() { return vendorLogo; }
    public void setVendorLogo(String v) { this.vendorLogo = v; }
    public String getVendorLevel() { return vendorLevel; }
    public void setVendorLevel(String v) { this.vendorLevel = v; }
    public Integer getVendorScore() { return vendorScore; }
    public void setVendorScore(Integer v) { this.vendorScore = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getWebsite() { return website; }
    public void setWebsite(String v) { this.website = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTel() { return tel; }
    public void setTel(String v) { this.tel = v; }
    public String getContact1() { return contact1; }
    public void setContact1(String v) { this.contact1 = v; }
    public String getContact1Tel() { return contact1Tel; }
    public void setContact1Tel(String v) { this.contact1Tel = v; }
    public String getContact1Email() { return contact1Email; }
    public void setContact1Email(String v) { this.contact1Email = v; }
    public String getContact2() { return contact2; }
    public void setContact2(String v) { this.contact2 = v; }
    public String getContact2Tel() { return contact2Tel; }
    public void setContact2Tel(String v) { this.contact2Tel = v; }
    public String getContact2Email() { return contact2Email; }
    public void setContact2Email(String v) { this.contact2Email = v; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String v) { this.creditCode = v; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String v) { this.settlementType = v; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String v) { this.paymentTerms = v; }
    public String getCoopStatus() { return coopStatus; }
    public void setCoopStatus(String v) { this.coopStatus = v; }
    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String v) { this.enableFlag = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("vendorId", getVendorId()).append("vendorCode", getVendorCode()).append("vendorName", getVendorName()).toString();
    }
}
