
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CreateIncidentInput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CreateIncidentInput">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SubscriberId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Subject" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Note" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CustomerEmailAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="OriginalIpAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="GroupId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="ServiceId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="PrivateLabelId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="ShopperId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CreatedBy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="IncidentType" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="EmployeeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="CategoryId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="PriorityId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="NoteId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="SourceSubscriberId" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
@XmlType(name = "CreateIncidentInput", propOrder = {
    "subscriberId",
    "subject",
    "note",
    "customerEmailAddress",
    "originalIpAddress",
    "groupId",
    "serviceId",
    "privateLabelId",
    "shopperId",
    "createdBy",
    "incidentType",
    "employeeId",
    "categoryId",
    "priorityId",
    "noteId",
    "sourceSubscriberId",
    "marketCode"
})
public class CreateIncidentInput {

    @XmlElement(name = "SubscriberId")
    protected int subscriberId;
    @XmlElement(name = "Subject")
    protected String subject;
    @XmlElement(name = "Note")
    protected String note;
    @XmlElement(name = "CustomerEmailAddress")
    protected String customerEmailAddress;
    @XmlElement(name = "OriginalIpAddress")
    protected String originalIpAddress;
    @XmlElement(name = "GroupId")
    protected int groupId;
    @XmlElement(name = "ServiceId")
    protected int serviceId;
    @XmlElement(name = "PrivateLabelId")
    protected int privateLabelId;
    @XmlElement(name = "ShopperId")
    protected String shopperId;
    @XmlElement(name = "CreatedBy")
    protected String createdBy;
    @XmlElement(name = "IncidentType")
    protected int incidentType;
    @XmlElement(name = "EmployeeId")
    protected int employeeId;
    @XmlElement(name = "CategoryId")
    protected int categoryId;
    @XmlElement(name = "PriorityId")
    protected int priorityId;
    @XmlElement(name = "NoteId")
    protected int noteId;
    @XmlElement(name = "SourceSubscriberId")
    protected int sourceSubscriberId;
    @XmlElement(name = "MarketCode")
    protected String marketCode;

    /**
     * Gets the value of the subscriberId property.
     * 
     */
    public int getSubscriberId() {
        return subscriberId;
    }

    /**
     * Sets the value of the subscriberId property.
     * 
     */
    public void setSubscriberId(int value) {
        this.subscriberId = value;
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
     * Gets the value of the originalIpAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalIpAddress() {
        return originalIpAddress;
    }

    /**
     * Sets the value of the originalIpAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalIpAddress(String value) {
        this.originalIpAddress = value;
    }

    /**
     * Gets the value of the groupId property.
     * 
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Sets the value of the groupId property.
     * 
     */
    public void setGroupId(int value) {
        this.groupId = value;
    }

    /**
     * Gets the value of the serviceId property.
     * 
     */
    public int getServiceId() {
        return serviceId;
    }

    /**
     * Sets the value of the serviceId property.
     * 
     */
    public void setServiceId(int value) {
        this.serviceId = value;
    }

    /**
     * Gets the value of the privateLabelId property.
     * 
     */
    public int getPrivateLabelId() {
        return privateLabelId;
    }

    /**
     * Sets the value of the privateLabelId property.
     * 
     */
    public void setPrivateLabelId(int value) {
        this.privateLabelId = value;
    }

    /**
     * Gets the value of the shopperId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShopperId() {
        return shopperId;
    }

    /**
     * Sets the value of the shopperId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShopperId(String value) {
        this.shopperId = value;
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
     * Gets the value of the incidentType property.
     * 
     */
    public int getIncidentType() {
        return incidentType;
    }

    /**
     * Sets the value of the incidentType property.
     * 
     */
    public void setIncidentType(int value) {
        this.incidentType = value;
    }

    /**
     * Gets the value of the employeeId property.
     * 
     */
    public int getEmployeeId() {
        return employeeId;
    }

    /**
     * Sets the value of the employeeId property.
     * 
     */
    public void setEmployeeId(int value) {
        this.employeeId = value;
    }

    /**
     * Gets the value of the categoryId property.
     * 
     */
    public int getCategoryId() {
        return categoryId;
    }

    /**
     * Sets the value of the categoryId property.
     * 
     */
    public void setCategoryId(int value) {
        this.categoryId = value;
    }

    /**
     * Gets the value of the priorityId property.
     * 
     */
    public int getPriorityId() {
        return priorityId;
    }

    /**
     * Sets the value of the priorityId property.
     * 
     */
    public void setPriorityId(int value) {
        this.priorityId = value;
    }

    /**
     * Gets the value of the noteId property.
     * 
     */
    public int getNoteId() {
        return noteId;
    }

    /**
     * Sets the value of the noteId property.
     * 
     */
    public void setNoteId(int value) {
        this.noteId = value;
    }

    /**
     * Gets the value of the sourceSubscriberId property.
     * 
     */
    public int getSourceSubscriberId() {
        return sourceSubscriberId;
    }

    /**
     * Sets the value of the sourceSubscriberId property.
     * 
     */
    public void setSourceSubscriberId(int value) {
        this.sourceSubscriberId = value;
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
