package com.godaddy.vps4.vm;

public class DataCenter {

    public final int dataCenterId;
    public final String dataCenterName;
    
    public DataCenter(int dataCenterId, String dataCenterName) {
        this.dataCenterId = dataCenterId;
        this.dataCenterName = dataCenterName;
    }
    
    public String toString(){
        return "DataCenter [id: " + dataCenterId
                + " name: " + dataCenterName + "]";
    }

}
