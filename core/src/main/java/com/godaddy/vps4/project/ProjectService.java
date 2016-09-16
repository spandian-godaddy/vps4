package com.godaddy.vps4.project;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    List<Project> getProjects(long userId, boolean active);

    Project getProject(long projectId);

    Project createProject(String name, long userId);
    
    Project deleteProject(long sgid);

}
