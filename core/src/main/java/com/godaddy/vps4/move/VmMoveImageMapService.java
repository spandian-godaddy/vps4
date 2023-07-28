package com.godaddy.vps4.move;

import com.godaddy.vps4.vm.ServerType;

public interface VmMoveImageMapService {

    VmMoveImageMap getVmMoveImageMap(int originalImageId, ServerType.Platform toPlatform);
}
