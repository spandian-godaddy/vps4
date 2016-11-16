package com.godaddy.vps4.security;

public class Vps4User {

    private final String username;
    private final long id;

    private final String shopperId;

    public Vps4User(String username, long id, String shopperId) {
        this.id = id;
        this.shopperId = shopperId;
        this.username = username;
    }

    public String getUserame() {
        return username;
    }

    public long getId() {
        return id;
    }

    public String getShopperId() {
        return shopperId;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", shopperId=" + shopperId + "username=" + username + "]";
    }

}
