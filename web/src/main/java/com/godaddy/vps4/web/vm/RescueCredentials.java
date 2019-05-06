package com.godaddy.vps4.web.vm;

public class RescueCredentials {
    private String username;
    private String password;

    public RescueCredentials(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
