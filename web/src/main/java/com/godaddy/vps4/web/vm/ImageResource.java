package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerType.Platform;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.featureFlag.ConfigFeatureMask;
import com.godaddy.vps4.web.featureFlag.ImageDetailFeatureSetting;
import com.godaddy.vps4.web.featureFlag.ImageListFeatureSetting;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImageResource {

    private static final Logger logger = LoggerFactory.getLogger(ImageResource.class);

    private final ImageService imageService;

    @Inject
    public ImageResource(ImageService imageService){
        this.imageService = imageService;
    }

    @GET
    @Path("/vmImages")
    @ConfigFeatureMask(
        setting = ImageListFeatureSetting.class,
        disabled = false
    )
    public List<Image> getImages(@QueryParam("os") String os,
                                 @QueryParam("controlPanel") String controlPanel,
                                 @ApiParam(value = "HFS image name") @QueryParam("imageName") String hfsImageName,
                                 @QueryParam("platform") String platform) {
        return imageService.getImages(validateAndReturnEnumValue(OperatingSystem.class, os.toUpperCase()),
                                      validateAndReturnEnumValue(ControlPanel.class, controlPanel.toUpperCase()),
                                      hfsImageName,
                                      validateAndReturnEnumValue(Platform.class, platform.toUpperCase()));
    }

    @GET
    @Path("/vmImages/{name}")
    @ConfigFeatureMask(
        setting = ImageDetailFeatureSetting.class,
        disabled = false
    )
    public Image getImage(@ApiParam(value = "Hfs image name") @PathParam("name") String name) {
        logger.info("getting images with name {}", name);
        Image image = imageService.getImageByHfsName(name);
        if (image == null) {
            throw new NotFoundException("Unknown image: " + name);
        }
        return image;
    }
}
