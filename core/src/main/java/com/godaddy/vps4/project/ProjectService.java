package com.godaddy.vps4.project;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    List<Project> getServiceGroups(long userId, boolean active);

    Project getServiceGroup(long sgid);

    Project createServiceGroup(String name, long userId, UUID account, short dataCenterId);

    Project deleteServiceGroup(long sgid);

}
