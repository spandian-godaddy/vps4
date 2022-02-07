package com.godaddy.vps4.security;

import java.util.UUID;

public interface Vps4UserService {

    Vps4User getUser(String shopperId);

    Vps4User getUser(long userId);

    Vps4User getUser(UUID customerId);

    Vps4User getOrCreateUserForShopper(String shopperId, String resellerId, UUID customerId);
}
