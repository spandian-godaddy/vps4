package com.godaddy.vps4.vm;

import java.util.List;

public interface ImageService {

    List<String> obtainCompatibleImages();

    void addCompatibleImage(String name, Long controlPanelId);

    void removeCompatibleImage(String name);

    int getImageId(String name);

    Image getImage(String name);
    
    List<Image> getImages(String os, String controlPanel, String hfsName, int tier);
}
