package com.godaddy.vps4.phase2.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcImageServiceTest {

    static private Injector injectorForDS;
    private Injector injector;
    static private DataSource dataSource;
    private String imageName = "hfs-ubuntu-1604";

    @BeforeClass
    public static void setUpInternalInjector() {
        injectorForDS = Guice.createInjector(new DatabaseModule());
        dataSource = injectorForDS.getInstance(DataSource.class);
    }

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DataSource.class).toInstance(dataSource);
                bind(ImageService.class).to(JdbcImageService.class);
            }
        });
    }

    @Test
    public void getImageForRegularImageOk() {
        assertNotNull(injector.getInstance(ImageService.class).getImageByHfsName("HfS-UbUnTu-1604"));
    }

    @Test
    public void getImagesIncludesImageNotDisabled() {
        List<Image> images = injector.getInstance(ImageService.class).getImages("linux", "myh", null, "openstack");
        assertTrue(images.stream().map(i -> i.hfsName).anyMatch(name -> name.equals(imageName)));
    }

    @Test
    public void insertImageTest() {
        ImageService service = injector.getInstance(ImageService.class);
        String imageName = UUID.randomUUID().toString();
        long imageId = service.insertImage(0, 1, imageName, 3, imageName, true);
        assertNotNull(imageId);
        service.removeCompatibleImage(imageName);
    }

    @Test
    public void getImageNotExistsReturnsZero() {
        ImageService service = injector.getInstance(ImageService.class);
        int imageId = service.getImageIdByHfsName("testImageNotExists");
        assertEquals(0, imageId);
    }
}