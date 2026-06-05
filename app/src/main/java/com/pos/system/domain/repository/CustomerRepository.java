package com.pos.system.domain.repository;

import com.pos.system.domain.model.Customer;
import java.util.List;

public interface CustomerRepository {
    List<Customer> getAllCustomers();
    List<Customer> searchCustomers(String query);
    Customer getCustomerById(long id);
    long insertCustomer(Customer customer);
    int updateCustomer(Customer customer);
    int deleteCustomer(long id);
    boolean phoneExists(String phone);
    int updateDebt(long customerId, double debtDelta);
}
