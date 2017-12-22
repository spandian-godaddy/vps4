
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
 *         &lt;element name="documentTypeID" type="{http://www.w3.org/2001/XMLSchema}int"/>
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
    "documentTypeID"
})
@XmlRootElement(name = "GetPrioritiesByDocumentType")
public class GetPrioritiesByDocumentType {

    protected int documentTypeID;

    /**
     * Gets the value of the documentTypeID property.
     * 
     */
    public int getDocumentTypeID() {
        return documentTypeID;
    }

    /**
     * Sets the value of the documentTypeID property.
     * 
     */
    public void setDocumentTypeID(int value) {
        this.documentTypeID = value;
    }

}
