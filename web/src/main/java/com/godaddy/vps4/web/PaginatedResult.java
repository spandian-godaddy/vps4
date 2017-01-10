package com.godaddy.vps4.web;

import java.util.List;

public class PaginatedResult<E> {
    
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
    
    public List<E> results;
    public PaginationData pagination;
    
    public PaginatedResult(List<E> results, long limit, long offset, long total, String baseUrl){
          this.results = results;
          this.pagination = new PaginationData("OMG NEXT", "OMG PREV", limit, total);
          this.pagination.limit = limit;
          this.pagination. total = total;
    }
}
