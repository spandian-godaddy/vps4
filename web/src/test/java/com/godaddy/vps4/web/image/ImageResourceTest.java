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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    private int tier = 0;
    private Image[] images = {mock(Image.class), mock(Image.class)};
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

        when(imageService.getImages(anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(new HashSet<>(Arrays.asList(images)));
        when(imageService.getImage(anyString())).thenReturn(image);

        resource = injector.getInstance(ImageResource.class);
    }

    @Test
    public void getImagesCallsImageServiceToGetListOfImages(){
        resource.getImages(os, controlPanel, null, tier);
        verify(imageService, times(1)).getImages(os, controlPanel, null, tier);
    }

    @Test
    public void getImagesReturnsSetOfImagesFound(){
        Set<Image> retImages = resource.getImages(os, controlPanel, null, tier);
        assertEquals(retImages, new HashSet<>(Arrays.asList(images)));
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
