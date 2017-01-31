package com.godaddy.vps4.vm;

import java.util.Set;

public interface ImageService {

    Set<String> obtainCompatibleImages();

    void addCompatibleImage(String name, Long controlPanelId);

    void removeCompatibleImage(String name);

    int getImageId(String name);

    Image getImage(String name);
    
    Set<Image> getImages(String os, String controlPanel, String hfsName);
}
