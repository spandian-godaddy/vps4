
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
 *         &lt;element name="CreateQuickIncidentinIRISResult" type="{http://www.w3.org/2001/XMLSchema}long"/>
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
    "createQuickIncidentinIRISResult"
})
@XmlRootElement(name = "CreateQuickIncidentinIRISResponse")
public class CreateQuickIncidentinIRISResponse {

    @XmlElement(name = "CreateQuickIncidentinIRISResult")
    protected long createQuickIncidentinIRISResult;

    /**
     * Gets the value of the createQuickIncidentinIRISResult property.
     * 
     */
    public long getCreateQuickIncidentinIRISResult() {
        return createQuickIncidentinIRISResult;
    }

    /**
     * Sets the value of the createQuickIncidentinIRISResult property.
     * 
     */
    public void setCreateQuickIncidentinIRISResult(long value) {
        this.createQuickIncidentinIRISResult = value;
    }

}
