package com.pos.system.domain.model;

public class Supplier {
    public long id;
    public String name;
    public String company;
    public String phone;
    public String address;
    public double debt;
    public String notes;
    public String createdAt;

    public Supplier() {}

    public Supplier(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public boolean hasDebt() {
        return debt > 0;
    }
}
