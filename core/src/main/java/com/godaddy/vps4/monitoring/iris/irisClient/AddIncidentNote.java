
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
 *         &lt;element name="incidentID" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="Note" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="NTLogin" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "note",
    "ntLogin"
})
@XmlRootElement(name = "AddIncidentNote")
public class AddIncidentNote {

    protected long incidentID;
    @XmlElement(name = "Note")
    protected String note;
    @XmlElement(name = "NTLogin")
    protected String ntLogin;

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

}
