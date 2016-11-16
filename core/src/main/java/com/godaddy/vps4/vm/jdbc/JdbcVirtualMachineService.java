package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.ConnectionProvider;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineRequest;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;

public class JdbcVirtualMachineService implements VirtualMachineService {

    private final DataSource dataSource;

    @Inject
    public JdbcVirtualMachineService(DataSource dataSource) {
        this.dataSource = dataSource;
        new ConnectionProvider(dataSource);
    }

    @Override
    public VirtualMachineSpec getSpec(String name) {

        return Sql.with(dataSource)
                .exec("SELECT spec_id, spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as \"spec_valid_on\", "
                        + "valid_until as \"spec_valid_until\" FROM virtual_machine_spec WHERE spec_name=? ",
                        Sql.nextOrNull(this::mapVirtualMachineSpec), name);
    }

    @Override
    public List<VirtualMachine> listVirtualMachines(long projectId) {
        return Sql.with(dataSource)
                .exec("SELECT vm.vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\", "
                        + "vm.valid_on as \"vm_valid_on\", vm.valid_until as \"vm_valid_until\", vms.spec_id, vms.spec_name, "
                        + "vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
                        + "vms.valid_until as \"spec_valid_until\", image.name as \"image_name\" FROM virtual_machine vm "
                        + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id " + "JOIN image ON image.image_id=vm.image_id "
                        + " WHERE project_id=?", Sql.listOf(this::mapVirtualMachine), projectId);
    }

    public VirtualMachine getVirtualMachine(long vmId) {
        return Sql.with(dataSource)
                .exec("SELECT vm.vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\", "
                        + "vm.valid_on as \"vm_valid_on\", vm.valid_until as \"vm_valid_until\", vms.spec_id, vms.spec_name, "
                        + "vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
                        + "vms.valid_until as \"spec_valid_until\", image.name as \"image_name\" FROM virtual_machine vm "
                        + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id JOIN image ON image.image_id=vm.image_id "
                        + " WHERE vm_id=?", Sql.nextOrNull(this::mapVirtualMachine), vmId);
    }

    public VirtualMachine getVirtualMachine(UUID orionGuid) {
        return Sql.with(dataSource)
                .exec("SELECT vm.vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\","
                        + "vm.valid_on as \"vm_valid_on\", vm.valid_until as \"vm_valid_until\", vms.spec_id, vms.spec_name, "
                        + "vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
                        + "vms.valid_until as \"spec_valid_until\", image.name as \"image_name\" FROM virtual_machine vm "
                        + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id JOIN image ON image.image_id=vm.image_id "
                        + " WHERE orion_guid=?", Sql.nextOrNull(this::mapVirtualMachine), orionGuid);
    }

    protected VirtualMachine mapVirtualMachine(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("vm_valid_until");
        VirtualMachineSpec spec = mapVirtualMachineSpec(rs);

        return new VirtualMachine(rs.getLong("vm_id"), java.util.UUID.fromString(rs.getString("orion_guid")), rs.getLong("project_id"),
                spec, rs.getString("vm_name"), rs.getString("image_name"), rs.getTimestamp("vm_valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null);
    }

    protected VirtualMachineSpec mapVirtualMachineSpec(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("spec_valid_until");

        return new VirtualMachineSpec(rs.getInt("spec_id"), rs.getString("spec_name"), rs.getInt("tier"), rs.getInt("cpu_core_count"),
                rs.getInt("memory_mib"), rs.getInt("disk_gib"), rs.getTimestamp("spec_valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null);
    }

    @Override
    public VirtualMachine createVirtualMachine(long vmId, long projectId, String spec, String name) {

        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_create(?,?,?,?)", null, vmId, projectId, spec, name);

        return null;
    }

    @Override
    public void destroyVirtualMachine(long vmId) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET valid_until=NOW() WHERE vm_id=?", null, vmId);
    }

    @Override
    public VirtualMachineSpec getSpec(int tier) {
        return Sql.with(dataSource)
                .exec("SELECT spec_id, spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as \"spec_valid_on\", "
                        + "valid_until as \"spec_valid_until\" FROM virtual_machine_spec WHERE tier=? ",
                        Sql.nextOrNull(this::mapVirtualMachineSpec), tier);
    }

    @Override
    public void createVirtualMachineRequest(UUID orionGuid, String operatingSystem, String controlPanel, int tier, int managedLevel) {
        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_request_create(?,?,?,?,?)", null, orionGuid, operatingSystem, tier,
                controlPanel, managedLevel);
    }

    @Override
    public VirtualMachineRequest getVirtualMachineRequest(UUID orionGuid) {
        return Sql.with(dataSource).exec("SELECT * FROM virtual_machine_request WHERE orion_guid = ?",
                Sql.nextOrNull(this::mapVirtualMachineRequest), orionGuid);
    }

    private VirtualMachineRequest mapVirtualMachineRequest(ResultSet rs) throws SQLException {
        Timestamp provisionDate = rs.getTimestamp("provision_date");

        return new VirtualMachineRequest(java.util.UUID.fromString(rs.getString("orion_guid")), rs.getInt("tier"),
                rs.getInt("managed_level"), rs.getString("operating_system"), rs.getString("control_panel"),
                rs.getTimestamp("create_date").toInstant(), provisionDate != null ? provisionDate.toInstant() : null);
    }

    @Override
    public void provisionVirtualMachine(long vmId, UUID orionGuid, String name, long projectId, int specId, int managedLevel, long imageId) {
        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_provision(?, ?, ?, ?, ?, ?, ?)", null, vmId, orionGuid, name, projectId,
                specId, managedLevel, imageId);
    }
    
    @Override
    public void updateVirtualMachine(long vmId, Map<String, Object> paramsToUpdate){
        if(paramsToUpdate.isEmpty())
            return;
        ArrayList<Object> values = new ArrayList<Object>() ;
        StringBuilder nameSets= new StringBuilder() ;
        nameSets.append("UPDATE virtual_machine vm SET ");
        for(Map.Entry<String,Object> pair: paramsToUpdate.entrySet()){
            if(values.size() > 0)
                nameSets.append(", ");
            nameSets.append(pair.getKey());
            nameSets.append("=?");
            values.add(pair.getValue());
        }
        nameSets.append(" WHERE vm_id=?");
        values.add(vmId);
        Sql.with(dataSource).exec(nameSets.toString(), null, values.toArray());
    }
}
