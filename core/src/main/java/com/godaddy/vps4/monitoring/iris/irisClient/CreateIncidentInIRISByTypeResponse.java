
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
 *         &lt;element name="CreateIncidentInIRISByTypeResult" type="{http://www.w3.org/2001/XMLSchema}long"/>
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
    "createIncidentInIRISByTypeResult"
})
@XmlRootElement(name = "CreateIncidentInIRISByTypeResponse")
public class CreateIncidentInIRISByTypeResponse {

    @XmlElement(name = "CreateIncidentInIRISByTypeResult")
    protected long createIncidentInIRISByTypeResult;

    /**
     * Gets the value of the createIncidentInIRISByTypeResult property.
     * 
     */
    public long getCreateIncidentInIRISByTypeResult() {
        return createIncidentInIRISByTypeResult;
    }

    /**
     * Sets the value of the createIncidentInIRISByTypeResult property.
     * 
     */
    public void setCreateIncidentInIRISByTypeResult(long value) {
        this.createIncidentInIRISByTypeResult = value;
    }

}
