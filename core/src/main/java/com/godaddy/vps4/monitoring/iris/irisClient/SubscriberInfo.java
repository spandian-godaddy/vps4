
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SubscriberInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SubscriberInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SubscriberId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Source" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="IncidentType" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="PriorityId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="ServiceId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="GroupId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Category" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="PrivateLabelId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="NoteType" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Action" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Visibility" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Protection" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="AutoValidate" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="MarketCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Active" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubscriberInfo", propOrder = {
    "subscriberId",
    "description",
    "source",
    "incidentType",
    "priorityId",
    "serviceId",
    "groupId",
    "category",
    "privateLabelId",
    "noteType",
    "action",
    "visibility",
    "protection",
    "autoValidate",
    "marketCode",
    "active"
})
public class SubscriberInfo {

    @XmlElement(name = "SubscriberId")
    protected int subscriberId;
    @XmlElement(name = "Description")
    protected String description;
    @XmlElement(name = "Source")
    protected int source;
    @XmlElement(name = "IncidentType")
    protected int incidentType;
    @XmlElement(name = "PriorityId")
    protected int priorityId;
    @XmlElement(name = "ServiceId")
    protected int serviceId;
    @XmlElement(name = "GroupId")
    protected int groupId;
    @XmlElement(name = "Category")
    protected int category;
    @XmlElement(name = "PrivateLabelId")
    protected int privateLabelId;
    @XmlElement(name = "NoteType")
    protected int noteType;
    @XmlElement(name = "Action")
    protected int action;
    @XmlElement(name = "Visibility")
    protected int visibility;
    @XmlElement(name = "Protection")
    protected int protection;
    @XmlElement(name = "AutoValidate")
    protected boolean autoValidate;
    @XmlElement(name = "MarketCode")
    protected String marketCode;
    @XmlElement(name = "Active")
    protected boolean active;

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
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the source property.
     * 
     */
    public int getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     */
    public void setSource(int value) {
        this.source = value;
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
     * Gets the value of the category property.
     * 
     */
    public int getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     */
    public void setCategory(int value) {
        this.category = value;
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
     * Gets the value of the noteType property.
     * 
     */
    public int getNoteType() {
        return noteType;
    }

    /**
     * Sets the value of the noteType property.
     * 
     */
    public void setNoteType(int value) {
        this.noteType = value;
    }

    /**
     * Gets the value of the action property.
     * 
     */
    public int getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     */
    public void setAction(int value) {
        this.action = value;
    }

    /**
     * Gets the value of the visibility property.
     * 
     */
    public int getVisibility() {
        return visibility;
    }

    /**
     * Sets the value of the visibility property.
     * 
     */
    public void setVisibility(int value) {
        this.visibility = value;
    }

    /**
     * Gets the value of the protection property.
     * 
     */
    public int getProtection() {
        return protection;
    }

    /**
     * Sets the value of the protection property.
     * 
     */
    public void setProtection(int value) {
        this.protection = value;
    }

    /**
     * Gets the value of the autoValidate property.
     * 
     */
    public boolean isAutoValidate() {
        return autoValidate;
    }

    /**
     * Sets the value of the autoValidate property.
     * 
     */
    public void setAutoValidate(boolean value) {
        this.autoValidate = value;
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

    /**
     * Gets the value of the active property.
     * 
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the value of the active property.
     * 
     */
    public void setActive(boolean value) {
        this.active = value;
    }

}
