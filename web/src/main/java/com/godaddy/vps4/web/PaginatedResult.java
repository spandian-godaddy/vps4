package com.godaddy.vps4.web;

import java.util.List;

import javax.ws.rs.core.UriInfo;

public class PaginatedResult<ResultType> {
    
    public class PaginationData{
        public String next;
        public String prev; 
        public long limit;
        public long total;
        
        public PaginationData(String next, String prev, long limit, long total){
            this.next = next;
            this.prev = prev;
            this.limit = limit;
            this.total = total;
        }
    }
    
    public List<ResultType> results;
    public PaginationData pagination;
    
    public PaginatedResult(List<ResultType> results, long limit, long offset, long total, UriInfo uri){
          this.results = results;
          String baseUrl = uri.getAbsolutePath().toString();
          String nextUrl = generateNextUrl(limit, offset, total, baseUrl);
          String prevUrl = generatePreviousUrl(limit, offset, baseUrl);
          this.pagination = new PaginationData(nextUrl, prevUrl, limit, total);
          this.pagination.limit = limit;
          this.pagination. total = total;
    }
    
    private String generatePreviousUrl(long limit, long offset, String baseUrl){
        long prevOffset = offset - limit;
        if( prevOffset < 0){
            prevOffset = 0;
        }
        String newUrl = generateNewUrl(limit, prevOffset, baseUrl);
        return newUrl;
    }
    
    private String generateNextUrl(long limit, long offset, long total, String baseUrl){
        long nextOffset = limit + offset;
        if(nextOffset >= total){
            nextOffset = offset;
        }
        String newUrl = generateNewUrl(limit, nextOffset, baseUrl);
        return newUrl;
    }
    
    private String generateNewUrl(long limit, long offset, String baseUrl) {
        return String.format("%s/?limit=%d&offset=%d", baseUrl, limit, offset);
    }
}
