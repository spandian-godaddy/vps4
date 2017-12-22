
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
 *         &lt;element name="GetIncidentInfoByIncidentIdResult" type="{http://tempuri.org/}IncidentInfo" minOccurs="0"/>
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
    "getIncidentInfoByIncidentIdResult"
})
@XmlRootElement(name = "GetIncidentInfoByIncidentIdResponse")
public class GetIncidentInfoByIncidentIdResponse {

    @XmlElement(name = "GetIncidentInfoByIncidentIdResult")
    protected IncidentInfo getIncidentInfoByIncidentIdResult;

    /**
     * Gets the value of the getIncidentInfoByIncidentIdResult property.
     * 
     * @return
     *     possible object is
     *     {@link IncidentInfo }
     *     
     */
    public IncidentInfo getGetIncidentInfoByIncidentIdResult() {
        return getIncidentInfoByIncidentIdResult;
    }

    /**
     * Sets the value of the getIncidentInfoByIncidentIdResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link IncidentInfo }
     *     
     */
    public void setGetIncidentInfoByIncidentIdResult(IncidentInfo value) {
        this.getIncidentInfoByIncidentIdResult = value;
    }

}
