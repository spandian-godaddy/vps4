package com.godaddy.vps4.vm;

import java.util.List;

public interface VmUserService {
	
	void createUser(String username, long vmId, boolean adminEnabled);
	void createUser(String username, long vmId);
	
	List<VmUser> listUsers(long vmId);
	
	void updateUserAdminAccess(String username, long vmId, boolean adminEnabled);
	
	boolean userExists(String username, long vmId);
}
