package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.jdbc.IpAddressMapper;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;
import com.godaddy.vps4.vm.VirtualMachineType;

public class JdbcVirtualMachineService implements VirtualMachineService {

    private final DataSource dataSource;

    private String selectVirtualMachineQuery = "SELECT vm.vm_id, vm.hfs_vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\", "
            + "vm.hostname, vm.account_status_id, vm.backup_job_id, vm.valid_on as \"vm_valid_on\", vm.canceled as \"vm_canceled\", vm.valid_until as \"vm_valid_until\", vm.managed_level, "
            + "vms.spec_id, vms.spec_name, vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
            + "vms.valid_until as \"spec_valid_until\", vms.name as \"spec_vps4_name\", "
            + "image.name, image.hfs_name, image.image_id, image.control_panel_id, image.os_type_id, "
            + "ip.ip_address_id, ip.ip_address, ip.ip_address_type_id, ip.valid_on, ip.valid_until, ip.ping_check_id "
            + "FROM virtual_machine vm "
            + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id "
            + "JOIN image ON image.image_id=vm.image_id "
            + "JOIN project prj ON prj.project_id=vm.project_id "
            + "LEFT JOIN ip_address ip ON ip.vm_id = vm.vm_id AND ip.ip_address_type_id = 1 ";

    @Inject
    public JdbcVirtualMachineService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public VirtualMachineSpec getSpec(String name) {

        return Sql.with(dataSource).exec(
                "SELECT spec_id, name as spec_vps4_name, spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as spec_valid_on, "
                        + "valid_until as spec_valid_until FROM virtual_machine_spec WHERE spec_name=? ",
                Sql.nextOrNull(this::mapVirtualMachineSpec), name);
    }

    @Override
    public VirtualMachineSpec getSpec(int tier) {
        return Sql.with(dataSource).exec(
                "SELECT spec_id, name as spec_vps4_name, spec_name, tier, cpu_core_count, memory_mib, disk_gib, valid_on as spec_valid_on, "
                        + "valid_until as spec_valid_until FROM virtual_machine_spec WHERE valid_until > now_utc() AND tier=? ",
                Sql.nextOrNull(this::mapVirtualMachineSpec), tier);
    }

    @Override
    public List<VirtualMachine> getVirtualMachinesForProject(long projectId) {
        return Sql.with(dataSource).exec(selectVirtualMachineQuery + "WHERE vm.project_id=?",
                Sql.listOf(this::mapVirtualMachine), projectId);
    }

    @Override
    public VirtualMachine getVirtualMachine(long hfsVmId) {
        return Sql.with(dataSource).exec(selectVirtualMachineQuery + "WHERE vm.hfs_vm_id=?",
                Sql.nextOrNull(this::mapVirtualMachine), hfsVmId);
    }

    @Override
    public VirtualMachine getVirtualMachine(UUID vmId) {
        return Sql.with(dataSource).exec(selectVirtualMachineQuery + "WHERE vm.vm_id=?",
                Sql.nextOrNull(this::mapVirtualMachine), vmId);
    }

    protected VirtualMachine mapVirtualMachine(ResultSet rs) throws SQLException {
        VirtualMachineSpec spec = mapVirtualMachineSpec(rs);
        UUID vmId = java.util.UUID.fromString(rs.getString("vm_id"));
        IpAddress ipAddress = mapIpAddress(rs);
        Image image = mapImage(rs);
        String backupJobId = rs.getString("backup_job_id");

        return new VirtualMachine(vmId, rs.getLong("hfs_vm_id"), java.util.UUID.fromString(rs.getString("orion_guid")),
                rs.getLong("project_id"), spec, rs.getString("vm_name"), image, ipAddress,
                rs.getTimestamp("vm_valid_on", TimestampUtils.utcCalendar).toInstant(),
                rs.getTimestamp("vm_canceled", TimestampUtils.utcCalendar).toInstant(),
                rs.getTimestamp("vm_valid_until", TimestampUtils.utcCalendar).toInstant(), rs.getString("hostname"),
                rs.getInt("managed_level"), backupJobId != null ? java.util.UUID.fromString(backupJobId) : null);
    }

    protected IpAddress mapIpAddress(ResultSet rs) throws SQLException {
        long ipAddressId = rs.getLong("ip_address_id");
        if (ipAddressId == 0) {
            return null;
        }
        return IpAddressMapper.mapIpAddress(rs);
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

    protected VirtualMachineSpec mapVirtualMachineSpec(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("spec_valid_until", TimestampUtils.utcCalendar);

        return new VirtualMachineSpec(rs.getInt("spec_id"), rs.getString("spec_vps4_name"), rs.getString("spec_name"),
                rs.getInt("tier"), rs.getInt("cpu_core_count"), rs.getInt("memory_mib"), rs.getInt("disk_gib"),
                rs.getTimestamp("spec_valid_on", TimestampUtils.utcCalendar).toInstant(),
                validUntil != null ? validUntil.toInstant() : null);
    }

    @Override
    public void setVmRemoved(UUID vmId) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET valid_until=now_utc() WHERE vm_id=?", null, vmId);
    }

    @Override
    public void setVmZombie(UUID vmId) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET canceled=now_utc() WHERE vm_id=?", null, vmId);
    }

    @Override
    public void reviveZombieVm(UUID vmId, UUID newOrionGuid) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET canceled = 'infinity', orion_guid = ? WHERE vm_id=?",
                null, newOrionGuid, vmId);
    }

    @Override
    public VirtualMachine provisionVirtualMachine(ProvisionVirtualMachineParameters vmProvisionParameters) {
        UUID vmId = UUID.randomUUID();
        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_provision(?, ?, ?, ?, ?, ?, ?, ?)",
                null,
                vmId,
                vmProvisionParameters.getVps4UserId(),
                vmProvisionParameters.getSgidPrefix(),
                vmProvisionParameters.getOrionGuid(),
                vmProvisionParameters.getName(),
                vmProvisionParameters.getTier(),
                vmProvisionParameters.getManagedLevel(),
                vmProvisionParameters.getImageHfsName());
        return getVirtualMachine(vmId);
    }

    @Override
    public void addHfsVmIdToVirtualMachine(UUID vmId, long hfsVmId) {
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("hfs_vm_id", hfsVmId);
        updateVirtualMachine(vmId, vmPatchMap);
    }

    @Override
    public void setHostname(UUID vmId, String hostname) {
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
    public List<VirtualMachine> getVirtualMachines(VirtualMachineType type, Long vps4UserId, String ipAddress,
            UUID orionGuid, Long hfsVmId) {
        List<Object> args = new ArrayList<>();
        StringBuilder queryAddition = new StringBuilder();
        if(vps4UserId != null){
            queryAddition.append("JOIN user_project_privilege upp on upp.project_id = prj.project_id "
            + "JOIN vps4_user on vps4_user.vps4_user_id = upp.vps4_user_id ");
        }
        queryAddition.append("WHERE 1=1");
        if(vps4UserId != null) {
            queryAddition.append(" AND vps4_user.vps4_user_id = ? ");
            args.add(vps4UserId);
        }
        if(type != null) {
            if(type == VirtualMachineType.ACTIVE) {
                queryAddition.append(" AND vm.canceled = 'infinity' AND vm.valid_until = 'infinity' ");
            }
            else if(type == VirtualMachineType.ZOMBIE) {
                queryAddition.append(" AND vm.canceled < 'infinity' AND vm.valid_until = 'infinity' ");
            }
        }
        if(ipAddress != null) {
            queryAddition.append(" AND ip.ip_address=?");
            args.add(ipAddress);
        }
        if(orionGuid != null) {
            queryAddition.append(" AND vm.orion_guid=?");
            args.add(orionGuid);
        }
        if(hfsVmId != null) {
            queryAddition.append(" AND vm.hfs_vm_id=?");
            args.add(hfsVmId);
        }
        String query = selectVirtualMachineQuery + queryAddition.toString();
        return Sql.with(dataSource)
                .exec(query, Sql.listOf(this::mapVirtualMachine), args.toArray());
    }

    private boolean virtualMachineHasControlPanel(UUID vmId, String controlPanel) {
        List<VirtualMachine> vms = Sql.with(dataSource)
                .exec(selectVirtualMachineQuery
                        + "JOIN control_panel ON image.control_panel_id = control_panel.control_panel_id "
                        + "where control_panel.name = ? " 
                        + "and vm.vm_id = ?;", Sql.listOf(this::mapVirtualMachine), controlPanel, vmId);
        return vms.size() > 0;
    }

    @Override
    public boolean virtualMachineHasCpanel(UUID vmId) {
        return virtualMachineHasControlPanel(vmId, "cpanel");
    }

    @Override
    public boolean virtualMachineHasPlesk(UUID vmId) {
        return virtualMachineHasControlPanel(vmId, "plesk");
    }

    @Override
    public long getUserIdByVmId(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id FROM virtual_machine v "
                + "JOIN user_project_privilege upp ON v.project_id = upp.project_id "
                + "WHERE v.vm_id=?;", Sql.nextOrNull(rs -> rs.getLong("vps4_user_id")), vmId);
    }

    @Override
    public UUID getOrionGuidByVmId(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT orion_guid FROM virtual_machine WHERE vm_id=?;",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("orion_guid"))), vmId);
    }

    @Override
    public Long getPendingSnapshotActionIdByVmId(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT sa.id FROM virtual_machine v"
                + " JOIN snapshot s ON v.vm_id = s.vm_id"
                + " JOIN snapshot_action sa ON s.id = sa.snapshot_id"
                + " JOIN action_status stat ON sa.status_id = stat.status_id"
                + " JOIN action_type a ON a.type_id = sa.action_type_id"
                + " WHERE a.type IN ('CREATE_SNAPSHOT')"
                + " AND stat.status IN ('NEW', 'IN_PROGRESS')"
                + " AND v.vm_id=?;",
                Sql.nextOrNull(rs -> rs.getLong("id")), vmId);
    }

    @Override
    public String getOSDistro(UUID vmId) {
        String hfsName = Sql.with(dataSource).exec("SELECT image.hfs_name FROM virtual_machine v "
                + "JOIN image ON image.image_id=v.image_id WHERE v.vm_id=?;",
                Sql.nextOrNull(rs -> rs.getString("hfs_name")), vmId);

        String pattern = "(?:hfs-)?(?<osdistro>\\w+-\\w+)(?:-\\w+-\\w+)?";
        Matcher m = Pattern.compile(pattern).matcher(hfsName);
        m.matches();
        return m.group("osdistro");
    }

    @Override
    public boolean isLinux(UUID vmId) {
        return getVirtualMachine(vmId).image.operatingSystem == Image.OperatingSystem.LINUX;
    }

    @Override
    public boolean hasControlPanel(UUID vmId) {
        VirtualMachine vm = getVirtualMachine(vmId);
        return vm.image.controlPanel == ControlPanel.CPANEL || vm.image.controlPanel == ControlPanel.PLESK;
    }

    @Override
    public void setBackupJobId(UUID vmId, UUID backupJobId) {
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("backup_job_id", backupJobId);
        updateVirtualMachine(vmId, vmPatchMap);
    }
}
