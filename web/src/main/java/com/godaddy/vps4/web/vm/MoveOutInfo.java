package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmUser;

import java.util.List;
import java.util.UUID;

public class MoveOutInfo {
    public UUID entitlementId;
    public String serverName;
    public String specName;
    public String hfsImageName;
    public String hostname;
    public IpAddress primaryIpAddress;
    public List<IpAddress> additionalIps;
    public VmUser vmUser;
    public Vps4User vps4User;
    public List<Action> actions;
}
