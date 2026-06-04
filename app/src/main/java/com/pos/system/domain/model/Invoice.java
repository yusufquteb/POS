package com.pos.system.domain.model;

import java.util.List;

public class Invoice {
    public long id;
    public String invoiceNumber;
    public long customerId;
    public String customerName;
    public double subtotal;
    public double discount;
    public double tax;
    public double total;
    public String paymentMethod;
    public String status;
    public String notes;
    public String createdBy;
    public String createdAt;

    public List<InvoiceItem> items;

    public Invoice() {}

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isReturned() {
        return "returned".equals(status);
    }
}
