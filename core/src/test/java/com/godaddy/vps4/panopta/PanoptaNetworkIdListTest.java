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

    private List<PanoptaMetricId> getMockData() throws IOException {
        String json = "{\n" +
                "  \"network_service_list\": [\n" +
                "    {\n" +
                "      \"service_type\": \"https://api2.panopta.com/v2/network_service_type/111\",\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/network_service/4692869\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"service_type\": \"https://api2.panopta.com/v2/network_service_type/311\",\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/network_service/4692873\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        List<PanoptaMetricId> list = mapper.readValue(json, PanoptaNetworkIdList.class).value;
        list.sort(Comparator.comparing(id -> id.typeId));
        return list;
    }

    @Test
    public void testDeserializer() throws IOException {
        List<PanoptaMetricId> list = getMockData();
        assertEquals(2, list.size());
    }

    @Test
    public void testFtpMetric() throws IOException {
        List<PanoptaMetricId> list = getMockData();
        assertEquals(111, list.get(0).typeId);
        assertEquals(4692869, list.get(0).id);
        assertTrue(list.get(0).metadata.isEmpty());
    }

    @Test
    public void testSshMetric() throws IOException {
        List<PanoptaMetricId> list = getMockData();
        assertEquals(311, list.get(1).typeId);
        assertEquals(4692873, list.get(1).id);
        assertTrue(list.get(1).metadata.isEmpty());
    }
}
