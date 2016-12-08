package com.godaddy.vps4.web.servicediscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceRegistration {

    public String name;
    public final List<String> locations = new ArrayList<>();
    public String address;
    public int sslPort;
    public int port;
    public UUID id = UUID.randomUUID();
}
