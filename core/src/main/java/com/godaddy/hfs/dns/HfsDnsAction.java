package com.godaddy.hfs.dns;

import com.godaddy.vps4.vm.ActionStatus;

public class HfsDnsAction {

    public long dns_action_id;
    public long vm_id;
    public long address_id;
    public String action_type;
    public ActionStatus status;
    public String workflow_id;
    public String message;
    private String created;
    private String modified;
    private String completed;
}
