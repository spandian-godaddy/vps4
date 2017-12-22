
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IncidentInput complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IncidentInput">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="IncidentId" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="IncludeNotes" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="EmployeeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IncidentInput", propOrder = {
    "incidentId",
    "includeNotes",
    "employeeId"
})
public class IncidentInput {

    @XmlElement(name = "IncidentId")
    protected long incidentId;
    @XmlElement(name = "IncludeNotes")
    protected boolean includeNotes;
    @XmlElement(name = "EmployeeId")
    protected int employeeId;

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
     * Gets the value of the includeNotes property.
     * 
     */
    public boolean isIncludeNotes() {
        return includeNotes;
    }

    /**
     * Sets the value of the includeNotes property.
     * 
     */
    public void setIncludeNotes(boolean value) {
        this.includeNotes = value;
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

}
