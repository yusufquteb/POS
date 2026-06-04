package com.pos.system.domain.model;

public class Shift {
    public long id;
    public String openedAt;
    public String closedAt;
    public double openingCash;
    public double closingCash;
    public double totalSales;
    public String notes;
    public String status;

    public Shift() {}

    public boolean isOpen() {
        return "open".equals(status);
    }
}
