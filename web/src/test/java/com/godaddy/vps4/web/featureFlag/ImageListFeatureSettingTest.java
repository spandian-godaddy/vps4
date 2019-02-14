package com.godaddy.vps4.web.featureFlag;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImageListFeatureSettingTest {
    private Injector injector;

    private GDUser gdUser = mock(GDUser.class);
    private Config config = mock(Config.class);
    private ImageListFeatureSetting featureSetting;
    private String[] imageNames = {"image1", "image2", "image3", "image4", "image5", "image6"};
    private Set<Image> images;

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FeatureSetting.class).to(ImageListFeatureSetting.class);
                bind(Config.class).toInstance(config);
                bind(GDUser.class).toInstance(gdUser);
            }
        });

        when(config.get(eq(ImageDetailFeatureSetting.FEATUREDFLAG_IMAGES_ENV_DISABLED), any(String.class)))
                .thenReturn("foobar,image1,image4");
        when(config.get(eq(ImageDetailFeatureSetting.FEATUREDFLAG_IMAGES_CUSTOMER_DISABLED), any(String.class)))
                .thenReturn("helloworld,image2,image3");
        when(gdUser.isPayingCustomer()).thenReturn(true);

        images = Arrays.stream(imageNames).map(n -> {
            Image i = new Image();
            i.hfsName = n;
            return i;
        }).collect(Collectors.toSet());

        featureSetting = injector.getInstance(ImageListFeatureSetting.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void imagesNotDisabledAreNotFilteredOut() {
        Set<Image> retImages = (Set<Image>) featureSetting.handle(images);
        assertTrue(retImages.stream().map(i -> i.hfsName)
                .allMatch(hfsName -> hfsName.equals("image5") || hfsName.equals("image6")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void imagesDisabledInEnvIsFilteredOut() {
       Set<Image> retImages = (Set<Image>) featureSetting.handle(images);
       assertTrue(retImages.stream().map(i -> i.hfsName)
           .noneMatch(hfsName -> hfsName.equals("image1") || hfsName.equals("image4")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void imagesDisabledForCustomerIsFilteredOut() {
        Set<Image> retImages = (Set<Image>) featureSetting.handle(images);
        assertTrue(retImages.stream().map(i -> i.hfsName)
            .noneMatch(hfsName -> hfsName.equals("image2") || hfsName.equals("image3")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void imagesNotDisabledForNonCustomerNotFilteredOut() {
        when(gdUser.isPayingCustomer()).thenReturn(false);
        Set<Image> retImages = (Set<Image>) featureSetting.handle(images);
        assertTrue(retImages.stream().map(i -> i.hfsName)
                .allMatch(hfsName -> !(hfsName.equals("image1") || hfsName.equals("image4"))));
    }
}