package com.godaddy.vps4.web.action;

import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.vm.Image;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class Orphans {
    public long hfsVmId;
    public String sgid;
    public String hfsVmStatus;
    public Image.ControlPanel controlPanel;

    public IpAddress ip;
    public NodePingCheck nodePingCheck;

    public List<Snapshot> snapshotList;

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
