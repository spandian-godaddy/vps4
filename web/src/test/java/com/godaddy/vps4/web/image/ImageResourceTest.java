package com.godaddy.vps4.web.image;

import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.web.vm.ImageResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ImageResourceTest {
    private ImageService imageService = mock(ImageService.class);
    private ImageResource resource;

    private Injector injector;
    private String os = "linux";
    private String controlPanel = "myh";
    private int tier = 10;
    private List<Image> images;
    private List<Image> verifyImages;
    private Image image = mock(Image.class);
    private String imageName = "foobar";

    @Before
    public void setupTest() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ImageService.class).toInstance(imageService);
            }
        });

        images = testImages("Ubuntu 16.04", "CentOS 7");
        verifyImages = testImages("Ubuntu 16.04", "CentOS 7");

        when(imageService.getImages(anyString(), anyString(), anyString(), anyInt())).thenReturn(images);
        when(imageService.getImage(anyString())).thenReturn(image);

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
        resource.getImages(os, controlPanel, null, tier);
        verify(imageService, times(1)).getImages(os, controlPanel, null, tier);
    }

    @Test
    public void getImagesReturnsListOfImagesFound(){
        List<Image> retImages = resource.getImages(os, controlPanel, null, tier);
        for (int i = 0; i < retImages.size(); i++) {
            assertEquals(verifyImages.get(i).imageName, retImages.get(i).imageName);
        }
    }

    @Test
    public void getImageCallsImageServiceToGetImage(){
        resource.getImage(imageName);
        verify(imageService, times(1)).getImage(imageName);
    }

    @Test
    public void getImageReturnsFoundImage(){
        assertEquals(image, resource.getImage(imageName));
    }

    @Test
    public void getImageThowsExceptionWhenImageNotFound(){
        when(imageService.getImage(anyString())).thenReturn(null);
        try {
            resource.getImage(imageName);
        }
        catch (NotFoundException ex) {
            assertEquals("Unknown image: " + imageName, ex.getMessage());
        }
    }
}
