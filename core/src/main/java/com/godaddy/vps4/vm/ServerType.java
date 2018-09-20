package com.godaddy.vps4.vm;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;


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
        OPENSTACK(1), OVH(2);

        private final int platformId;

        Platform(int platformId) {
            this.platformId = platformId;
        }

        private final static Map<Integer, Platform> map = stream(Platform.values())
                .collect(toMap(type -> type.platformId, type -> type));

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
