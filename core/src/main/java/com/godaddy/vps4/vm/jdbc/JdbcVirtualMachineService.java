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

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;

public class JdbcVirtualMachineService implements VirtualMachineService {

    private final DataSource dataSource;

    private String selectVirtualMachineQuery = "SELECT vm.vm_id, vm.hfs_vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\", "
            + "vm.hostname, vm.valid_on as \"vm_valid_on\", vm.valid_until as \"vm_valid_until\", vms.spec_id, vms.spec_name, "
            + "vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
            + "vms.valid_until as \"spec_valid_until\", vms.name as \"spec_vps4_name\", "
            + "image.name, image.hfs_name, image.image_id, image.control_panel_id, image.os_type_id, "
            + "ip.ip_address_id, ip.ip_address, ip.ip_address_type_id, ip.valid_on, ip.valid_until, "
            + "dc.data_center_id, dc.description "
            + "FROM virtual_machine vm "
            + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id "
            + "JOIN image ON image.image_id=vm.image_id "
            + "JOIN project prj ON prj.project_id=vm.project_id "
            + "JOIN data_center dc ON dc.data_center_id=prj.data_center_id "
            + "LEFT JOIN ip_address ip ON ip.vm_id = vm.vm_id AND ip.ip_address_type_id = 1";
    @Inject
    public JdbcVirtualMachineService(DataSource dataSource) {
        this.dataSource = dataSource;
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
                .exec(selectVirtualMachineQuery + "WHERE vm.hfs_vm_id=?", Sql.nextOrNull(this::mapVirtualMachine), hfsVmId);
    }

    public VirtualMachine getVirtualMachineByOrionGuid(UUID orionGuid) {
        return Sql.with(dataSource)
                .exec(selectVirtualMachineQuery + "WHERE vm.orion_guid=?", Sql.nextOrNull(this::mapVirtualMachine), orionGuid);
    }

    public VirtualMachine getVirtualMachine(UUID vmId) {
        return Sql.with(dataSource)
                .exec(selectVirtualMachineQuery + "WHERE vm.vm_id=?", Sql.nextOrNull(this::mapVirtualMachine), vmId);
    }

    protected VirtualMachine mapVirtualMachine(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("vm_valid_until");
        VirtualMachineSpec spec = mapVirtualMachineSpec(rs);
        UUID vmId = java.util.UUID.fromString(rs.getString("vm_id"));
        IpAddress ipAddress = mapIpAddress(rs);
        Image image = mapImage(rs);
        DataCenter dataCenter = mapDataCenter(rs);

        return new VirtualMachine(vmId, rs.getLong("hfs_vm_id"),
                java.util.UUID.fromString(rs.getString("orion_guid")),
                rs.getLong("project_id"),
                spec, rs.getString("vm_name"),
                image, ipAddress, dataCenter,
                rs.getTimestamp("vm_valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null,
                rs.getString("hostname"));
    }

    protected IpAddress mapIpAddress(ResultSet rs) throws SQLException {
        long ipAddressId = rs.getLong("ip_address_id");
        if (ipAddressId == 0) {
            return null;
        }
        return new IpAddress(rs.getLong("ip_address_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("ip_address"),
                IpAddress.IpAddressType.valueOf(rs.getInt("ip_address_type_id")),
                rs.getTimestamp("valid_on").toInstant(),
                rs.getTimestamp("valid_until").toInstant());
    }

    private Image mapImage(ResultSet rs) throws SQLException {
        Image image = new Image();

        image.imageName = rs.getString("name");
        image.hfsName = rs.getString("hfs_name");
        image.imageId = rs.getLong("image_id");
        image.controlPanel = ControlPanel.valueOf(rs.getInt("control_panel_id"));
        image.operatingSystem = OperatingSystem.valueOf(rs.getInt("os_type_id"));

        return image;
    }

    private DataCenter mapDataCenter(ResultSet rs) throws SQLException {
        return new DataCenter(rs.getInt("data_center_id"),
                rs.getString("description"));
    }

    protected VirtualMachineSpec mapVirtualMachineSpec(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("spec_valid_until");

        return new VirtualMachineSpec(rs.getInt("spec_id"), rs.getString("spec_vps4_name"), rs.getString("spec_name"), rs.getInt("tier"),
                rs.getInt("cpu_core_count"), rs.getInt("memory_mib"), rs.getInt("disk_gib"), rs.getTimestamp("spec_valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null);
    }

    @Override
    public void destroyVirtualMachine(long vmId) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET valid_until=NOW() WHERE hfs_vm_id=?", null, vmId);
    }

    @Override
    public VirtualMachineSpec getSpec(int tier) {
        return Sql.with(dataSource)
                .exec("SELECT spec_id, name as \"spec_vps4_name\", spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as \"spec_valid_on\", "
                        + "valid_until as \"spec_valid_until\" FROM virtual_machine_spec WHERE tier=? ",
                        Sql.nextOrNull(this::mapVirtualMachineSpec), tier);
    }

    @Override
    public void createVirtualMachineCredit(UUID orionGuid, String operatingSystem, String controlPanel, int tier, int managedLevel, String shopperId) {
        Sql.with(dataSource).exec("SELECT * FROM credit_create(?,?,?,?,?,?)",
                null, orionGuid, operatingSystem, tier,
                controlPanel, managedLevel, shopperId);
    }

    @Override
    public VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid) {
        return Sql.with(dataSource).exec("SELECT * FROM credit WHERE orion_guid = ?",
                Sql.nextOrNull(this::mapVirtualMachineCredit), orionGuid);
    }

    private VirtualMachineCredit mapVirtualMachineCredit(ResultSet rs) throws SQLException {
        Timestamp provisionDate = rs.getTimestamp("provision_date");

        return new VirtualMachineCredit(java.util.UUID.fromString(rs.getString("orion_guid")), rs.getInt("tier"),
                rs.getInt("managed_level"), rs.getString("operating_system"), rs.getString("control_panel"),
                rs.getTimestamp("create_date").toInstant(), provisionDate != null ? provisionDate.toInstant() : null,
                rs.getString("shopper_id"));
    }

    @Override
    public UUID provisionVirtualMachine(UUID orionGuid, String name,
                                        long projectId, int specId, int managedLevel, long imageId) {
        UUID virtual_machine_id = UUID.randomUUID();
        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_provision(?, ?, ?, ?, ?, ?, ?)", null,
                virtual_machine_id, orionGuid, name, projectId, specId, managedLevel, imageId);
        return virtual_machine_id;
    }

    @Override
    public void addHfsVmIdToVirtualMachine(UUID vmId, long hfsVmId){
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("hfs_vm_id", hfsVmId);
        updateVirtualMachine(vmId, vmPatchMap);
    }

    @Override
    public void setHostname(UUID vmId, String hostname){
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("hostname", hostname);
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
        nameSets.append(" WHERE vm_id=?");
        values.add(id);
        Sql.with(dataSource).exec(nameSets.toString(), null, values.toArray());
    }

    @Override
    public List<VirtualMachine> getVirtualMachinesForUser(long vps4UserId) {
        return Sql.with(dataSource).exec(selectVirtualMachineQuery
                + "JOIN user_project_privilege up ON up.project_id = vm.project_id "
                + "JOIN vps4_user u ON up.vps4_user_id = u.vps4_user_id "
                + "WHERE u.vps4_user_id = ?",
                Sql.listOf(this::mapVirtualMachine), vps4UserId);
    }

    @Override
    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId) {
        return (List<VirtualMachineCredit>) Sql.with(dataSource).exec(
                "SELECT * from credit WHERE shopper_id = ? AND provision_date IS NULL",
                Sql.listOf(this::mapVirtualMachineCredit), shopperId);
    }

    @Override
    public void createOrionRequestIfNoneExists(Vps4User vps4User) {
        Sql.with(dataSource).exec("SELECT * FROM auto_create_credit(?, ?, ?, ?, ?)", null, vps4User.getId(), 10, "linux", "cpanel",
                1);
    }

    @Override
    public boolean virtualMachineHasCpanel(UUID vmId){
        List<VirtualMachine> vms = Sql.with(dataSource).exec(selectVirtualMachineQuery
                + "JOIN control_panel ON image.control_panel_id = control_panel.control_panel_id "
                + "where control_panel.name = 'cpanel' "
                + "and vm.vm_id = ?;", Sql.listOf(this::mapVirtualMachine), vmId);
        return vms.size() > 0;
	}	
		
    public VirtualMachineCredit getAndReserveCredit(UUID orionGuid) {
        return Sql.with(dataSource).exec("SELECT * FROM get_and_reserve_vm_credit_for_provision(?)",
                Sql.nextOrNull(this::mapVirtualMachineCredit), orionGuid);
    }
}
