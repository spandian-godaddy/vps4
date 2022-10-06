package com.godaddy.vps4.customer;

import java.util.UUID;

public interface CustomerService {
    Customer getCustomer(UUID customerId);
}
