package com.godaddy.vps4.orchestration.hfs;

import com.godaddy.hfs.sysadmin.SysAdminAction;

public class SysAdminActionNotCompletedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    final SysAdminAction sysAdminAction;

    public SysAdminActionNotCompletedException(SysAdminAction sysAdminAction) {
        this.sysAdminAction = sysAdminAction;
    }

    public SysAdminAction getAction() {
        return sysAdminAction;
    }
}
