package com.godaddy.vps4.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.snapshot.SnapshotAction;


public class ActionInitiatedByFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
            throws Exception {
        if (pojo instanceof VmAction && writer.getName().equalsIgnoreCase("initiatedBy")
                && !((VmAction) pojo).isRequesterEmployee) {
            return;
        } else if (pojo instanceof SnapshotAction && writer.getName().equalsIgnoreCase("initiatedBy")
                && !((SnapshotAction) pojo).isRequesterEmployee) {
            return;
        }
        writer.serializeAsField(pojo, jgen, prov);
    }
}
