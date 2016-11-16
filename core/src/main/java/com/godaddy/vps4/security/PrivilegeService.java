package com.godaddy.vps4.security;

import com.godaddy.vps4.project.ProjectPrivilege;

public interface PrivilegeService {

    void requirePrivilege(Vps4User user, Privilege privilege);

    boolean checkPrivilege(Vps4User user, Privilege privilege);

    void requireAnyPrivilegeToSgid(Vps4User user, long sgid);

    boolean checkAnyPrivilegeToSgid(Vps4User user, long sgid);

    boolean checkPrivilege(Vps4User user, long sgid, ProjectPrivilege privilege);

    void requirePrivilege(Vps4User user, long sgid, ProjectPrivilege privilege);
}