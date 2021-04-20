package com.godaddy.vps4.vm;

import java.util.List;

public interface ImageService {

    List<String> obtainCompatibleImages();

    void addCompatibleImage(String name, Long controlPanelId);

    void removeCompatibleImage(String name);

    int getImageIdByHfsName(String hfsName);

    Image getImageByHfsName(String hfsName);

    long insertImage(int controlPanelId, int osTypeId, String name, int serverTypeId, String hfsName,
                     boolean importedImage);

    List<Image> getImages(String os, String controlPanel, String hfsName, String platform);
}
