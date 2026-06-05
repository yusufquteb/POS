package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "invoices",
    indices = {@Index(value = "invoice_number", unique = true)}
)
public class InvoiceEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "invoice_number")
    public String invoiceNumber;

    @ColumnInfo(name = "customer_id", defaultValue = "0")
    public long customerId;

    @ColumnInfo(name = "customer_name")
    public String customerName;

    @ColumnInfo(name = "subtotal", defaultValue = "0.0")
    public double subtotal;

    @ColumnInfo(name = "discount", defaultValue = "0.0")
    public double discount;

    @ColumnInfo(name = "tax", defaultValue = "0.0")
    public double tax;

    @ColumnInfo(name = "total", defaultValue = "0.0")
    public double total;

    @ColumnInfo(name = "payment_method", defaultValue = "نقدي")
    public String paymentMethod = "نقدي";

    @ColumnInfo(name = "status", defaultValue = "completed")
    public String status = "completed";

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_by", defaultValue = "admin")
    public String createdBy = "admin";

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
