
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SearchSubscriberInput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SearchSubscriberInput">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SubscriberDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="SubscriberNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="IsInternal" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="MarketCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchSubscriberInput", propOrder = {
    "subscriberDescription",
    "subscriberNumber",
    "isInternal",
    "marketCode"
})
public class SearchSubscriberInput {

    @XmlElement(name = "SubscriberDescription")
    protected String subscriberDescription;
    @XmlElement(name = "SubscriberNumber")
    protected int subscriberNumber;
    @XmlElement(name = "IsInternal")
    protected boolean isInternal;
    @XmlElement(name = "MarketCode")
    protected String marketCode;

    /**
     * Gets the value of the subscriberDescription property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubscriberDescription() {
        return subscriberDescription;
    }

    /**
     * Sets the value of the subscriberDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubscriberDescription(String value) {
        this.subscriberDescription = value;
    }

    /**
     * Gets the value of the subscriberNumber property.
     * 
     */
    public int getSubscriberNumber() {
        return subscriberNumber;
    }

    /**
     * Sets the value of the subscriberNumber property.
     * 
     */
    public void setSubscriberNumber(int value) {
        this.subscriberNumber = value;
    }

    /**
     * Gets the value of the isInternal property.
     * 
     */
    public boolean isIsInternal() {
        return isInternal;
    }

    /**
     * Sets the value of the isInternal property.
     * 
     */
    public void setIsInternal(boolean value) {
        this.isInternal = value;
    }

    /**
     * Gets the value of the marketCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMarketCode() {
        return marketCode;
    }

    /**
     * Sets the value of the marketCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMarketCode(String value) {
        this.marketCode = value;
    }

}
