package com.godaddy.vps4.phase2.image;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
        assertNotNull(injector.getInstance(ImageService.class).getImage("hfs-ubuntu-1604"));
    }

    @Test
    public void getImagesIncludesImageNotDisabled() {
        List<Image> images = injector.getInstance(ImageService.class).getImages("linux", "myh", null, 0);
        assertTrue(images.stream().map(i -> i.hfsName).anyMatch(name -> name.equals(imageName)));
    }
}