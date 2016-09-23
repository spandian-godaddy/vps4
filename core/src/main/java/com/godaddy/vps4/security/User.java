package com.godaddy.vps4.security;

public class User {

    private final String name;
    private final long id;

	private final String shopperId;

	public User(String name, long id, String shopperId) {
		this.id = id;
		this.shopperId = shopperId;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public long getId() {
		return id;
	}

	public String getShopperId() {
		return shopperId;
	}

    @Override
    public String toString() {
        return "User [id=" + id + ", shopperId=" + shopperId + "]";
    }
    
}
