
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
 *         &lt;element name="UpdatePriorityResult" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "updatePriorityResult"
})
@XmlRootElement(name = "UpdatePriorityResponse")
public class UpdatePriorityResponse {

    @XmlElement(name = "UpdatePriorityResult")
    protected int updatePriorityResult;

    /**
     * Gets the value of the updatePriorityResult property.
     * 
     */
    public int getUpdatePriorityResult() {
        return updatePriorityResult;
    }

    /**
     * Sets the value of the updatePriorityResult property.
     * 
     */
    public void setUpdatePriorityResult(int value) {
        this.updatePriorityResult = value;
    }

}
