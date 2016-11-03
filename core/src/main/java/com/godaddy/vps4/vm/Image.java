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
        NONE(0), CPANEL(1), PLESK(2);

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
    public String imageName;
    public ControlPanel controlPanel;
    public OperatingSystem operatingSystem;
}
