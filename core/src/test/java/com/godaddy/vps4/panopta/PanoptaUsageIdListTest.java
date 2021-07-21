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

public class PanoptaUsageIdListTest {
    private final Injector injector = Guice.createInjector(new ObjectMapperModule());
    private ObjectMapper mapper;

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        mapper = injector.getInstance(ObjectMapper.class);
    }

    private List<PanoptaGraphId> getMockData() throws IOException {
        String json = "{\n" +
                "  \"agent_resource_list\": [\n" +
                "    {\n" +
                "      \"name\": \"vps4_ram_total_percent_used\",\n" +
                "      \"resource_option\": {},\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/agent_resource/78917415\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"vps4_disk_total_percent_used\",\n" +
                "      \"resource_option\": \"/dev/sda3 mounted at /var\",\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/8211236/agent_resource/78917436\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        List<PanoptaGraphId> list = mapper.readValue(json, PanoptaUsageIdList.class).value;
        list.sort(Comparator.comparing(id -> id.type));
        return list;
    }

    @Test
    public void testDeserializer() throws IOException {
        List<PanoptaGraphId> list = getMockData();
        assertEquals(2, list.size());
    }

    @Test
    public void testRamMetric() throws IOException {
        List<PanoptaGraphId> list = getMockData();
        assertEquals(VmMetric.RAM, list.get(0).type);
        assertEquals(78917415, list.get(0).id);
        assertTrue(list.get(0).metadata.isEmpty());
    }

    @Test
    public void testDiskMetric() throws IOException {
        List<PanoptaGraphId> list = getMockData();
        assertEquals(VmMetric.DISK, list.get(1).type);
        assertEquals(78917436, list.get(1).id);
        assertEquals("/var", list.get(1).metadata.get("mountPoint"));
    }
}
