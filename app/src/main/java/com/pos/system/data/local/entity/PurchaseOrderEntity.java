package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchase_orders")
public class PurchaseOrderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "order_number")
    public String orderNumber;

    @ColumnInfo(name = "supplier_id")
    public long supplierId;

    @ColumnInfo(name = "supplier_name")
    public String supplierName;

    @ColumnInfo(name = "total", defaultValue = "0.0")
    public double total;

    @ColumnInfo(name = "status", defaultValue = "pending")
    public String status = "pending";

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
