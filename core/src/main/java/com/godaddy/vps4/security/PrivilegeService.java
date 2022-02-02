package com.godaddy.vps4.security;

import java.util.UUID;

public interface PrivilegeService {

    void requireAnyPrivilegeToVmId(Vps4User user, UUID id);

    void requireAnyPrivilegeToProjectId(Vps4User user, long projectId);

    boolean checkAnyPrivilegeToProjectId(Vps4User user, long projectId);

}