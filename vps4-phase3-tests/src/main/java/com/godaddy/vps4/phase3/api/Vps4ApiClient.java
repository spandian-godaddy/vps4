package com.godaddy.vps4.phase3.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class Vps4ApiClient {

    private String baseUrl;
    private String authHeader;
    private static final Logger logger = LoggerFactory.getLogger(Vps4ApiClient.class);

    public Vps4ApiClient(String baseUrl, String authHeader){
      this.baseUrl = baseUrl;
      this.authHeader = "sso-jwt " + authHeader;
    }

    protected class Result {
        public int statusCode;
        public StringBuffer result;

        public Result(int statusCode, StringBuffer result){
            this.statusCode = statusCode;
            this.result = result;
        }
    }

    public class Vps4JsonResponse <T>{
        public int statusCode;
        public T jsonResponse;
        public Vps4JsonResponse(int statusCode, T jsonResponse){
            this.statusCode = statusCode;
            this.jsonResponse = jsonResponse;
        }
    }

    public Vps4JsonResponse<JSONObject> sendGetObject(String urlAppendix){
        Result result = sendGet(urlAppendix);
        JSONObject jsonResponse = convertToJSONObject(result.result.toString());
        return new Vps4JsonResponse<JSONObject>(result.statusCode, jsonResponse);
    }

    public Vps4JsonResponse<JSONArray> sendGetList(String urlAppendix){
        Result result = sendGet(urlAppendix);

        JSONParser parser = new JSONParser();
        try{
            return new Vps4JsonResponse<JSONArray>(result.statusCode,  (JSONArray) parser.parse(result.result.toString()));
        }catch(ParseException e){
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    public Vps4JsonResponse<JSONObject> sendPost(String urlAppendix) {
        return this.sendPost(urlAppendix, null);
    }

    protected HttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }

    public Vps4JsonResponse<JSONObject> sendPost(String urlAppendix, JSONObject body){
        String url = baseUrl + urlAppendix;

        HttpClient client = getHttpClient();
        HttpPost post = new HttpPost(url);

        try{
            if(body != null){
                StringEntity params =new StringEntity(body.toJSONString());
                post.setEntity(params);
            }
        }catch(IOException e){
            throw new RuntimeException(e.getMessage(), e.getCause());
        }

        Result result = callApi(client, post);
        JSONObject jsonResponse = convertToJSONObject(result.result.toString());
        return new Vps4JsonResponse<JSONObject>(result.statusCode, jsonResponse);
    }

    public String getVmPrimaryIp(UUID vmId){
        Vps4JsonResponse<JSONObject> getVmResponse = sendGetObject("api/vms/" + vmId);
        assertStatusCode200(getVmResponse);
        JSONObject vm = getVmResponse.jsonResponse;
        JSONObject primaryIp = (JSONObject) vm.get("primaryIpAddress");
        return primaryIp.get("ipAddress").toString();
    }

    public JSONObject getImage(String imageName) {
        Vps4JsonResponse<JSONObject> getImageResponse = sendGetObject("api/vmImages/" + imageName);
        assertStatusCode200(getImageResponse);
        return getImageResponse.jsonResponse;
    }

    public long setHostname(UUID vmId, String hostname) {
        JSONObject body = new JSONObject();
        body.put("hostname", hostname);
        Vps4JsonResponse<JSONObject> setHostnameResponse = sendPost("api/vms/" + vmId.toString() + "/setHostname", body);
        assertStatusCode200(setHostnameResponse);
        JSONObject setHostnameJsonResult = setHostnameResponse.jsonResponse;
        return (long) setHostnameJsonResult.get("id");
    }

    public long setPassword(UUID vmId, String username, String password) {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        Vps4JsonResponse<JSONObject> setPwdResponse = sendPost("api/vms/" + vmId.toString() + "/setPassword", body);
        assertStatusCode200(setPwdResponse);
        JSONObject setPwdJsonResult = setPwdResponse.jsonResponse;
        return (long) setPwdJsonResult.get("id");
    }

    public JSONObject addSupportUser(UUID vmId) {
        Vps4JsonResponse<JSONObject> addSupportUserResponse = sendPost("api/vms/" + vmId + "/supportUsers");
        assertStatusCode200(addSupportUserResponse);
        return addSupportUserResponse.jsonResponse;
    }

    public long enableAdmin(UUID vmId, String username) {
        JSONObject body = new JSONObject();
        body.put("username", username);
        Vps4JsonResponse<JSONObject> enableAdminResponse = sendPost("api/vms/" + vmId.toString() + "/enableAdmin", body);
        assertStatusCode200(enableAdminResponse);
        JSONObject enableAdminJsonResult = enableAdminResponse.jsonResponse;
        return (long) enableAdminJsonResult.get("id");
    }

    public long disableAdmin(UUID vmId, String username) {
        JSONObject body = new JSONObject();
        body.put("username", username);
        Vps4JsonResponse<JSONObject> disableAdminResponse = sendPost("api/vms/" + vmId.toString() + "/disableAdmin", body);
        assertStatusCode200(disableAdminResponse);
        JSONObject enableAdminJsonResult = disableAdminResponse.jsonResponse;
        return (long) enableAdminJsonResult.get("id");
    }

    public UUID createVmCredit(String shopperId, String osType, String controlPanel,
                                int managedLevel, int tier){
        JSONObject body = new JSONObject();
        body.put("tier", tier);
        body.put("managedLevel", managedLevel);
        body.put("operatingSystem", osType);
        body.put("controlPanel", controlPanel);
        body.put("shopperId", shopperId);
        Vps4JsonResponse<JSONObject> createCreditResponse = sendPost("api/support/createCredit", body);
        assertStatusCode200(createCreditResponse);
        JSONObject createCreditJsonResult = createCreditResponse.jsonResponse;
        UUID retVal =  UUID.fromString(createCreditJsonResult.get("orionGuid").toString());

        return retVal;
    }

    public UUID getVmCredit(String shopperId, String os, String panel, String platform) {
        boolean isDed4 = platform.equalsIgnoreCase("OVH");

        Vps4JsonResponse<JSONArray> getCreditsResponse = sendGetList("/api/credits");
        assertStatusCode200(getCreditsResponse);
        JSONArray credits = getCreditsResponse.jsonResponse;
        for (int i=0; i < credits.size(); i++)
        {
            JSONObject credit = (JSONObject) credits.get(i);
            if ((credit != null) && credit.get("controlPanel").toString().equalsIgnoreCase(panel)
                    && credit.get("operatingSystem").toString().equalsIgnoreCase(os)
                    && (Boolean.parseBoolean(credit.get("ded4").toString()) == isDed4))
                return UUID.fromString(credit.get("orionGuid").toString());
        }
        return null;
    }

    public JSONObject getCredit(UUID orionGuid) {
        Vps4JsonResponse<JSONObject> getCreditResponse = sendGetObject("/api/credits/" + orionGuid);
        assertStatusCode200(getCreditResponse);
        return getCreditResponse.jsonResponse;
    }

    public Vps4JsonResponse<JSONArray> getVms() {
        Vps4JsonResponse<JSONArray> getVmsResponse = sendGetList("/api/vms");
        assertStatusCode200(getVmsResponse);
        return getVmsResponse;
    }

    public List<UUID> getListOfExistingVmIds() {
        Vps4JsonResponse<JSONArray> getVmsResponse = getVms();
        List<UUID> vmIds = new ArrayList<>();
        for (Object vmJson : getVmsResponse.jsonResponse) {
            JSONObject vm = (JSONObject) vmJson;
            vmIds.add(UUID.fromString(vm.get("vmId").toString()));
        }
        return vmIds;
    }

    public JSONObject provisionVm(String name, UUID orionGuid,
                      String imageName, int dcId,
                      String username, String password){
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("orionGuid", orionGuid.toString());
        body.put("image", imageName);
        body.put("dataCenterId", dcId);
        body.put("username", username);
        body.put("password", password);

        Vps4JsonResponse<JSONObject> provisionVmResponse = sendPost("api/vms", body);
        return provisionVmResponse.jsonResponse;
    }

    public long restartVm(UUID vmId) {
        Vps4JsonResponse<JSONObject> restartRequestResponse = sendPost("api/vms/"+ vmId + "/restart");
        assertStatusCode200(restartRequestResponse);
        JSONObject restartJsonResult = restartRequestResponse.jsonResponse;
        return (long)restartJsonResult.get("id");
    }

    public long stopVm(UUID vmId) {
        Vps4JsonResponse<JSONObject> stopRequestResponse = sendPost("api/vms/"+ vmId + "/stop");
        assertStatusCode200(stopRequestResponse);
        JSONObject stopJsonResult = stopRequestResponse.jsonResponse;
        return (long)stopJsonResult.get("id");
    }

    public long startVm(UUID vmId) {
        Vps4JsonResponse<JSONObject> startRequestResponse = sendPost("api/vms/"+ vmId + "/start");
        assertStatusCode200(startRequestResponse);
        JSONObject startJsonResult = startRequestResponse.jsonResponse;
        return (long)startJsonResult.get("id");
    }

    public long enableWinexe(UUID vmId) {
        Vps4JsonResponse<JSONObject> winexeResponse = sendPost("api/vms/" + vmId + "/enableWinexe");
        assertStatusCode200(winexeResponse);
        JSONObject startJsonResult = winexeResponse.jsonResponse;
        return (long) startJsonResult.get("id");
    }

    private void pollForActionComplete(UUID vmId, long actionId, long timeoutSeconds, String urlAppendix) {
        Vps4ApiClient.Vps4JsonResponse<JSONObject> result = sendGetObject(urlAppendix);
        Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
        long secondsPassed = 0;
        while (!result.jsonResponse.get("status").equals("COMPLETE") && Instant.now().isBefore(timeout)) {
            result = sendGetObject(urlAppendix);
            if (result.jsonResponse.get("status").equals("ERROR")) {
                throw new RuntimeException("VM action " + actionId + " for vm " + vmId + " failed.");
            }
            try {
                Thread.sleep(5000);
                secondsPassed+=5;
                if (secondsPassed % 30 == 0) {
                    logger.debug("{} seconds passed waiting on action {} for vm {}", secondsPassed, actionId, vmId);
                }
            } catch (InterruptedException e) {
                logger.error("Test was interrupted: ", e);
                throw new RuntimeException(e);
            }
        }
        if (!result.jsonResponse.get("status").equals("COMPLETE")) {
            throw new RuntimeException("Couldn't complete action " + actionId + " in time " + timeoutSeconds + "s for vm " + vmId);
        }
    }

    public void pollForVmActionComplete(UUID vmId, long actionId, long timeoutSeconds) {
        String urlAppendix = "api/vms/" + vmId.toString() + "/actions/" + actionId;
        pollForActionComplete(vmId, actionId, timeoutSeconds, urlAppendix);
    }

    public void pollForSnapshotActionComplete(UUID vmId, UUID snapshotId, long actionId, long timeoutSeconds) {
        String urlAppendix = "api/vms/" + vmId.toString() + "/snapshots/" + snapshotId.toString() + "/actions/" + actionId;
        pollForActionComplete(vmId, actionId, timeoutSeconds, urlAppendix);
    }

    public void pollForVmAgentStatusOK(UUID vmId, long timeoutSeconds) {
        Vps4JsonResponse<JSONObject> result = sendGetObject("api/vms/" + vmId + "/troubleshoot");
        Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
        long secondsPassed = 0;
        while (!((JSONObject)result.jsonResponse.get("status")).get("hfsAgentStatus").equals("OK") && Instant.now().isBefore(timeout)) {
            logger.debug("Retry getting vm agent status for vm {}. JsonResponse: {}", vmId, result.jsonResponse);
            result = sendGetObject("api/vms/" + vmId + "/troubleshoot");
            try {
                Thread.sleep(10000);
                secondsPassed+=10;
            } catch (InterruptedException e) {
                logger.error("Test was interrupted: ", e);
                throw new RuntimeException(e);
            }
        }
        if (!((JSONObject)result.jsonResponse.get("status")).get("hfsAgentStatus").equals("OK")) {
            throw new RuntimeException("VM troubleshoot endpoint does not return OK in time " + timeoutSeconds + "s for vm " + vmId);
        }
        else {
            logger.debug("Getting vm agent status for vm {}. JsonResponse: {}", vmId, result.jsonResponse);
        }
    }

    public Vps4JsonResponse<JSONObject> deleteVm(UUID vmId) {
        String url = baseUrl  + "api/vms/"+ vmId.toString();

        HttpClient client = getHttpClient();
        HttpDelete request = new HttpDelete(url);

        Result result = callApi(client, request);
        JSONObject jsonResponse = convertToJSONObject(result.result.toString());
        return new Vps4JsonResponse<JSONObject>(result.statusCode, jsonResponse);
    }


    private Result sendGet(String urlAppendix){
        String url = baseUrl + urlAppendix;

        HttpClient client = getHttpClient();
        HttpGet request = new HttpGet(url);
        return callApi(client, request);
    }


    protected Result callApi(HttpClient client, HttpRequestBase request){
        // add request header
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", authHeader);
        try{
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            return new Result(statusCode, result);
        }catch(IOException e){
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    private JSONObject convertToJSONObject(String jsonString){
        JSONParser parser = new JSONParser();
        try{
            return (JSONObject) parser.parse(jsonString);
        }catch(ParseException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    public JSONObject snapshotVm(UUID vmId) {
        JSONObject body = new JSONObject();
        body.put("name", "t" + System.currentTimeMillis());
        body.put("snapshotType", "ON_DEMAND");
        Vps4JsonResponse<JSONObject> snapshotVmResponse = sendPost("api/vms/" + vmId + "/snapshots", body);
        assertStatusCode200(snapshotVmResponse);
        return snapshotVmResponse.jsonResponse;
    }

    public String getSnapshotStatus(UUID vmId, UUID snapshotId){
        Vps4JsonResponse<JSONObject> getSnapshotResponse = sendGetObject("api/vms/" + vmId + "/snapshots/" + snapshotId);
        assertStatusCode200(getSnapshotResponse);
        JSONObject snapshot = getSnapshotResponse.jsonResponse;
        return snapshot.get("status").toString();
    }

    public JSONObject restoreVm(UUID vmId, UUID snapshotId, String newPassword) {
        JSONObject body = new JSONObject();
        body.put("backupId", snapshotId.toString());
        if(!newPassword.isEmpty()){
            body.put("password", newPassword);
        }
        Vps4JsonResponse<JSONObject> restoreVmResponse = sendPost("api/vms/" + vmId + "/restore", body);
        assertStatusCode200(restoreVmResponse);
        return restoreVmResponse.jsonResponse;
    }

    public JSONObject createConsoleUrl(UUID vmId) {
        Vps4JsonResponse<JSONObject> createConsoleUrlResponse = sendPost("api/vms/" + vmId + "/consoleUrl");
        assertStatusCode200(createConsoleUrlResponse);
        return createConsoleUrlResponse.jsonResponse;
    }

    public String getConsoleUrl(UUID vmId){
        Vps4JsonResponse<JSONObject> getConsoleUrlResponse = sendGetObject("api/vms/" + vmId + "/consoleUrl");
        assertStatusCode200(getConsoleUrlResponse);
        JSONObject console = getConsoleUrlResponse.jsonResponse;
        return console.get("url").toString();
    }

    public long abuseSuspend(UUID vmId) {
        Vps4JsonResponse<JSONObject> abuseSuspendResponse = sendPost("api/vms/" + vmId + "/abuseSuspend");
        assertStatusCode200(abuseSuspendResponse);
        JSONObject suspendJsonResult = abuseSuspendResponse.jsonResponse;
        return (long)suspendJsonResult.get("id");
    }

    public long reinstateAbuseSuspend(UUID vmId) {
        Vps4JsonResponse<JSONObject> reinstateAbuseSuspendResponse = sendPost("api/vms/" + vmId + "/reinstateAbuseSuspend");
        assertStatusCode200(reinstateAbuseSuspendResponse);
        JSONObject reinstateJsonResult = reinstateAbuseSuspendResponse.jsonResponse;
        return (long)reinstateJsonResult.get("id");
    }

    public JSONObject getServerActions(UUID vmId) {
        Vps4JsonResponse<JSONObject> getVmActionsResponse = sendGetObject("/api/vms/" + vmId + "/actions");
        assertStatusCode200(getVmActionsResponse);
        return getVmActionsResponse.jsonResponse;
    }

    private void assertStatusCode200(Vps4JsonResponse response) {
        if (response.statusCode != 200) {
            logger.debug("json response: {} (status code {})", response.jsonResponse.toString(), response.statusCode);
        }
        assert(response.statusCode == 200);
    }
}
