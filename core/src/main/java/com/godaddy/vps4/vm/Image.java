package com.godaddy.vps4.vm;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Image {

    public static final String ISPCONFIG = "ispconfig";

    public enum OperatingSystem {
        LINUX(1), WINDOWS(2);

        private final int id;

        private final static Map<Integer, OperatingSystem> map = stream(OperatingSystem.values()).collect(toMap(os -> os.id, os -> os));

        OperatingSystem(int id) {
            this.id = id;
        }

        public int getOperatingSystemId() {
            return id;
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

        public static ControlPanel getEnumValueFromEcommName(String controlPanel) {
            switch (controlPanel.toLowerCase()) {
                case "plesk":
                case "pleskwebpro":
                case "pleskwebhost":
                    return PLESK;
                case "cpanel":
                    return CPANEL;
                case "myh":
                    return MYH;
                default:
                    throw new IllegalArgumentException(controlPanel + " is not a valid control panel");
            }
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
     */
    public OperatingSystem operatingSystem;

    /**
     * The Server Type that this image is valid on (platform and server type)
     */
    public ServerType serverType;

    public boolean hasCpanel() {
        return this.controlPanel == ControlPanel.CPANEL;
    }

    public boolean hasPlesk() {
        return this.controlPanel == ControlPanel.PLESK;
    }

    public boolean hasMYH() {
        return this.controlPanel == ControlPanel.MYH;
    }

    public boolean hasIspConfig() {
        return this.hasMYH() && this.hfsName.toLowerCase().contains(ISPCONFIG);
    }

    public boolean hasControlPanel() {
        return this.hasPaidControlPanel() || this.hasFreeControlPanel();
    }

    public boolean hasPaidControlPanel() {
        return this.hasCpanel() || this.hasPlesk();
    }

    public boolean hasFreeControlPanel() {
        return this.hasIspConfig();
    }

    /**
     * This method returns the actual control panel on the hfs image. This is used by hfs nydus and/or cnc workflow
     * This is needed because ISPconfig (which is a free control panel) is represented as MYH
     * @return
     */
    @JsonIgnore
    public String getImageControlPanel() {
        return this.hasPaidControlPanel()
            ? this.controlPanel.toString().toLowerCase()
            : this.hasFreeControlPanel() ? ISPCONFIG : null;
    }
}
