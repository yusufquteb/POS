package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class CustomerEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "debt", defaultValue = "0.0")
    public double debt;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "last_purchase_at", defaultValue = "''")
    public String lastPurchaseAt = "";

    @ColumnInfo(name = "total_spent", defaultValue = "0.0")
    public double totalSpent;

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
