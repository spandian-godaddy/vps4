package com.godaddy.vps4.vm;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;


public class ServerType {
    public enum Type {
        VIRTUAL(1), DEDICATED(2);

        private final int typeId;

        Type(int typeId) {
            this.typeId = typeId;
        }

        private final static Map<Integer, Type> map = stream(Type.values())
                .collect(toMap(type -> type.typeId, type -> type));

        public static Type valueOf(int typeId) {
            return map.get(typeId);
        }

        public int getTypeId() {
            return typeId;
        }
    }

    public enum Platform {
        OPENSTACK(1) {
            @Override public String getZone() {return "openstack.zone";}
            @Override public String getProvisionCommand() {return "ProvisionVm";}
            @Override public String getDestroyCommand() {return "Vps4DestroyVm";}
            @Override public String getRebuildCommand() {return "Vps4RebuildVm";}
            @Override public String getRestoreCommand() {return "Vps4RestoreVm";}
            @Override public String getUpgradeCommand() {return "Vps4UpgradeVm";}
        },
        OVH(2) {
            @Override public String getZone() {return "ovh.zone";}
            @Override public String getProvisionCommand() {return "ProvisionDedicated";}
            @Override public String getDestroyCommand() {return "Vps4DestroyDedicated";}
            @Override public String getRebuildCommand() {return "Vps4RebuildDedicated";}
            @Override public String getRestoreCommand() {
                throw new UnsupportedOperationException("Not yet implemented for OH");
            }
            @Override public String getUpgradeCommand() {
                throw new UnsupportedOperationException("Not supported for OH");
            }
        },
        OPTIMIZED_HOSTING(3) {
            @Override public String getZone() {return "optimizedHosting.zone";}
            @Override public String getProvisionCommand() {return "ProvisionOHVm";}
            @Override public String getDestroyCommand() {return "Vps4DestroyOHVm";}
            @Override public String getRebuildCommand() {return "Vps4RebuildOHVm";}
            @Override public String getRestoreCommand() {return "Vps4RestoreOHVm";}
            @Override public String getUpgradeCommand() {return "Vps4UpgradeOHVm";}
        };

        private final int platformId;

        Platform(int platformId) {
            this.platformId = platformId;
        }

        private final static Map<Integer, Platform> map = stream(Platform.values())
                .collect(toMap(type -> type.platformId, type -> type));

        public abstract String getZone();
        public abstract String getProvisionCommand();
        public abstract String getDestroyCommand();
        public abstract String getRestoreCommand();
        public abstract String getRebuildCommand();
        public abstract String getUpgradeCommand();

        public static Platform valueOf(int platformId) {
            return map.get(platformId);
        }

        public int getplatformId() {
            return platformId;
        }
    }

    public int serverTypeId;

    /**
     * The virtualization type of the server.  Currently Virtual or Dedicated
     */
    public Type serverType;

    /**
     * The platform used to house the server.
     */
    public Platform platform;

}
