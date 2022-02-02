package com.godaddy.vps4.project;

import java.util.List;

public interface ProjectService {

    List<Project> getProjects(long userId, boolean active);

    Project getProject(long projectId);

    Project createProject(String name, long userId, String sgidPrefix);

    Project createProjectAndPrivilegeWithSgid(String name, long userId, String sgid);

    Project deleteProject(long projectId);

    void updateProjectUser(long projectId, long id);
}
