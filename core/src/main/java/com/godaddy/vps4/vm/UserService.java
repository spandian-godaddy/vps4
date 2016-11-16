package com.godaddy.vps4.vm;

import java.util.List;

public interface UserService {
	
	void createUser(String username, long vmId, boolean adminEnabled);
	void createUser(String username, long vmId);
	
	List<User> listUsers(long vmId);
	
	void updateUserAdminAccess(String username, long vmId, boolean adminEnabled);
	
	boolean userExists(String username, long vmId);
}
