package com.godaddy.vps4.move;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.move.jdbc.JdbcVmMoveImageMapService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;

public class VmMoveImageMapServiceTest {

    private VmMoveImageMapService vmMoveImageMapService;
    private ImageService imageService;
    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        vmMoveImageMapService = new JdbcVmMoveImageMapService(dataSource);
        imageService = new JdbcImageService(dataSource);
    }

    @Test
    public void testGetMap() {
        Image osImage = imageService.getImageByHfsName("hfs-centos-7");
        Image ohImage = imageService.getImageByHfsName("hfs-centos7");
        VmMoveImageMap map = vmMoveImageMapService.getVmMoveImageMap(osImage.imageId, ohImage.serverType.platform);
        assertEquals(ohImage.imageId, map.toImageId);
    }
}
