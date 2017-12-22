
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
 *         &lt;element name="subscriberID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="subject" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Note" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="customerEmailAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="originalIPAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="groupID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="serviceID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="privateLabelID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="shopperID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="createdBy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="employeeID" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "subscriberID",
    "subject",
    "note",
    "customerEmailAddress",
    "originalIPAddress",
    "groupID",
    "serviceID",
    "privateLabelID",
    "shopperID",
    "createdBy",
    "employeeID"
})
@XmlRootElement(name = "CreateIncidentInIRISByEmployee")
public class CreateIncidentInIRISByEmployee {

    protected int subscriberID;
    protected String subject;
    @XmlElement(name = "Note")
    protected String note;
    protected String customerEmailAddress;
    protected String originalIPAddress;
    protected int groupID;
    protected int serviceID;
    protected int privateLabelID;
    protected String shopperID;
    protected String createdBy;
    protected int employeeID;

    /**
     * Gets the value of the subscriberID property.
     * 
     */
    public int getSubscriberID() {
        return subscriberID;
    }

    /**
     * Sets the value of the subscriberID property.
     * 
     */
    public void setSubscriberID(int value) {
        this.subscriberID = value;
    }

    /**
     * Gets the value of the subject property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the value of the subject property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubject(String value) {
        this.subject = value;
    }

    /**
     * Gets the value of the note property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNote() {
        return note;
    }

    /**
     * Sets the value of the note property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNote(String value) {
        this.note = value;
    }

    /**
     * Gets the value of the customerEmailAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustomerEmailAddress() {
        return customerEmailAddress;
    }

    /**
     * Sets the value of the customerEmailAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustomerEmailAddress(String value) {
        this.customerEmailAddress = value;
    }

    /**
     * Gets the value of the originalIPAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalIPAddress() {
        return originalIPAddress;
    }

    /**
     * Sets the value of the originalIPAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalIPAddress(String value) {
        this.originalIPAddress = value;
    }

    /**
     * Gets the value of the groupID property.
     * 
     */
    public int getGroupID() {
        return groupID;
    }

    /**
     * Sets the value of the groupID property.
     * 
     */
    public void setGroupID(int value) {
        this.groupID = value;
    }

    /**
     * Gets the value of the serviceID property.
     * 
     */
    public int getServiceID() {
        return serviceID;
    }

    /**
     * Sets the value of the serviceID property.
     * 
     */
    public void setServiceID(int value) {
        this.serviceID = value;
    }

    /**
     * Gets the value of the privateLabelID property.
     * 
     */
    public int getPrivateLabelID() {
        return privateLabelID;
    }

    /**
     * Sets the value of the privateLabelID property.
     * 
     */
    public void setPrivateLabelID(int value) {
        this.privateLabelID = value;
    }

    /**
     * Gets the value of the shopperID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShopperID() {
        return shopperID;
    }

    /**
     * Sets the value of the shopperID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShopperID(String value) {
        this.shopperID = value;
    }

    /**
     * Gets the value of the createdBy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the value of the createdBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreatedBy(String value) {
        this.createdBy = value;
    }

    /**
     * Gets the value of the employeeID property.
     * 
     */
    public int getEmployeeID() {
        return employeeID;
    }

    /**
     * Sets the value of the employeeID property.
     * 
     */
    public void setEmployeeID(int value) {
        this.employeeID = value;
    }

}
