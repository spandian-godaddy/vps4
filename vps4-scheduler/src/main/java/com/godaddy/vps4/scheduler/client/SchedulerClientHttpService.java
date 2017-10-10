package com.godaddy.vps4.scheduler.client;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerClientHttpService implements SchedulerClientService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerClientHttpService.class);

    private final String baseUrl;

    private final CloseableHttpClient httpClient;

    private final ObjectMapper mapper;

    @Inject
    public SchedulerClientHttpService(String baseUrl, CloseableHttpClient httpClient, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public SchedulerResponse submitJobToGroup(String product, String jobGroup, RequestBody requestJson) {
        if(isBlank(product)) {
            throw new InvalidRequestException("Missing parameter [product] for request.");
        }
        if(isBlank(jobGroup)) {
            throw new InvalidRequestException("Missing parameter [jobGroup] for request.");
        }
        if(requestJson == null) {
            throw new InvalidRequestException("Missing request json body for request.");
        }
        if(requestJson.getVmId() == null) {
            throw new InvalidRequestException("Missing request json body element [vmId] for request.");
        }
        if(requestJson.getWhen() == null) {
            throw new InvalidRequestException("Missing request json body element [when] for request.");
        }

        String pathParams = join(new Object[] {product, jobGroup, "jobs"}, "/");
        String requestUri = baseUrl + "/api/scheduler/" + pathParams;
        HttpPost postRequest = new HttpPost(requestUri);
        postRequest.addHeader("Content-Type", "application/json");
        try {
            postRequest.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(requestJson)));
        } catch (JsonProcessingException e) {
            logger.error("Could not map values from request: ", e);
            throw new RuntimeException(e);
        }
        try {
            logger.info("Request: {}, Request Body JSON: {} ", postRequest.toString(), mapper.writeValueAsString(requestJson));
        } catch (JsonProcessingException e) {
            logger.info("Request logging failed. ", e);
        }
        return executePostRequest(postRequest, SchedulerResponse.class);
    }

    @Override
    public List<SchedulerResponse> getGroupJobs(String product, String jobGroup) {
        if(isBlank(product)) {
            throw new InvalidRequestException("Missing parameter [product] for request.");
        }
        if(isBlank(jobGroup)) {
            throw new InvalidRequestException("Missing parameter [jobGroup] for request.");
        }

        String pathParams = join(new Object[] {product, jobGroup, "jobs"}, "/");
        String requestUri = baseUrl + "/api/scheduler/" + pathParams;
        HttpGet getRequest = new HttpGet(requestUri);
        logger.info("Request: {} ", getRequest.toString());
        return Arrays.asList(executeGetRequest(getRequest, SchedulerResponse[].class));
    }

    @Override
    public SchedulerResponse getJob(String product, String jobGroup, UUID jobId) {
        if(isBlank(product)) {
            throw new InvalidRequestException("Missing parameter [product] for request.");
        }
        if(isBlank(jobGroup)) {
            throw new InvalidRequestException("Missing parameter [jobGroup] for request.");
        }
        if(jobId == null) {
            throw new InvalidRequestException("Missing parameter [jobId] for request.");
        }

        String pathParams = join(new Object[] {product, jobGroup, "jobs"}, "/");
        String requestUri = baseUrl + "/api/scheduler/" + pathParams;
        HttpGet getRequest = new HttpGet(requestUri);
        logger.info("Request: {} ", getRequest.toString());
        return executeGetRequest(getRequest, SchedulerResponse[].class)[0];
    }

    @Override
    public SchedulerResponse rescheduleJob(String product, String jobGroup, UUID jobId, RequestBody requestJson) {
        if(isBlank(product)) {
            throw new InvalidRequestException("Missing parameter [product] for request.");
        }
        if(isBlank(jobGroup)) {
            throw new InvalidRequestException("Missing parameter [jobGroup] for request.");
        }
        if(jobId == null) {
            throw new InvalidRequestException("Missing parameter [jobId] for request.");
        }
        if(requestJson == null) {
            throw new InvalidRequestException("Missing request json body for request.");
        }
        if(requestJson.getVmId() == null) {
            throw new InvalidRequestException("Missing request json body element [vmId] for request.");
        }
        if(requestJson.getWhen() == null) {
            throw new InvalidRequestException("Missing request json body element [when] for request.");
        }

        String pathParams = join(new Object[] {product, jobGroup, "jobs", jobId}, "/");
        String requestUri = baseUrl + "/api/scheduler/" + pathParams;
        HttpPatch patchRequest = new HttpPatch(requestUri);
        patchRequest.addHeader("Content-Type", "application/json");
        try {
            patchRequest.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(requestJson)));
        } catch (JsonProcessingException e) {
            logger.error("Could not map values from request: ", e);
            throw new RuntimeException(e);
        }
        try {
            logger.info("Request: {}, Request Body JSON: {} ", patchRequest.toString(), mapper.writeValueAsString(requestJson));
        } catch (JsonProcessingException e) {
            logger.info("Request logging failed. ", e);
        }
        return executePostRequest(patchRequest, SchedulerResponse.class);
    }

    @Override
    public void deleteJob(String product, String jobGroup, UUID jobId) {
        if(isBlank(product)) {
            throw new InvalidRequestException("Missing parameter [product] for request.");
        }
        if(isBlank(jobGroup)) {
            throw new InvalidRequestException("Missing parameter [jobGroup] for request.");
        }
        if(jobId == null) {
            throw new InvalidRequestException("Missing parameter [jobId] for request.");
        }

        String pathParams = join(new Object[] {product, jobGroup, "jobs", jobId}, "/");
        String requestUri = baseUrl + "/api/scheduler/" + pathParams;
        HttpDelete deleteRequest = new HttpDelete(requestUri);
        logger.info("Request: {} ", deleteRequest.toString());
        executeGetRequest(deleteRequest, SchedulerResponse[].class);
    }

    private <SchedulerResponse> SchedulerResponse[] executeGetRequest(HttpRequestBase requestBase, Class<SchedulerResponse[]> responseClass) {

        try {
            CloseableHttpResponse response = httpClient.execute(requestBase);
            return handleResponse(response, httpResponse -> {
                if (httpResponse == null) {
                    logger.info("Response object is null.");
                    return null;
                }
                try {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String responseString = EntityUtils.toString(response.getEntity());
                        logger.info("Response: {}", responseString);
                        return mapper.readValue(responseString, responseClass);
                    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                        logger.info("Response Status: {} {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                        return null; // TODO: should an empty response be returned? Why is the delete job not returning a response?
                    }
                    throw new InvalidResponseException("Could not parse response.");
                } catch (IOException ioe) {
                    logger.error("Exception encountered during response handling: {}", ioe);
                    throw new RuntimeException(ioe);
                }
            });
        } catch (InvalidResponseException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidResponseException(e);
        }
    }

    private <SchedulerResponse> SchedulerResponse executePostRequest(HttpRequestBase requestBase, Class<SchedulerResponse> responseClass) {

        try {
            CloseableHttpResponse response = httpClient.execute(requestBase);
            return handleResponse(response, httpResponse -> {
                if (httpResponse == null) {
                    logger.info("Response object is null.");
                    return null;
                }
                try {
                    HttpEntity entity = response.getEntity();
                    if(entity != null) {
                        String responseString = EntityUtils.toString(response.getEntity());
                        logger.info("Response: {}", responseString);
                        return mapper.readValue(responseString, responseClass);
                    }
                    throw new InvalidResponseException("Could not parse response.");
                }catch (IOException ioe) {
                    logger.error("Exception encountered during response handling: {}", ioe);
                    throw new RuntimeException(ioe);
                }
            });
        } catch (InvalidResponseException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidResponseException(e);
        }
    }

    private <SchedulerResponse> SchedulerResponse handleResponse(
            CloseableHttpResponse response,
            Function<CloseableHttpResponse, SchedulerResponse> function) {

        try {
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {

                    return function.apply(response);

                } else {
                    throw new InvalidResponseException(new IllegalStateException("Invalid response status code: " + status).fillInStackTrace());
                }
            } finally {
                response.close();
            }

        } catch (IOException e) {
            throw new InvalidResponseException(new RuntimeException(e).fillInStackTrace());
        }
    }

}
