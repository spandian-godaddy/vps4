
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
 *         &lt;element name="GetIncidentCustomerNotesResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "getIncidentCustomerNotesResult"
})
@XmlRootElement(name = "GetIncidentCustomerNotesResponse")
public class GetIncidentCustomerNotesResponse {

    @XmlElement(name = "GetIncidentCustomerNotesResult")
    protected String getIncidentCustomerNotesResult;

    /**
     * Gets the value of the getIncidentCustomerNotesResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGetIncidentCustomerNotesResult() {
        return getIncidentCustomerNotesResult;
    }

    /**
     * Sets the value of the getIncidentCustomerNotesResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGetIncidentCustomerNotesResult(String value) {
        this.getIncidentCustomerNotesResult = value;
    }

}
