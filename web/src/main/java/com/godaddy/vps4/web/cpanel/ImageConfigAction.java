package com.godaddy.vps4.web.cpanel;

import com.godaddy.vps4.web.Action;

public class ImageConfigAction extends Action {

    public long vmId;
    public String publicIp;

    public ImageConfigAction(long vmId, String publicIp) {
        this.vmId = vmId;
        this.publicIp = publicIp;
    }
}
