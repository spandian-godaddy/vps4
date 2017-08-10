package com.godaddy.vps4.phase3.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SsoClient {

    private String baseUrl;

    public SsoClient(String baseUrl){
      this.baseUrl = baseUrl;
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

    public String getVps4SsoToken(String username, String password){
        return getSsoToken(username, password, "idp");
    }

    public String getJomaxSsoToken(String username, String password){
        return getSsoToken(username, password, "jomax");
    }

    @SuppressWarnings("unchecked")
    public String getSsoToken(String username, String password, String realm){
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        body.put("realm", realm);

        Vps4JsonResponse<JSONObject> response = sendPost(body);
        return response.jsonResponse.get("data").toString();
    }

    protected HttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }

    public Vps4JsonResponse<JSONObject> sendPost( JSONObject body){

        HttpClient client = getHttpClient();
        HttpPost post = new HttpPost(baseUrl);

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



    protected Result callApi(HttpClient client, HttpRequestBase request){
        // add request header
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        request.addHeader("Cache-Control", "no-cache");
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

