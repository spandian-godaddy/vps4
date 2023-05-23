package com.godaddy.vps4.web.controlPanel.cpanel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cpanel.CPanelAccount;
import com.godaddy.vps4.cpanel.CPanelAccountCacheStatus;
import com.godaddy.vps4.cpanel.CPanelDomain;
import com.godaddy.vps4.cpanel.CPanelDomainType;
import com.godaddy.vps4.cpanel.CPanelSession;
import com.godaddy.vps4.cpanel.InstallatronApplication;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.cpanel.UpdateNginxRequest;
import com.godaddy.vps4.cpanel.CpanelInvalidUserException;
import com.godaddy.vps4.orchestration.cpanel.Vps4AddAddOnDomain;
import com.godaddy.vps4.orchestration.cpanel.Vps4InstallCPanelPackage;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import gdg.hfs.orchestration.CommandService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.controlPanel.ControlPanelRequestValidation;
import com.godaddy.vps4.web.vm.VmResource;

import io.swagger.annotations.Api;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;

@Vps4Api
@Api(tags = { "vms" })
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CPanelResource {
    public static final String INSTALLATRON_PATH = "3rdparty/installatron/index.cgi?#/";

    private static final Logger logger = LoggerFactory.getLogger(CPanelResource.class);

    final VmResource vmResource;
    final Vps4CpanelService cpanelService;
    final ActionService actionService;
    final CommandService commandService;
    final GDUser user;
    final Config config;

    @Inject
    public CPanelResource(VmResource vmResource, Vps4CpanelService cpanelService, ActionService actionService,
                          CommandService commandService, GDUser user, Config config) {
        this.vmResource = vmResource;
        this.cpanelService = cpanelService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.config = config;
    }

    @GET
    @Path("{vmId}/cpanel/whmSession")
    public CPanelSession getWHMSession(@PathParam("vmId") UUID vmId) {
        logger.info("get WHM session for vmId {}", vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.createSession(vm.hfsVmId, "root", CpanelServiceType.whostmgrd);
        } catch (Exception e) {
            logger.warn("Could not provide WHM cpanel session for vmId {} , Exception: {} ", vmId, e);
        }

        return null;
    }

    @GET
    @Path("{vmId}/cpanel/cpanelSession")
    public CPanelSession getCPanelSession(@PathParam("vmId") UUID vmId, @QueryParam("username") String username,
                                          @QueryParam("installatronAppId") String appId,
                                          @QueryParam("installatronCommand") InstallatronCommand command) {
        logger.info("get cPanel session for vmId {}", vmId);
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            CPanelSession session = cpanelService.createSession(vm.hfsVmId, username, CpanelServiceType.cpaneld);

            return command != null ? appendGoToURI(session, command, appId) : session;
        } catch (Exception e) {
            logger.warn("Could not provide cpanel session for vmId {} , Exception: {} ", vmId, e);
        }

        return null;
    }

    public enum InstallatronCommand {
        LIST_INSTALLED_APPS,
        MANAGE_APP,
        BROWSE_APPS
    }

    private CPanelSession appendGoToURI(CPanelSession session, InstallatronCommand command, String appId) throws UnsupportedEncodingException {
        String path = INSTALLATRON_PATH;

        switch (command) {
            case LIST_INSTALLED_APPS:
                path += "installs?";
                break;
            case MANAGE_APP:
                path += "installs/" + appId ;
                break;
            case BROWSE_APPS:
                path += "apps?";
                break;
            default:
                break;
        }

        String encodedURI = "&goto_uri=" + URLEncoder.encode(path, "UTF-8");
        session.data.url += encodedURI;
        return session;
    }

    @GET
    @Path("{vmId}/cpanel/{username}/installatronApps")
    public List<InstallatronApplication> getInstalledInstallatronApps(@PathParam("vmId") UUID vmId, @PathParam("username") String username) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listInstalledInstallatronApplications(vm.hfsVmId, username);
        } catch (Exception e) {
            logger.warn("Could not get list of installed Installatron applications for vmId {}", vmId);
            throw new Vps4Exception("LIST_INSTALLATRON_APPS_FAILED", e.getMessage(), e);
        }
    }

    @GET
    @Path("{vmId}/cpanel/installatronApps")
    public List<InstallatronApplication> getAllInstalledInstallatronApps(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listAllInstalledInstallatronApplications(vm.hfsVmId);
        } catch (Exception e) {
            logger.warn("Could not get list of all installed Installatron applications for vmId {}", vmId);
            throw new Vps4Exception("LIST_ALL_INSTALLATRON_APPS_FAILED", e.getMessage(), e);
        }
    }
    @GET
    @Path("/{vmId}/cpanel/accounts")
    public List<CPanelAccount> listCpanelAccounts(@PathParam("vmId") UUID vmId) {
        logger.info("GET listCpanelAccounts for VM: {}", vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listCpanelAccounts(vm.hfsVmId);
        } catch (Exception e) {
            logger.warn("Could not provide cpanel accounts for vmId {} , Exception: {} ", vmId, e);
        }
        return null;
    }

    @GET
    @Path("/{vmId}/cpanel/{username}/addOnDomains")
    public List<String> listAddOnDomains(@PathParam("vmId") UUID vmId, @PathParam("username") String username) {
        logger.info("GET listAddOnDomains for user {} on VM: {}", username, vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listAddOnDomains(vm.hfsVmId, username);
        } catch (CpanelInvalidUserException e) {
            throw new Vps4Exception("INVALID_CPANEL_USER", e.getMessage());
        }
        catch (Exception e) {
            logger.warn("Could not provide cpanel add on domains for user {} on vmId {} , Exception: {} ", username, vmId, e);

        }
        return null;
    }

    @POST
    @Path("/{vmId}/cpanel/{username}/addOnDomain")
    public VmAction createAddOnDomain(@PathParam("vmId") UUID vmId, @PathParam("username") String username, CreateAddOnDomainRequest request) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        validateNoConflictingActions(vmId, actionService, ActionType.ADD_ADDON_DOMAIN);

        JSONObject addOnDomainRequest = new JSONObject();
        addOnDomainRequest.put("username", username);
        addOnDomainRequest.put("newDomain", request.newDomain);

        long actionId = actionService.createAction(vmId, ActionType.ADD_ADDON_DOMAIN,
                addOnDomainRequest.toJSONString(), user.getUsername());

        Vps4AddAddOnDomain.Request addAddOnDomainReq = new Vps4AddAddOnDomain.Request();
        addAddOnDomainReq.hfsVmId = vm.hfsVmId;
        addAddOnDomainReq.vmId = vmId;
        addAddOnDomainReq.actionId = actionId;
        addAddOnDomainReq.username = username;
        addAddOnDomainReq.newDomain = request.newDomain;

        Commands.execute(commandService, actionService, "Vps4AddAddonDomain", addAddOnDomainReq);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }

    @GET
    @Path("/{vmId}/cpanel/domains")
    public List<CPanelDomain> listDomains(@PathParam("vmId") UUID vmId, @QueryParam("domainType")@DefaultValue("ALL") CPanelDomainType domainType) {
        logger.info("GET domains with {} type(s) on VM: {}", domainType, vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listDomains(vm.hfsVmId, domainType);
        } catch (Exception e) {
            throw new Vps4Exception("CPANEL_LIST_DOMAINS_FAILED", e.getMessage(), e);
        }
    }

    public static class PasswordStrengthRequest {
        public String password;
    }

    @POST
    @Path("/{vmId}/cpanel/passwordStrength")
    public Long calculatePasswordStrength(
        @PathParam("vmId") UUID vmId, PasswordStrengthRequest passwordStrengthRequest) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.calculatePasswordStrength(vm.hfsVmId, passwordStrengthRequest.password);
        } catch (Exception e) {
            logger.warn("Could not calculate cpanel password strength for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("PASSWORD_STRENGTH_CALCULATION_FAILED", e.getMessage(), e);
        }
    }

    public static class CreateAccountRequest {
        public String domainName;
        public String username;
        public String password;
        public String plan;
        public String contactEmail;
    }

    @POST
    @Path("/{vmId}/cpanel/createAccount")
    public void createAccount(
            @PathParam("vmId") UUID vmId, CreateAccountRequest createAccountRequest) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
            cpanelService.createAccount(
                vm.hfsVmId, createAccountRequest.domainName, createAccountRequest.username,
                createAccountRequest.password, createAccountRequest.plan, createAccountRequest.contactEmail);
        } catch (Exception e) {
            logger.warn("Could not create cpanel account for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("CREATE_CPANEL_ACCOUNT_FAILED", e.getMessage(), e);
        }
    }

    @GET
    @Path("/{vmId}/cpanel/packages")
    public List<String> listPackages(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
            return cpanelService.listPackages(vm.hfsVmId);
        } catch (Exception e) {
            logger.warn("Could not list CPanel packages for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("LIST_PACKAGES_FAILED", e.getMessage(), e);
        }
    }

    @GET
    @Path("/{vmId}/cpanel/nginxManager")
    public CpanelNginxStatusResponse getNginxManagerStatus(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        NginxStatus nginxStatus = NginxStatus.NOT_INSTALLABLE;
        List<CPanelAccountCacheStatus> accountCachingStatus = new ArrayList<>();
        try {
            List<String> rpmPackages = cpanelService.listInstalledRpmPackages(vm.hfsVmId);
            if (rpmPackages != null && rpmPackages.contains("ea-nginx")){
                nginxStatus = NginxStatus.INSTALLED;
                accountCachingStatus = cpanelService.getNginxCacheConfig(vm.hfsVmId);
            }
            else{
                String version = cpanelService.getVersion(vm.hfsVmId);
                if(version != null && isVersionCompatible(version)) {
                    nginxStatus = NginxStatus.INSTALLABLE;
                }
            }
            return new CpanelNginxStatusResponse(nginxStatus, accountCachingStatus);
        } catch (Exception e) {
            logger.warn("Could not retrieve CPanel nginx manager status for vmId {} ", vmId, e);
            throw new Vps4Exception("GET_NGINX_STATUS_FAILED", e.getMessage(), e);
        }
    }

    public boolean isVersionCompatible(String actualVersion) {
        // minimum compatible version is 11.102.0.0
        String[] actualVerArr = actualVersion.split("\\.");
        if(actualVerArr.length != 4) {
            throw new Vps4Exception("INCORRECT_VERSION_FORMAT", "CPanel version format is incorrect.");
        }
        return Integer.parseInt(actualVerArr[0]) >= 11 ? Integer.parseInt(actualVerArr[1]) >= 102 : false;
    }

    public static class CpanelNginxStatusResponse {
        public NginxStatus installStatus;
        public List<CPanelAccountCacheStatus> accountCachingStatus;

        public CpanelNginxStatusResponse(NginxStatus installStatus, List<CPanelAccountCacheStatus> accountCachingStatus) {
            this.installStatus = installStatus;
            this.accountCachingStatus = accountCachingStatus;
        }
    }

    public enum NginxStatus{
        INSTALLED,
        NOT_INSTALLABLE,
        INSTALLABLE
    }

    @GET
    @Path("/{vmId}/cpanel/version")
    public CpanelVersionResponse getVersion(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
            String version = cpanelService.getVersion(vm.hfsVmId);
            CpanelVersionResponse response = new CpanelVersionResponse(version);
            return response;
        } catch (Exception e) {
            logger.warn("Could not retrieve CPanel/WHM version for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("GET_VERSION_FAILED", e.getMessage(), e);
        }
    }

    public static class CpanelVersionResponse {
        public String version;

        public CpanelVersionResponse(String version) {
            this.version = version;
        }
    }

    @POST
    @Path("/{vmId}/cpanel/rpmPackages")
    public VmAction installRpmPackage(@PathParam("vmId") UUID vmId, InstallPackageRequest request) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        validateNoConflictingActions(vmId, actionService, ActionType.INSTALL_CPANEL_PACKAGE);
        List<String> approvedPackageList = Arrays.asList(config.get("cpanel.rpm.packages", "").split(","));
        if(!approvedPackageList.contains(request.packageName)) {
            throw new Vps4Exception("PACKAGE_NOT_ALLOWED", "Package isn't in the list of approved CPanel rpm packages");
        }

        JSONObject packageJsonRequest = new JSONObject();
        packageJsonRequest.put("packageName", request.packageName);

        long actionId = actionService.createAction(vmId, ActionType.INSTALL_CPANEL_PACKAGE,
                packageJsonRequest.toJSONString(), user.getUsername());

        Vps4InstallCPanelPackage.Request installPackageReq = new Vps4InstallCPanelPackage.Request();
        installPackageReq.packageName = request.packageName;
        installPackageReq.hfsVmId = vm.hfsVmId;
        installPackageReq.vmId = vmId;
        installPackageReq.actionId = actionId;

        Commands.execute(commandService, actionService, "Vps4InstallCPanelPackage", installPackageReq);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }

    @POST
    @Path("/{vmId}/cpanel/updateNginx")
    public void updateNginx(@PathParam("vmId") UUID vmId, UpdateNginxRequest updateNginxRequest) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
            cpanelService.updateNginx(vm.hfsVmId, updateNginxRequest.enabled, updateNginxRequest.usernames);
        } catch (Exception e) {
            logger.warn("Could not enabled NGINX for vmId {}, username {}, Exception: {} ", vmId, updateNginxRequest.usernames, e);
            throw new Vps4Exception("UPDATE_NGINX_FAILED", e.getMessage(), e);
        }
    }

    @DELETE
    @Path("/{vmId}/cpanel/clearNginxCache")
    public void clearNginxCache(@PathParam("vmId") UUID vmId, @QueryParam("usernames") List<String> usernames) {
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
            cpanelService.clearNginxCache(vm.hfsVmId, usernames);
        } catch (Exception e) {
            logger.warn("Could not clear NGiNX cache for vmId {}, users {}, Exception: {} ", vmId, usernames, e);
            throw new Vps4Exception("CLEAR_NGINX_CACHE_FAILED", e.getMessage(), e);
        }
    }

    private VirtualMachine resolveVirtualMachine(UUID vmId) {
        return ControlPanelRequestValidation.getValidVirtualMachine(vmResource,
                ControlPanel.CPANEL, vmId);
    }

    public static class InstallPackageRequest {
        public String packageName;
    }

    public static class CreateAddOnDomainRequest {
        public String newDomain;
    }
}
