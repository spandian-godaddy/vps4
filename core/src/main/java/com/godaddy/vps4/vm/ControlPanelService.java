package com.godaddy.vps4.vm;

import java.util.List;

public interface ControlPanelService {
    void addControlPanelType(String controlPanel);

    void deleteControlPanelType(String controlPanel);

    List<String> listControlPanelTypes();

    Long getControlPanelId(String controlPanel);
}
