package com.godaddy.vps4.vm;

import java.util.List;
import java.util.UUID;

public interface VmUserService {
	
	void createUser(String username, UUID vmId, boolean adminEnabled);
	void createUser(String username, UUID vmId);
	
	List<VmUser> listUsers(UUID vmId);
	
	void updateUserAdminAccess(String username, UUID vmId, boolean adminEnabled);
	
	boolean userExists(String username, UUID vmId);
}
