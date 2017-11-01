package com.godaddy.vps4.vm;

public class DataCenter {

    public int dataCenterId;
    public String dataCenterName;
    
    public DataCenter() {
    }
    
    public DataCenter(int dataCenterId, String dataCenterName) {
        this.dataCenterId = dataCenterId;
        this.dataCenterName = dataCenterName;
    }
    
    public String toString(){
        return "DataCenter [id: " + dataCenterId
                + " name: " + dataCenterName + "]";
    }

}
