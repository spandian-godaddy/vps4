
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="input" type="{http://tempuri.org/}CreateIncidentInput" minOccurs="0"/>
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
    "input"
})
@XmlRootElement(name = "CreateIrisIncident")
public class CreateIrisIncident {

    protected CreateIncidentInput input;

    /**
     * Gets the value of the input property.
     * 
     * @return
     *     possible object is
     *     {@link CreateIncidentInput }
     *     
     */
    public CreateIncidentInput getInput() {
        return input;
    }

    /**
     * Sets the value of the input property.
     * 
     * @param value
     *     allowed object is
     *     {@link CreateIncidentInput }
     *     
     */
    public void setInput(CreateIncidentInput value) {
        this.input = value;
    }

}
