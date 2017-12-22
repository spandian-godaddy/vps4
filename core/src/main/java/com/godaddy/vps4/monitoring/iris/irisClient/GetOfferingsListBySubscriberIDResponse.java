
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
 *         &lt;element name="GetOfferingsListBySubscriberIDResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "getOfferingsListBySubscriberIDResult"
})
@XmlRootElement(name = "GetOfferingsListBySubscriberIDResponse")
public class GetOfferingsListBySubscriberIDResponse {

    @XmlElement(name = "GetOfferingsListBySubscriberIDResult")
    protected String getOfferingsListBySubscriberIDResult;

    /**
     * Gets the value of the getOfferingsListBySubscriberIDResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGetOfferingsListBySubscriberIDResult() {
        return getOfferingsListBySubscriberIDResult;
    }

    /**
     * Sets the value of the getOfferingsListBySubscriberIDResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGetOfferingsListBySubscriberIDResult(String value) {
        this.getOfferingsListBySubscriberIDResult = value;
    }

}
