package com.godaddy.vps4.vm.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.credit.CreditHistory;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.jdbc.IpAddressMapper;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
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

public class JdbcVirtualMachineService implements VirtualMachineService {

    private final DataSource dataSource;

    private String selectVirtualMachineQuery = "SELECT DISTINCT vm.vm_id, vm.hfs_vm_id, vm.orion_guid, vm.project_id, vm.name as \"vm_name\", "
            + "vm.hostname, vm.account_status_id, vm.backup_job_id, vm.valid_on as \"vm_valid_on\", vm.canceled as \"vm_canceled\", vm.valid_until as \"vm_valid_until\", vm.nydus_warning_ack, vm.managed_level, "
            + "vms.spec_id, vms.spec_name, vms.tier, vms.cpu_core_count, vms.memory_mib, vms.disk_gib, vms.valid_on as \"spec_valid_on\", "
            + "vms.valid_until as \"spec_valid_until\", vms.name as \"spec_vps4_name\", vms.ip_address_count, st.server_type, st.server_type_id, st.platform, "
            + "image.name, image.hfs_name, image.image_id, image.control_panel_id, image.os_type_id, "
            + "primaryIp.address_id, primaryIp.hfs_address_id, primaryIp.ip_address, primaryIp.ip_address_type_id, primaryIp.valid_on, primaryIp.valid_until, primaryIp.ping_check_id, family(primaryIp.ip_address), "
            + "dc.data_center_id, dc.description "
            + "FROM virtual_machine vm "
            + "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id "
            + "JOIN image ON image.image_id=vm.image_id "
            + "JOIN project prj ON prj.project_id=vm.project_id "
            + "JOIN server_type st ON st.server_type_id = vms.server_type_id "
            + "LEFT JOIN data_center dc ON dc.data_center_id = vm.data_center_id "
            + "LEFT JOIN ip_address primaryIp ON primaryIp.vm_id = vm.vm_id AND primaryIp.ip_address_type_id = 1 "
            + "LEFT JOIN ip_address allIps ON allIps.vm_id = vm.vm_id ";

    @Inject
    public JdbcVirtualMachineService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ServerSpec getSpec(String name) {
        return Sql.with(dataSource).exec(
                "SELECT spec_id, name as spec_vps4_name, spec_name, tier, cpu_core_count, memory_mib, disk_gib, " +
                        "valid_on as spec_valid_on, valid_until as spec_valid_until, ip_address_count, " +
                        "st.server_type_id, st.server_type, st.platform " +
                "FROM virtual_machine_spec " +
                "JOIN server_type st USING(server_type_id)" +
                "WHERE spec_name=? ",
                Sql.nextOrNull(this::mapServerSpec), name);
    }

    @Override
    public ServerSpec getSpec(int tier, int serverTypeId) {
        return Sql.with(dataSource).exec(
                "SELECT spec_id, name as spec_vps4_name, spec_name, tier, cpu_core_count, memory_mib, disk_gib, " +
                        "valid_on as spec_valid_on, valid_until as spec_valid_until, ip_address_count, " +
                        "st.server_type_id, st.server_type, st.platform " +
                "FROM virtual_machine_spec " +
                "JOIN server_type st USING(server_type_id)" +
                "WHERE valid_until > now_utc() AND tier=? AND server_type_id=?",
                Sql.nextOrNull(this::mapServerSpec), tier, serverTypeId);
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

    @Override
    public VirtualMachine getVirtualMachineByCheckId(long nodePingCheckId) {
        return Sql.with(dataSource).exec(selectVirtualMachineQuery + "WHERE ip.ping_check_id=?",
                Sql.nextOrNull(this::mapVirtualMachine), nodePingCheckId);
    }

    @Override
    public List<CreditHistory> getCreditHistory(UUID orionGuid) {
        return Sql.with(dataSource).exec("SELECT DISTINCT vm_id, valid_on, valid_until FROM virtual_machine WHERE orion_guid = ? ORDER BY valid_until DESC",
                Sql.listOf(this::mapCreditHistory), orionGuid);
    }

    protected VirtualMachine mapVirtualMachine(ResultSet rs) throws SQLException {
        ServerSpec spec = mapServerSpec(rs);
        UUID vmId = UUID.fromString(rs.getString("vm_id"));
        IpAddress ipAddress = mapIpAddress(rs);
        Image image = mapImage(rs);
        String backupJobId = rs.getString("backup_job_id");
        DataCenter dataCenter = DataCenterMapper.mapDataCenter(rs);

        return new VirtualMachine(vmId, rs.getLong("hfs_vm_id"),
                                  UUID.fromString(rs.getString("orion_guid")),
                                  rs.getLong("project_id"),
                                  spec,
                                  rs.getString("vm_name"),
                                  image,
                                  ipAddress,
                                  rs.getTimestamp("vm_valid_on", TimestampUtils.utcCalendar).toInstant(),
                                  rs.getTimestamp("vm_canceled", TimestampUtils.utcCalendar).toInstant(),
                                  rs.getTimestamp("vm_valid_until", TimestampUtils.utcCalendar).toInstant(),
                                  rs.getTimestamp("nydus_warning_ack", TimestampUtils.utcCalendar).toInstant(),
                                  rs.getString("hostname"),
                                  rs.getInt("managed_level"),
                                  backupJobId != null ? java.util.UUID.fromString(backupJobId) : null,
                                  dataCenter);
    }

    protected IpAddress mapIpAddress(ResultSet rs) throws SQLException {
        String ip = rs.getString("ip_address");
        if (StringUtils.isEmpty(ip)){
            return null;
        }

        return IpAddressMapper.mapIpAddress(rs);
    }

    private Image mapImage(ResultSet rs) throws SQLException {
        ServerType serverType = mapServerType(rs);
        Image image = new Image();

        image.imageName = rs.getString("name");
        image.hfsName = rs.getString("hfs_name");
        image.imageId = rs.getLong("image_id");
        image.controlPanel = ControlPanel.valueOf(rs.getInt("control_panel_id"));
        image.operatingSystem = OperatingSystem.valueOf(rs.getInt("os_type_id"));
        image.serverType = serverType;

        return image;
    }

    protected ServerSpec mapServerSpec(ResultSet rs) throws SQLException {
        ServerType serverType = mapServerType(rs);
        Timestamp validUntil = rs.getTimestamp("spec_valid_until", TimestampUtils.utcCalendar);

        return new ServerSpec(rs.getInt("spec_id"), rs.getString("spec_vps4_name"), rs.getString("spec_name"),
                rs.getInt("tier"), rs.getInt("cpu_core_count"), rs.getInt("memory_mib"), rs.getInt("disk_gib"),
                rs.getTimestamp("spec_valid_on", TimestampUtils.utcCalendar).toInstant(),
                validUntil != null ? validUntil.toInstant() : null, serverType, rs.getInt("ip_address_count"));
    }

    protected  ServerType mapServerType(ResultSet rs) throws SQLException {
        ServerType serverType = new ServerType();
        serverType.serverTypeId = rs.getInt("server_type_id");
        serverType.serverType = ServerType.Type.valueOf(rs.getString("server_type"));
        serverType.platform = ServerType.Platform.valueOf(rs.getString("platform"));
        return serverType;
    }

    protected CreditHistory mapCreditHistory(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("valid_until", TimestampUtils.utcCalendar);
        return new CreditHistory(UUID.fromString(rs.getString("vm_id")), rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant(),
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
        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_provision(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                null,
                vmId,
                vmProvisionParameters.getVps4UserId(),
                vmProvisionParameters.getSgidPrefix(),
                vmProvisionParameters.getOrionGuid(),
                vmProvisionParameters.getName(),
                vmProvisionParameters.getTier(),
                vmProvisionParameters.getManagedLevel(),
                vmProvisionParameters.getImageHfsName(),
                vmProvisionParameters.getDataCenterId());
        return getVirtualMachine(vmId);
    }

    @Override
    public VirtualMachine importVirtualMachine(ImportVirtualMachineParameters importVirtualMachineParameters) {
        UUID vmId = UUID.randomUUID();
        Sql.with(dataSource).exec("INSERT INTO virtual_machine (vm_id, hfs_vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id, data_center_id)" +
                                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                  null,
                                  vmId,
                                  importVirtualMachineParameters.hfsVmId,
                                  importVirtualMachineParameters.orionGuid,
                                  importVirtualMachineParameters.name,
                                  importVirtualMachineParameters.projectId,
                                  importVirtualMachineParameters.specId,
                                  0,
                                  importVirtualMachineParameters.imageId,
                                  importVirtualMachineParameters.dataCenterId);
        Sql.with(dataSource).exec("INSERT INTO imported_vm (vm_id) VALUES (?)", null, vmId);
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
            queryAddition.append("JOIN vps4_user on vps4_user.vps4_user_id = prj.vps4_user_id ");
        }
        queryAddition.append("WHERE prj.valid_until = 'infinity'");
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
            queryAddition.append(" AND allIps.ip_address=?::inet AND allIps.valid_until = 'infinity' ");
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
        String query = selectVirtualMachineQuery + queryAddition;
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
                + "JOIN project prj ON v.project_id = prj.project_id "
                + "WHERE v.vm_id=?;", Sql.nextOrNull(rs -> rs.getLong("vps4_user_id")), vmId);
    }

    @Override
    public UUID getOrionGuidByVmId(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT orion_guid FROM virtual_machine WHERE vm_id=?;",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("orion_guid"))), vmId);
    }

    @Override
    public long getHfsVmIdByVmId(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT hfs_vm_id FROM virtual_machine WHERE vm_id=?;",
                Sql.nextOrNull(rs -> rs.getLong("hfs_vm_id")), vmId);
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

        String pattern = "(?:hfs-|vps4-)?(?<osdistro>\\w+-\\w+)(?:-\\w+-\\w+)?";
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
        return vm.image.hasPaidControlPanel();
    }

    @Override
    public void setBackupJobId(UUID vmId, UUID backupJobId) {
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("backup_job_id", backupJobId);
        updateVirtualMachine(vmId, vmPatchMap);
    }

    @Override
    public Map<Integer, Integer> getActiveServerCountByTiers(){
        return Sql.with(dataSource).exec(
                "SELECT count(vm.vm_id), vms.tier " +
                        "FROM virtual_machine vm " +
                        "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id " +
                        "WHERE vm.canceled = 'infinity' AND vm.valid_until = 'infinity' " +
                        "GROUP BY vms.tier; ",
                Sql.mapOf(this::mapCount, this::mapTier));
    }

    @Override
    public Map<Integer, Integer> getZombieServerCountByTiers(){
        return Sql.with(dataSource).exec(
                "SELECT count(vm.vm_id), vms.tier " +
                        "FROM virtual_machine vm " +
                        "JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id " +
                        "WHERE vm.canceled < 'infinity' AND vm.valid_until = 'infinity' " +
                        "GROUP BY vms.tier; ",
                Sql.mapOf(this::mapCount, this::mapTier));
    }

    @Override
    public UUID getImportedVm(UUID vmId) {
        String stringVmId = Sql.with(dataSource).exec("SELECT vm_id from imported_vm where vm_id = ?",
                                         Sql.nextOrNull(rs -> rs.getString("vm_id")), vmId);
        if (stringVmId == null) return null;
        return UUID.fromString(stringVmId);
    }

    protected Integer mapTier(ResultSet rs) throws SQLException {
        return rs.getInt("tier");
    }

    protected Integer mapCount(ResultSet rs) throws SQLException {
        return rs.getInt("count");
    }

    @Override
    public void ackNydusWarning(UUID vmId) {
        Sql.with(dataSource).exec("UPDATE virtual_machine vm SET nydus_warning_ack=now_utc() WHERE vm_id=?", null, vmId);
    }

    @Override
    public boolean getMonitoringPlanFeature (UUID vmId){
        return Sql.with(dataSource).exec("SELECT monitoring FROM monitoring_pf WHERE vm_id = ?;",
                                         Sql.nextOrNull(rs -> rs.getBoolean("monitoring")), vmId);
    }

    @Override
    public void setMonitoringPlanFeature (UUID vmId, boolean monitoring) {
        Sql.with(dataSource).exec("INSERT INTO monitoring_pf (vm_id, monitoring) VALUES (?, ?) " +
                                          " ON CONFLICT (vm_id) DO UPDATE SET monitoring = ? ",
                                  null, vmId, monitoring, monitoring);
    }
}
