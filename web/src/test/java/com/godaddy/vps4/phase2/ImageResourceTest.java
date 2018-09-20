package com.godaddy.vps4.phase2;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.vm.ImageResource;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ImageResourceTest {
    @Inject ImageService imageService;
    @Inject DataSource dataSource;

    private ImageResource resource;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new VmModule()
    );

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        resource = new ImageResource(imageService);
    }

    @Test
    public void testGetImagesReturnsImages(){
        assertFalse(resource.getImages("LINUX", "MYH", 20).isEmpty());
    }

    @Test
    public void testGetImagesReturnsImagesIfTierNotPassed(){
        // Tier is an optional primitive type and will result in 0 if the api is called without that parameter.
        assertFalse(resource.getImages("LINUX", "MYH", 0).isEmpty());
    }

    @Test
    public void testGetImagesIsNotCaseSensitive(){
        // Tier is an optional primitive type and will result in 0 if the api is called without that parameter.
        assertFalse(resource.getImages("linux", "myh", 0).isEmpty());
    }

    @Test
    public void testGetImagesReturnsEmptyWhenNoImagesFound(){
        assertTrue(resource.getImages("fake", "MYH", 0).isEmpty());
    }
}
