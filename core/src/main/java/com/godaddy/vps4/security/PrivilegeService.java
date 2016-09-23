package com.godaddy.vps4.security;

import com.godaddy.vps4.project.ProjectPrivilege;

public interface PrivilegeService {

    void requirePrivilege(User user, Privilege privilege);

    boolean checkPrivilege(User user, Privilege privilege);

    void requireAnyPrivilegeToSgid(User user, long sgid);

    boolean checkAnyPrivilegeToSgid(User user, long sgid);

    boolean checkPrivilege(User user, long sgid, ProjectPrivilege privilege);

    void requirePrivilege(User user, long sgid, ProjectPrivilege privilege);
}