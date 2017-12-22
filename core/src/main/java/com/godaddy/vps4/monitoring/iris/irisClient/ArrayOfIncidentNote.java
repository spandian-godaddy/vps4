
package com.godaddy.vps4.monitoring.iris.irisClient;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfIncidentNote complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfIncidentNote">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="IncidentNote" type="{http://tempuri.org/}IncidentNote" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfIncidentNote", propOrder = {
    "incidentNote"
})
public class ArrayOfIncidentNote {

    @XmlElement(name = "IncidentNote", nillable = true)
    protected List<IncidentNote> incidentNote;

    /**
     * Gets the value of the incidentNote property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the incidentNote property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIncidentNote().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IncidentNote }
     * 
     * 
     */
    public List<IncidentNote> getIncidentNote() {
        if (incidentNote == null) {
            incidentNote = new ArrayList<IncidentNote>();
        }
        return this.incidentNote;
    }

}
