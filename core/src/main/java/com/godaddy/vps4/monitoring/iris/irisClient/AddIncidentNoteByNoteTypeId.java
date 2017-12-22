
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
 *         &lt;element name="incidentId" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="note" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="NTLogin" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="noteTypeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "incidentId",
    "note",
    "ntLogin",
    "noteTypeId"
})
@XmlRootElement(name = "AddIncidentNoteByNoteTypeId")
public class AddIncidentNoteByNoteTypeId {

    protected long incidentId;
    protected String note;
    @XmlElement(name = "NTLogin")
    protected String ntLogin;
    protected int noteTypeId;

    /**
     * Gets the value of the incidentId property.
     * 
     */
    public long getIncidentId() {
        return incidentId;
    }

    /**
     * Sets the value of the incidentId property.
     * 
     */
    public void setIncidentId(long value) {
        this.incidentId = value;
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
     * Gets the value of the ntLogin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNTLogin() {
        return ntLogin;
    }

    /**
     * Sets the value of the ntLogin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNTLogin(String value) {
        this.ntLogin = value;
    }

    /**
     * Gets the value of the noteTypeId property.
     * 
     */
    public int getNoteTypeId() {
        return noteTypeId;
    }

    /**
     * Sets the value of the noteTypeId property.
     * 
     */
    public void setNoteTypeId(int value) {
        this.noteTypeId = value;
    }

}
