
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="incidentID" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="updatedBy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="newPriority" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "incidentID",
    "updatedBy",
    "newPriority"
})
@XmlRootElement(name = "UpdatePriority")
public class UpdatePriority {

    protected long incidentID;
    protected String updatedBy;
    protected int newPriority;

    /**
     * Gets the value of the incidentID property.
     * 
     */
    public long getIncidentID() {
        return incidentID;
    }

    /**
     * Sets the value of the incidentID property.
     * 
     */
    public void setIncidentID(long value) {
        this.incidentID = value;
    }

    /**
     * Gets the value of the updatedBy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the value of the updatedBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdatedBy(String value) {
        this.updatedBy = value;
    }

    /**
     * Gets the value of the newPriority property.
     * 
     */
    public int getNewPriority() {
        return newPriority;
    }

    /**
     * Sets the value of the newPriority property.
     * 
     */
    public void setNewPriority(int value) {
        this.newPriority = value;
    }

}
