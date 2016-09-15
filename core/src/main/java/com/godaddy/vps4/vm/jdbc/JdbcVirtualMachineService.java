package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.ConnectionProvider;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.VirtualMachine;
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
    	
        return Sql.with(dataSource).exec("SELECT * FROM virtual_machine_spec "
                + " WHERE spec_name=? ",
                Sql.nextOrNull(this::mapVirtualMachineSpec),
                name);
    }

    @Override
    public List<VirtualMachine> listVirtualMachines(long sgid) {
        return Sql.with(dataSource).exec("SELECT * FROM virtual_machine vm "
                + " JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id "
                + " WHERE sgid=? ",
                Sql.listOf(this::mapVirtualMachine),
                sgid);
    }

    public VirtualMachine getVirtualMachine(long vmId) {
        return Sql.with(dataSource).exec("SELECT * FROM virtual_machine vm "
                + " JOIN virtual_machine_spec vms ON vms.spec_id=vm.spec_id "
                + " WHERE vm_id=? ",
                Sql.nextOrNull(this::mapVirtualMachine),
                vmId);
    }

    protected VirtualMachine mapVirtualMachine(ResultSet rs) throws SQLException {

        VirtualMachineSpec spec = mapVirtualMachineSpec(rs);

        return new VirtualMachine(rs.getLong("vm_id"), 
        		java.util.UUID.fromString(rs.getString("orion_guid")), 
        		rs.getLong("project_id"), 
        		spec, 
        		rs.getString("name"),
        		rs.getInt("control_panel_id"),
        		rs.getInt("os_type_id"));
    }

    protected VirtualMachineSpec mapVirtualMachineSpec(ResultSet rs) throws SQLException {
        Timestamp validUntil = rs.getTimestamp("valid_until");

        return new VirtualMachineSpec(rs.getInt("spec_id"),
                rs.getString("spec_name"),
                rs.getInt("tier"),
                rs.getInt("cpu_core_count"),
                rs.getInt("memory_mib"),
                rs.getInt("disk_gib"),
                rs.getTimestamp("valid_on").toInstant(),
                validUntil != null ? validUntil.toInstant() : null );
    }

    @Override
    public VirtualMachine createVirtualMachine(long vmId, long sgid, String spec, String name) {

        Sql.with(dataSource).exec("SELECT * FROM virtual_machine_create(?,?,?,?)", null, vmId, sgid, spec, name);

        return null;
    }

    @Override
    public void destroyVirtualMachine(long vmId) {

        // FIXME this should be setting the 'valid_until' instead of deleting the record
        //       (the direct deletion was just until active/destroyed filtering is put in place)
        //       we can't properly bill for virtual machines if we don't have soft deletes

//        Sql.with(dataSource).exec("UPDATE virtual_machine vm "
//                + " SET valid_until=NOW() "
//                + " WHERE vm_id=? AND valid_until IS NULL ",
//                null,
//                vmId);

        // ^^ the assumption is we are calling into the vertical service
        //    to get historical data of active virtual machines

        Sql.with(dataSource).exec("DELETE FROM virtual_machine vm WHERE vm_id=? ",
                null,
                vmId);
    }

	@Override
	public VirtualMachineSpec getSpec(int tier) {
        return Sql.with(dataSource).exec("SELECT * FROM virtual_machine_spec "
                + " WHERE tier=? ",
                Sql.nextOrNull(this::mapVirtualMachineSpec),
                tier);
	}

	@Override
	public void createVirtualMachine(UUID orionGuid,
			long projectId,
			int osTypeId, 
			int controlPanelId, 
			int specId,
			int managedLevel) {
		Sql.with(dataSource).exec("SELECT * FROM virtual_machine_create(?,?,?,?,?,?)", 
				null, 
				orionGuid, 
				projectId,
				osTypeId, 
				controlPanelId, 
				specId, 
				managedLevel);
	}


}
