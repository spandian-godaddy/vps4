package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.web.featureFlag.ConfigFeatureMask;
import com.godaddy.vps4.web.featureFlag.ImageDetailFeatureSetting;
import com.godaddy.vps4.web.featureFlag.ImageListFeatureSetting;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

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
                                 @ApiParam(value = "HFS Image name") @QueryParam("imageName") String hfsImageName,
                                 @QueryParam("tier") int tier) {
        logger.info("getting images with os = {} and controlPanel = {} available for tier {}", os, controlPanel, tier);
        return imageService.getImages(os, controlPanel, hfsImageName, tier);
    }

    @GET
    @Path("/vmImages/{name}")
    @ConfigFeatureMask(
        setting = ImageDetailFeatureSetting.class,
        disabled = false
    )
    public Image getImage(@ApiParam(value = "Hfs image name") @PathParam("name") String name) {
        logger.info("getting images with name {}", name);
        Image image = imageService.getImage(name);
        if (image == null) {
            throw new NotFoundException("Unknown image: " + name);
        }
        return image;
    }
}
