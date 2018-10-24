package com.godaddy.vps4.jdbc;

import java.util.List;

public class ResultSubset<ResultType> {
    public long totalRows;
    public List<ResultType> results;

    public ResultSubset(List<ResultType> results, long totalRows){
        this.results = results;
        this.totalRows = totalRows;
    }

}
