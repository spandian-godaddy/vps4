package com.godaddy.vps4.web.featureFlag;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImageDetailFeatureSettingTest {
    private Injector injector;

    private GDUser gdUser = mock(GDUser.class);
    private Config config = mock(Config.class);
    private Image image = new Image();
    private ImageDetailFeatureSetting featureSetting;

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FeatureSetting.class).to(ImageDetailFeatureSetting.class);
                bind(Config.class).toInstance(config);
                bind(GDUser.class).toInstance(gdUser);
            }
        });

        when(config.get(eq(ImageDetailFeatureSetting.FEATUREDFLAG_IMAGES_ENV_DISABLED), any(String.class)))
            .thenReturn("foobar,image1");
        when(config.get(eq(ImageDetailFeatureSetting.FEATUREDFLAG_IMAGES_CUSTOMER_DISABLED), any(String.class)))
                .thenReturn("helloworld,image2");
        when(gdUser.isPayingCustomer()).thenReturn(false);

        featureSetting = injector.getInstance(ImageDetailFeatureSetting.class);
    }

    @Test
    public void imageThatIsNotDisabledIsOk() {
        image.hfsName = "image3";
        assertEquals(image, featureSetting.handle(image));
    }

    @Test
    public void imageDisabledInEnvThrows404() {
        image.hfsName = "image1";

        try {
            featureSetting.handle(image);
        }
        catch (NotFoundException ex) {
            assertEquals("Unknown image", ex.getMessage());
            return;
        }

        fail("TEST SHOULDN'T GET HERE");
    }

    @Test
    public void imageDisabledForCustomerThrows404() {
        image.hfsName = "image2";
        when(gdUser.isPayingCustomer()).thenReturn(true);

        try {
            featureSetting.handle(image);
        }
        catch (NotFoundException ex) {
            assertEquals("Unknown image", ex.getMessage());
            return;
        }

        fail("TEST SHOULDN'T GET HERE");
    }

    @Test
    public void nullImageThrows404() {
        try {
            featureSetting.handle(null);
        }
        catch (NotFoundException ex) {
            assertEquals("Unknown image", ex.getMessage());
        }
    }
}