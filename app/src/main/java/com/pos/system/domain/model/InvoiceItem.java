package com.pos.system.domain.model;

public class InvoiceItem {
    public long id;
    public long invoiceId;
    public long productId;
    public String barcode;
    public String name;
    public double price;
    public int qty;
    public double total;

    public InvoiceItem() {}

    public InvoiceItem(String barcode, String name, double price, int qty) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.qty = qty;
        this.total = price * qty;
    }
}
