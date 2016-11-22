package com.godaddy.vps4.security;

public interface Vps4UserService {

    Vps4User getUser(String shopperId);

    Vps4User getUser(long userId);

    Vps4User getOrCreateUserForShopper(String shopperId);

}