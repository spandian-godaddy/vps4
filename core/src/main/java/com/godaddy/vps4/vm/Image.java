package com.godaddy.vps4.vm;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

public class Image {
    public enum OperatingSystem {
        LINUX(1), WINDOWS(2);

        private final int id;

        private final static Map<Integer, OperatingSystem> map = stream(OperatingSystem.values()).collect(toMap(os -> os.id, os -> os));

        OperatingSystem(int id) {
            this.id = id;
        }

        public static OperatingSystem valueOf(int id) {
            return map.get(id);
        }
    }

    public enum ControlPanel {
        // MYH means no control panel
        MYH(0), CPANEL(1), PLESK(2);

        private final int id;

        private final static Map<Integer, ControlPanel> map = stream(ControlPanel.values()).collect(toMap(cp -> cp.id, cp -> cp));

        ControlPanel(int id) {
            this.id = id;
        }

        public static ControlPanel valueOf(int id) {
            return map.get(id);
        }
    }

    public long imageId;

    /**
     * The human readable/display-able name of this image
     */
    public String imageName;

    /**
     * The name of this image in HFS
     * (the name that would be used interacting with the HFS
     *  VM vertical)
     */
    public String hfsName;

    /**
     * The {@link ControlPanel} installed on this image.
     *
     */
    public ControlPanel controlPanel;

    /**
     * The operating system this VM runs.
     *
     * In order to provision a VM, this must match the
     * {@link VirtualMachineCredit#operatingSystem}.
     */
    public OperatingSystem operatingSystem;

    /**
     * The Server Type that this image is valid on (platform and server type)
     */
    public ServerType serverType;
}
