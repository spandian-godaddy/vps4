package com.godaddy.vps4.web.image;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerType.Platform;
import com.godaddy.vps4.web.vm.ImageResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ImageResourceTest {
    private final ImageService imageService = mock(ImageService.class);
    private ImageResource resource;

    private final OperatingSystem os = Image.OperatingSystem.LINUX;
    private final ControlPanel controlPanel = Image.ControlPanel.MYH;
    private final Platform platform = ServerType.Platform.OPENSTACK;
    private List<Image> verifyImages;
    private final Image image = mock(Image.class);
    private final String imageName = "foobar";

    @Before
    public void setupTest() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ImageService.class).toInstance(imageService);
            }
        });

        List<Image> images = testImages("Ubuntu 16.04", "CentOS 7");
        verifyImages = testImages("Ubuntu 16.04", "CentOS 7");

        when(imageService.getImages(any(), any(), anyString(), any())).thenReturn(images);
        when(imageService.getImageByHfsName(anyString())).thenReturn(image);

        resource = injector.getInstance(ImageResource.class);
    }

    private List<Image> testImages(String... names) {
        List<Image> images = new ArrayList<>();
        for (String name : names) {
            Image image = new Image();
            image.imageName = name;
            images.add(image);
        }
        return images;
    }

    @Test
    public void getImagesCallsImageServiceToGetListOfImages(){
        resource.getImages(os.name(), controlPanel.name(), null, platform.name());
        verify(imageService, times(1)).getImages(os, controlPanel, null, platform);
    }

    @Test
    public void getImagesReturnsListOfImagesFound(){
        List<Image> retImages = resource.getImages(os.name(), controlPanel.name(), null, platform.name());
        for (int i = 0; i < retImages.size(); i++) {
            assertEquals(verifyImages.get(i).imageName, retImages.get(i).imageName);
        }
    }

    @Test
    public void getImageCallsImageServiceToGetImage(){
        resource.getImage(imageName);
        verify(imageService, times(1)).getImageByHfsName(imageName);
    }

    @Test
    public void getImageReturnsFoundImage(){
        assertEquals(image, resource.getImage(imageName));
    }

    @Test
    public void getImageThowsExceptionWhenImageNotFound(){
        when(imageService.getImageByHfsName(anyString())).thenReturn(null);
        try {
            resource.getImage(imageName);
        }
        catch (NotFoundException ex) {
            assertEquals("Unknown image: " + imageName, ex.getMessage());
        }
    }
}
