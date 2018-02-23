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

public class Vps4ApiClient {

    private String baseUrl;
    private String authHeader;

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
        assert(getVmResponse.statusCode == 200);
        JSONObject vm = getVmResponse.jsonResponse;
        JSONObject primaryIp = (JSONObject) vm.get("primaryIpAddress");
        return primaryIp.get("ipAddress").toString();
    }

    @SuppressWarnings("unchecked")
    public long setHostname(UUID vmId, String hostname) {
        JSONObject body = new JSONObject();
        body.put("hostname", hostname);
        Vps4JsonResponse<JSONObject> setHostnameResponse = sendPost("api/vms/" + vmId.toString() + "/setHostname", body);
        assert(setHostnameResponse.statusCode == 200);
        JSONObject setHostnameJsonResult = setHostnameResponse.jsonResponse;
        return (long) setHostnameJsonResult.get("id");

    }

    @SuppressWarnings("unchecked")
    public UUID createVmCredit(String shopperId, String osType, String controlPanel,
                                int managedLevel, int tier){
        JSONObject body = new JSONObject();
        body.put("tier", tier);
        body.put("managedLevel", managedLevel);
        body.put("operatingSystem", osType);
        body.put("controlPanel", controlPanel);
        body.put("shopperId", shopperId);
        Vps4JsonResponse<JSONObject> createCreditResponse = sendPost("api/support/createCredit", body);
        assert(createCreditResponse.statusCode == 200);
        JSONObject createCreditJsonResult = createCreditResponse.jsonResponse;
        UUID retVal =  UUID.fromString(createCreditJsonResult.get("orionGuid").toString());

        return retVal;
    }

    public UUID getVmCredit(String shopperId, String os, String panel) {
        Vps4JsonResponse<JSONArray> getCreditsResponse = sendGetList("/api/credits");
        assert(getCreditsResponse.statusCode == 200);
        JSONArray credits = getCreditsResponse.jsonResponse;
        for (int i=0; i < credits.size(); i++)
        {
            JSONObject credit = (JSONObject) credits.get(i);
            if ( ((String)credit.get("controlPanel")).equalsIgnoreCase(panel) &&
                 ((String)credit.get("operatingSystem")).equalsIgnoreCase(os))
                return UUID.fromString(credit.get("orionGuid").toString());
        }
        return null;
    }

    public Vps4JsonResponse<JSONArray> getVms() {
        Vps4JsonResponse<JSONArray> getVmsResponse = sendGetList("/api/vms");
        assert (getVmsResponse.statusCode == 200);
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

    @SuppressWarnings("unchecked")
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
        assert(restartRequestResponse.statusCode == 200);
        JSONObject resartJsonResult = restartRequestResponse.jsonResponse;
        return (long)resartJsonResult.get("id");
    }

    public long stopVm(UUID vmId) {
        Vps4JsonResponse<JSONObject> restartRequestResponse = sendPost("api/vms/"+ vmId + "/stop");
        assert(restartRequestResponse.statusCode == 200);
        JSONObject resartJsonResult = restartRequestResponse.jsonResponse;
        return (long)resartJsonResult.get("id");
    }

    public long startVm(UUID vmId) {
        Vps4JsonResponse<JSONObject> restartRequestResponse = sendPost("api/vms/"+ vmId + "/start");
        assert(restartRequestResponse.statusCode == 200);
        JSONObject resartJsonResult = restartRequestResponse.jsonResponse;
        return (long)resartJsonResult.get("id");
    }

    public void pollForVmActionComplete(UUID vmId, long actionId) {
        this.pollForVmActionComplete(vmId, actionId, 60);
    }

    public void pollForVmActionComplete(UUID vmId, long actionId, long timeoutSeconds) {
        String urlAppendix = "api/vms/"+vmId.toString()+"/actions/"+actionId;
        Vps4ApiClient.Vps4JsonResponse<JSONObject> result = sendGetObject(urlAppendix);
        Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
        while(! result.jsonResponse.get("status").equals("COMPLETE") && Instant.now().isBefore(timeout)){
            result = sendGetObject(urlAppendix);
            try{
                Thread.sleep(1000);
            }catch(InterruptedException e){
                throw new RuntimeException(e);
            }
        }
        if(!result.jsonResponse.get("status").equals("COMPLETE")){
            throw new RuntimeException("Couldn't complete action " + actionId + "in time " + timeoutSeconds + "s");
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
        }catch(ParseException e){
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
      }

}
