
package com.godaddy.vps4.monitoring.iris.irisClient;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for NoteType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="NoteType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Customer"/>
 *     &lt;enumeration value="Comment"/>
 *     &lt;enumeration value="Resolution"/>
 *     &lt;enumeration value="ResolutionWithoutNotification"/>
 *     &lt;enumeration value="Reply"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "NoteType")
@XmlEnum
public enum NoteType {

    @XmlEnumValue("Customer")
    CUSTOMER("Customer"),
    @XmlEnumValue("Comment")
    COMMENT("Comment"),
    @XmlEnumValue("Resolution")
    RESOLUTION("Resolution"),
    @XmlEnumValue("ResolutionWithoutNotification")
    RESOLUTION_WITHOUT_NOTIFICATION("ResolutionWithoutNotification"),
    @XmlEnumValue("Reply")
    REPLY("Reply");
    private final String value;

    NoteType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static NoteType fromValue(String v) {
        for (NoteType c: NoteType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
