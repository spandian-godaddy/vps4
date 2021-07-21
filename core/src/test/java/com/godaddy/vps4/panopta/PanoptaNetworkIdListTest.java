package com.godaddy.vps4.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.vm.VmMetric;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PanoptaNetworkIdListTest {
    private final Injector injector = Guice.createInjector(new ObjectMapperModule());
    private ObjectMapper mapper;

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        mapper = injector.getInstance(ObjectMapper.class);
    }

    private List<PanoptaGraphId> getMockData() throws IOException {
        String json = "{\n" +
                "  \"network_service_list\": [\n" +
                "    {\n" +
                "      \"port\": 22,\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/network_service/4692869\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"port\": 21,\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/network_service/4692873\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        List<PanoptaGraphId> list = mapper.readValue(json, PanoptaNetworkIdList.class).value;
        list.sort(Comparator.comparing(id -> id.type));
        return list;
    }

    @Test
    public void testDeserializer() throws IOException {
        List<PanoptaGraphId> list = getMockData();
        assertEquals(2, list.size());
    }

    @Test
    public void testFtpMetric() throws IOException {
        List<PanoptaGraphId> list = getMockData();
        assertEquals(VmMetric.FTP, list.get(0).type);
        assertEquals(4692873, list.get(0).id);
        assertTrue(list.get(0).metadata.isEmpty());
    }

    @Test
    public void testSshMetric() throws IOException {
        List<PanoptaGraphId> list = getMockData();
        assertEquals(VmMetric.SSH, list.get(1).type);
        assertEquals(4692869, list.get(1).id);
        assertTrue(list.get(1).metadata.isEmpty());
    }
}
