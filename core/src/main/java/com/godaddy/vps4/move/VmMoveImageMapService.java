package com.godaddy.vps4.move;

import com.godaddy.vps4.vm.ServerType;

public interface VmMoveImageMapService {

    VmMoveImageMap getVmMoveImageMap(long originalImageId, ServerType.Platform toPlatform);
}
