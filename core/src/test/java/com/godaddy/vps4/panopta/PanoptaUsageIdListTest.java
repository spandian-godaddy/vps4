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

public class PanoptaUsageIdListTest {
    private final Injector injector = Guice.createInjector(new ObjectMapperModule());
    private ObjectMapper mapper;

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        mapper = injector.getInstance(ObjectMapper.class);
    }

    private List<PanoptaMetricId> getMockData() throws IOException {
        String json = "{\n" +
                "  \"agent_resource_list\": [\n" +
                "    {\n" +
                "      \"agent_resource_type\": \"https://api2.panopta.com/v2/agent_resource_type/675\",\n" +
                "      \"resource_option\": {},\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/agent_resource/78917415\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"agent_resource_type\": \"https://api2.panopta.com/v2/agent_resource_type/575\",\n" +
                "      \"resource_option\": \"/dev/sda3 mounted at /var\",\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/agent_resource/78917436\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        List<PanoptaMetricId> list = mapper.readValue(json, PanoptaUsageIdList.class).value;
        list.sort(Comparator.comparing(id -> id.typeId));
        return list;
    }

    @Test
    public void testDeserializer() throws IOException {
        List<PanoptaMetricId> list = getMockData();
        assertEquals(2, list.size());
    }

    @Test
    public void testDiskMetric() throws IOException {
        List<PanoptaMetricId> list = getMockData();
        assertEquals(575, list.get(0).typeId);
        assertEquals(78917436, list.get(0).id);
        assertEquals("/var", list.get(0).metadata.get("mountPoint"));
    }

    @Test
    public void testRamMetric() throws IOException {
        List<PanoptaMetricId> list = getMockData();
        assertEquals(675, list.get(1).typeId);
        assertEquals(78917415, list.get(1).id);
        assertTrue(list.get(1).metadata.isEmpty());
    }
}
