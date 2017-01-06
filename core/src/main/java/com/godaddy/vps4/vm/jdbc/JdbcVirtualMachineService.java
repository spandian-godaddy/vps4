package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.ConnectionProvider;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineRequest;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;

public class JdbcVirtualMachineService implements VirtualMachineService {

    private final DataSource dataSource;

    private NetworkService networkService;
    private ImageService imageService;

    private String selectVirtualMachineQuery = "SELECT vm.id, vm.vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\", "
            + "vm.valid_on as \"vm_valid_on\", vm.valid_until as \"vm_valid_until\", vms.spec_id, vms.spec_name, "
            + "vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
            + "vms.valid_until as \"spec_valid_until\", vms.name as \"spec_vps4_name\", image.name as \"image_name\" FROM virtual_machine vm "
            + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id "
            + "JOIN image ON image.image_id=vm.image_id ";

    @Inject
    public JdbcVirtualMachineService(DataSource dataSource, NetworkService networkService, ImageService imageService) {
        this.dataSource = dataSource;
        new ConnectionProvider(dataSource);
        this.networkService = networkService;
        this.imageService = imageService;
    }

    @Override
    public VirtualMachineSpec getSpec(String name) {

        return Sql.with(dataSource)
                .exec("SELECT spec_id, spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as \"spec_valid_on\", "
                        + "valid_until as \"spec_valid_until\" FROM virtual_machine_spec WHERE spec_name=? ",
                        Sql.nextOrNull(this::mapVirtualMachineSpec), name);
    }

    @Override
    public List<VirtualMachine> getVirtualMachinesForProject(long projectId) {
        return Sql.with(dataSource)
                .exec(selectVirtualMachineQuery + "WHERE vm.project_id=?", Sql.listOf(this::mapVirtualMachine), projectId);
    }

    public VirtualMachine getVirtualMachine(long hfsVmId) {
        return Sql.with(dataSource)
                .exec(selectVirtualMachineQuery + "WHERE vm.vm_id=?", Sql.nextOrNull(this::mapVirtualMachine), hfsVmId);
    }

    public VirtualMachine getVirtualMachine(UUID orionGuid) {
        return Sql.with(dataSource)
                .exec(selectVirtualMachineQuery + "WHERE vm.orion_guid=?", Sql.nextOrNull(this::mapVirtualMachine), orionGuid);
    }

    protected VirtualMachine mapVirtualMachine(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("vm_valid_until");
        VirtualMachineSpec spec = mapVirtualMachineSpec(rs);
        long vmId = rs.getLong("vm_id");
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        Image image = imageService.getImage(rs.getString("image_name"));

        return new VirtualMachine(java.util.UUID.fromString(rs.getString("id")), rs.getLong("vm_id"), java.util.UUID.fromString(rs.getString("orion_guid")), rs.getLong("project_id"),
                spec, rs.getString("vm_name"), image, ipAddress, rs.getTimestamp("vm_valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null);
    }

    protected VirtualMachineSpec mapVirtualMachineSpec(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("spec_valid_until");

        return new VirtualMachineSpec(rs.getInt("spec_id"), rs.getString("spec_vps4_name"), rs.getString("spec_name"), rs.getInt("tier"),
                rs.getInt("cpu_core_count"), rs.getInt("memory_mib"), rs.getInt("disk_gib"), rs.getTimestamp("spec_valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null);
    }

    @Override
    public void destroyVirtualMachine(long vmId) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET valid_until=NOW() WHERE vm_id=?", null, vmId);
    }

    @Override
    public VirtualMachineSpec getSpec(int tier) {
        return Sql.with(dataSource)
                .exec("SELECT spec_id, name as \"spec_vps4_name\", spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as \"spec_valid_on\", "
                        + "valid_until as \"spec_valid_until\" FROM virtual_machine_spec WHERE tier=? ",
                        Sql.nextOrNull(this::mapVirtualMachineSpec), tier);
    }

    @Override
    public void createVirtualMachineRequest(UUID orionGuid, String operatingSystem, String controlPanel, int tier, int managedLevel, String shopperId) {
        Sql.with(dataSource).exec("SELECT * FROM orion_request_create(?,?,?,?,?,?)", 
                null, orionGuid, operatingSystem, tier,
                controlPanel, managedLevel, shopperId);
    }

    @Override
    public VirtualMachineRequest getVirtualMachineRequest(UUID orionGuid) {
        return Sql.with(dataSource).exec("SELECT * FROM orion_request WHERE orion_guid = ?",
                Sql.nextOrNull(this::mapVirtualMachineRequest), orionGuid);
    }

    private VirtualMachineRequest mapVirtualMachineRequest(ResultSet rs) throws SQLException {
        Timestamp provisionDate = rs.getTimestamp("provision_date");

        return new VirtualMachineRequest(java.util.UUID.fromString(rs.getString("orion_guid")), rs.getInt("tier"),
                rs.getInt("managed_level"), rs.getString("operating_system"), rs.getString("control_panel"),
                rs.getTimestamp("create_date").toInstant(), provisionDate != null ? provisionDate.toInstant() : null,
                rs.getString("shopper_id"));
    }

    @Override
    public UUID provisionVirtualMachine(long vmId, UUID orionGuid, String name, 
                                        long projectId, int specId, int managedLevel, long imageId) {
        UUID virtual_machine_id = UUID.randomUUID();
        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_provision(?, ?, ?, ?, ?, ?, ?, ?)", null, 
                virtual_machine_id, vmId, orionGuid, name, projectId, specId, managedLevel, imageId);
        return virtual_machine_id;
    }
    
    @Override
    public void addHfsVmIdToVirtualMachine(UUID vmId, long hfsVmId){
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("vm_id", hfsVmId);
        updateVirtualMachine(vmId, vmPatchMap);
    }

    @Override
    public void updateVirtualMachine(UUID id, Map<String, Object> paramsToUpdate) {
        if (paramsToUpdate.isEmpty())
            return;
        ArrayList<Object> values = new ArrayList<Object>();
        StringBuilder nameSets = new StringBuilder();
        nameSets.append("UPDATE virtual_machine vm SET ");
        for (Map.Entry<String, Object> pair : paramsToUpdate.entrySet()) {
            if (values.size() > 0)
                nameSets.append(", ");
            nameSets.append(pair.getKey());
            nameSets.append("=?");
            values.add(pair.getValue());
        }
        nameSets.append(" WHERE id=?");
        values.add(id);
        Sql.with(dataSource).exec(nameSets.toString(), null, values.toArray());
    }

    @Override
    public List<VirtualMachine> getVirtualMachinesForUser(long vps4UserId) {
        return Sql.with(dataSource).exec(selectVirtualMachineQuery
                + "JOIN user_project_privilege up ON up.project_id = vm.project_id "
                + "JOIN vps4_user u ON up.vps4_user_id = u.vps4_user_id "
                + "WHERE u.vps4_user_id = ? "
                + "AND vm.valid_until = 'infinity'",
                Sql.listOf(this::mapVirtualMachine), vps4UserId);
    }

    @Override
    public List<VirtualMachineRequest> getOrionRequests(String shopperId) {
        return (List<VirtualMachineRequest>) Sql.with(dataSource).exec(
                "SELECT * from orion_request WHERE shopper_id = ? AND provision_date IS NULL",
                Sql.listOf(this::mapVirtualMachineRequest), shopperId);
    }

    @Override
    public void createOrionRequestIfNoneExists(Vps4User vps4User) {
        Sql.with(dataSource).exec("SELECT * FROM auto_create_orion_request(?, ?, ?, ?, ?)", null, vps4User.getId(), 10, "linux", "cpanel",
                1);
    }
}
