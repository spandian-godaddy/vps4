
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
 *         &lt;element name="GetSubscriberByCriteriaResult" type="{http://tempuri.org/}SubscriberInfo" minOccurs="0"/>
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
    "getSubscriberByCriteriaResult"
})
@XmlRootElement(name = "GetSubscriberByCriteriaResponse")
public class GetSubscriberByCriteriaResponse {

    @XmlElement(name = "GetSubscriberByCriteriaResult")
    protected SubscriberInfo getSubscriberByCriteriaResult;

    /**
     * Gets the value of the getSubscriberByCriteriaResult property.
     * 
     * @return
     *     possible object is
     *     {@link SubscriberInfo }
     *     
     */
    public SubscriberInfo getGetSubscriberByCriteriaResult() {
        return getSubscriberByCriteriaResult;
    }

    /**
     * Sets the value of the getSubscriberByCriteriaResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link SubscriberInfo }
     *     
     */
    public void setGetSubscriberByCriteriaResult(SubscriberInfo value) {
        this.getSubscriberByCriteriaResult = value;
    }

}
