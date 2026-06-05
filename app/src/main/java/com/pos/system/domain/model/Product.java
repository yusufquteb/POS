package com.pos.system.domain.model;

public class Product {
    public long id;
    public String barcode;
    public String name;
    public String brand;
    public String unit;
    public double cost;
    public double price;
    public int qty;
    public String location;
    public String supplier;
    public String expiry;
    public String imagePath;
    public int reorderLevel;
    public String category;
    public String notes;
    public String batchNumber;
    public String supplierReference;
    public String createdAt;

    public Product() {}

    public Product(String barcode, String name, double price, int qty) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.qty = qty;
    }

    public boolean isLowStock() {
        return qty <= reorderLevel;
    }

    public boolean isExpired() {
        return expiry != null && !expiry.isEmpty();
    }
}
