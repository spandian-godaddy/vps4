package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VmUser;

import java.util.List;
import java.util.UUID;

public class MoveOutInfo {
    public UUID entitlementId;
    public String serverName;
    public ServerSpec spec;
    public Image image;
    public String hostname;
    public Project project;
    public IpAddress primaryIpAddress;
    public List<IpAddress> additionalIps;
    public PanoptaDetail panoptaDetail;
    public VmUser vmUser;
    public Vps4User vps4User;
    public List<Action> actions;
    public UUID commandId;
}
