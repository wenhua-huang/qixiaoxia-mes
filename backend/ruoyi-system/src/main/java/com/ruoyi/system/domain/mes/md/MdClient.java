package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户对象 qxx_md_client
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdClient extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long clientId;
    private Long factoryId;

    @Excel(name = "客户编码")
    private String clientCode;

    @Excel(name = "客户全称")
    private String clientName;

    @Excel(name = "客户简称")
    private String clientNick;

    @Excel(name = "英文名称")
    private String clientEn;

    @Excel(name = "客户类型", readConverterExp = "DOMESTIC=内贸,FOREIGN=外贸,SPOT=现货")
    private String clientType;

    @Excel(name = "业务员")
    private String salesperson;

    @Excel(name = "社会信用代码")
    private String creditCode;

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

    @Excel(name = "收货地址")
    private String shippingAddress;

    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    // getters & setters
    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { this.clientCode = v; }
    public String getClientName() { return clientName; }
    public void setClientName(String v) { this.clientName = v; }
    public String getClientNick() { return clientNick; }
    public void setClientNick(String v) { this.clientNick = v; }
    public String getClientEn() { return clientEn; }
    public void setClientEn(String v) { this.clientEn = v; }
    public String getClientType() { return clientType; }
    public void setClientType(String v) { this.clientType = v; }
    public String getSalesperson() { return salesperson; }
    public void setSalesperson(String v) { this.salesperson = v; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String v) { this.creditCode = v; }
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
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String v) { this.shippingAddress = v; }
    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String v) { this.enableFlag = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("clientId", getClientId()).append("clientCode", getClientCode()).append("clientName", getClientName()).toString();
    }
}
