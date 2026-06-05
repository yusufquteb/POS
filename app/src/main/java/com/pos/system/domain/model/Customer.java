package com.pos.system.domain.model;

public class Customer {
    public long id;
    public String name;
    public String phone;
    public String email;
    public String address;
    public double debt;
    public String notes;
    public String lastPurchaseAt;
    public double totalSpent;
    public String createdAt;

    public Customer() {}

    public Customer(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public boolean hasDebt() {
        return debt > 0;
    }
}
