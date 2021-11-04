package com.godaddy.vps4.util;

import com.godaddy.vps4.notifications.NotificationType;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class NotificationListSearchFilters {
    private List<String> imageIds = new ArrayList<>();
    private List<String> resellerIds = new ArrayList<>();
    private List<NotificationType> types = new ArrayList<>();
    private List<String> hypervisor = new ArrayList<>();
    private List<String> tiers = new ArrayList<>();
    private List<String> platformIds = new ArrayList<>();
    private List<String> vmIds = new ArrayList<>();
    private Instant validOn;
    private Instant validUntil;
    private boolean showActive;
    private boolean adminView;
    private Boolean isManaged;

    public void byType(NotificationType... typeList) {
        types = Arrays.asList(typeList);
    }
    public void byType(Collection<NotificationType> typeList) {
        this.types = new ArrayList<>(typeList);
    }

    public void byImageId(Collection<String> images) {
        this.imageIds = new ArrayList<>(images);
    }
    public void byImageId(String... images) {
        imageIds = Arrays.asList(images);
    }

    public void byResellerId(Collection<String> resellers) {
        this.resellerIds = new ArrayList<>(resellers);
    }
    public void byResellerId(String... resellers) {
        resellerIds = Arrays.asList(resellers);
    }

    public void byHypervisors(Collection<String> hypervisors) {
        this.hypervisor = new ArrayList<>(hypervisors);
    }
    public void byHypervisors(String... hypervisors) {
        hypervisor = Arrays.asList(hypervisors);
    }

    public void byTier(Collection<String> tier) {
        this.tiers = new ArrayList<>(tier);
    }
    public void byTier(String... tier) {
        tiers = Arrays.asList(tier);
    }

    public void byPlatform(Collection<String> platforms) {
        this.platformIds = new ArrayList<>(platforms);
    }
    public void byPlatform(String... platform) {
        platformIds = Arrays.asList(platform);
    }

    public void byVmId(Collection<String> vmIds) {
        this.vmIds = new ArrayList<>(vmIds);
    }
    public void byVmId(String... vmId) {
        vmIds = Arrays.asList(vmId);
    }

    public void byDateRange(Instant start, Instant end) {
        this.validOn = start;
        this.validUntil = end;
    }

    public void byActive(boolean showActive) {this.showActive = showActive;
    }

    public void byIsManaged(Boolean managed) {
        this.isManaged = managed;
    }

    public void byAdminView(boolean adminView) {this.adminView = adminView;
    }

    public List<NotificationType> getTypeList() {
        return types;
    }
    public List<String> getResellers() {
        return resellerIds;
    }
    public List<String> getHypervisor() {
        return hypervisor;
    }
    public List<String> getImageIds() {
        return imageIds;
    }
    public List<String> getTiers() {
        return tiers;
    }
    public List<String> getPlatformIds() {
        return platformIds;
    }
    public List<String> getVmIds() {
        return vmIds;
    }
    public Boolean getIsManaged() {
        return isManaged;
    }
    public List<String> getIsManagedAsList() {
        if(isManaged == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(Boolean.toString(isManaged));
    }
    public Instant getValidOn() {
        return validOn;
    }
    public Instant getValidUntil() {
        return validUntil;
    }
    public boolean getShowActive() {
        return showActive;
    }
    public boolean getAdminView() {
        return adminView;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
