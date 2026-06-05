package com.pos.system.domain.model;

public class Expense {
    public long id;
    public String title;
    public double amount;
    public String category;
    public String note;
    public String date;
    public String createdAt;

    public Expense() {}

    public Expense(String title, double amount, String category, String date) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }
}
