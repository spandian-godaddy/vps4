package com.godaddy.vps4.security;

import java.util.UUID;

import com.godaddy.vps4.project.ProjectPrivilege;

public interface PrivilegeService {

    void requirePrivilege(Vps4User user, Privilege privilege);

    boolean checkPrivilege(Vps4User user, Privilege privilege);

    void requireAnyPrivilegeToVmId(Vps4User user, UUID id);

    void requireAnyPrivilegeToProjectId(Vps4User user, long projectId);

    boolean checkAnyPrivilegeToProjectId(Vps4User user, long projectId);

    boolean checkPrivilege(Vps4User user, long projectId, ProjectPrivilege privilege);

    void requirePrivilege(Vps4User user, long projectId, ProjectPrivilege privilege);
}