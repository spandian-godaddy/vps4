
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
 *         &lt;element name="CreateIrisIncidentResult" type="{http://www.w3.org/2001/XMLSchema}long"/>
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
    "createIrisIncidentResult"
})
@XmlRootElement(name = "CreateIrisIncidentResponse")
public class CreateIrisIncidentResponse {

    @XmlElement(name = "CreateIrisIncidentResult")
    protected long createIrisIncidentResult;

    /**
     * Gets the value of the createIrisIncidentResult property.
     * 
     */
    public long getCreateIrisIncidentResult() {
        return createIrisIncidentResult;
    }

    /**
     * Sets the value of the createIrisIncidentResult property.
     * 
     */
    public void setCreateIrisIncidentResult(long value) {
        this.createIrisIncidentResult = value;
    }

}
