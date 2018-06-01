package com.godaddy.vps4.vm;

import java.util.List;

public interface DataCenterService {

    DataCenter getDataCenter(int dataCenterId);

    List<DataCenter> getDataCentersByReseller(String resellerId);
}
