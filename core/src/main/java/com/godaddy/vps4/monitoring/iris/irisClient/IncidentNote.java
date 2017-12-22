
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IncidentNote complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IncidentNote">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="IncidentNoteId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="IrisId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="IncidentNoteType" type="{http://tempuri.org/}NoteType"/>
 *         &lt;element name="Note" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IncidentNote", propOrder = {
    "incidentNoteId",
    "irisId",
    "incidentNoteType",
    "note"
})
public class IncidentNote {

    @XmlElement(name = "IncidentNoteId")
    protected int incidentNoteId;
    @XmlElement(name = "IrisId")
    protected int irisId;
    @XmlElement(name = "IncidentNoteType", required = true)
    @XmlSchemaType(name = "string")
    protected NoteType incidentNoteType;
    @XmlElement(name = "Note")
    protected String note;

    /**
     * Gets the value of the incidentNoteId property.
     * 
     */
    public int getIncidentNoteId() {
        return incidentNoteId;
    }

    /**
     * Sets the value of the incidentNoteId property.
     * 
     */
    public void setIncidentNoteId(int value) {
        this.incidentNoteId = value;
    }

    /**
     * Gets the value of the irisId property.
     * 
     */
    public int getIrisId() {
        return irisId;
    }

    /**
     * Sets the value of the irisId property.
     * 
     */
    public void setIrisId(int value) {
        this.irisId = value;
    }

    /**
     * Gets the value of the incidentNoteType property.
     * 
     * @return
     *     possible object is
     *     {@link NoteType }
     *     
     */
    public NoteType getIncidentNoteType() {
        return incidentNoteType;
    }

    /**
     * Sets the value of the incidentNoteType property.
     * 
     * @param value
     *     allowed object is
     *     {@link NoteType }
     *     
     */
    public void setIncidentNoteType(NoteType value) {
        this.incidentNoteType = value;
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

}
