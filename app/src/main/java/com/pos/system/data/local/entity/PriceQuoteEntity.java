package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "price_quotes",
    indices = {@Index(value = "quote_number", unique = true)}
)
public class PriceQuoteEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "quote_number")
    public String quoteNumber;

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

    /** "pending" / "accepted" / "rejected" */
    @ColumnInfo(name = "status", defaultValue = "pending")
    public String status = "pending";

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
