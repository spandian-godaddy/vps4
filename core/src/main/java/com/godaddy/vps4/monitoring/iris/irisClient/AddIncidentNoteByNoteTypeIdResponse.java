
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
 *         &lt;element name="AddIncidentNoteByNoteTypeIdResult" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "addIncidentNoteByNoteTypeIdResult"
})
@XmlRootElement(name = "AddIncidentNoteByNoteTypeIdResponse")
public class AddIncidentNoteByNoteTypeIdResponse {

    @XmlElement(name = "AddIncidentNoteByNoteTypeIdResult")
    protected int addIncidentNoteByNoteTypeIdResult;

    /**
     * Gets the value of the addIncidentNoteByNoteTypeIdResult property.
     * 
     */
    public int getAddIncidentNoteByNoteTypeIdResult() {
        return addIncidentNoteByNoteTypeIdResult;
    }

    /**
     * Sets the value of the addIncidentNoteByNoteTypeIdResult property.
     * 
     */
    public void setAddIncidentNoteByNoteTypeIdResult(int value) {
        this.addIncidentNoteByNoteTypeIdResult = value;
    }

}
