package com.godaddy.hfs.servicediscovery;

import java.util.Arrays;
import java.util.List;


public class HfsServiceMetadata {
    
    public enum ServiceType {
        WEB,
        DAEMON,
        OTHER
    }
    
    private final String name;
    private final ServiceType type;
    private final List<String> locations;

    public HfsServiceMetadata(String name, ServiceType type, String ... locations) {
        this.name = name;
        this.type = type;
        this.locations = Arrays.asList(locations);
    }
    
    public String getServiceName() {
        return name;
    }
    
    public ServiceType getServiceType() {
        return type;
    }
    
    public List<String> getLocations() {
        return locations;
    }
    
}
