
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GetDashboardReportByGroupResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getDashboardReportByGroupResult"
})
@XmlRootElement(name = "GetDashboardReportByGroupResponse")
public class GetDashboardReportByGroupResponse {

    @XmlElement(name = "GetDashboardReportByGroupResult")
    protected String getDashboardReportByGroupResult;

    /**
     * Gets the value of the getDashboardReportByGroupResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGetDashboardReportByGroupResult() {
        return getDashboardReportByGroupResult;
    }

    /**
     * Sets the value of the getDashboardReportByGroupResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGetDashboardReportByGroupResult(String value) {
        this.getDashboardReportByGroupResult = value;
    }

}
