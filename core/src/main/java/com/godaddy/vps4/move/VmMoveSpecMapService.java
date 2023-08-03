package com.godaddy.vps4.move;

import com.godaddy.vps4.vm.ServerType;

public interface VmMoveSpecMapService {
    VmMoveSpecMap getVmMoveSpecMap(int originalSpecId, ServerType.Platform toPlatform);
}
