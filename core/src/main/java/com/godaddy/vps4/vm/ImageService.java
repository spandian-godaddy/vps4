package com.godaddy.vps4.vm;

import java.util.List;

import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ServerType.Platform;

public interface ImageService {

    void removeCompatibleImage(String name);

    int getImageIdByHfsName(String hfsName);

    Image getImageByHfsName(String hfsName);

    long insertImage(int controlPanelId, int osTypeId, String name, int serverTypeId, String hfsName,
                     boolean importedImage);

    List<Image> getImages(OperatingSystem os, ControlPanel controlPanel, String hfsName, Platform platform);

    Image getImage(long id);
}
