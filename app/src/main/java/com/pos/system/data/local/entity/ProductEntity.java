package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "products",
    indices = {@Index(value = "barcode", unique = true)}
)
public class ProductEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "barcode")
    public String barcode;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "brand")
    public String brand;

    @ColumnInfo(name = "unit")
    public String unit;

    @ColumnInfo(name = "cost", defaultValue = "0.0")
    public double cost;

    @ColumnInfo(name = "price", defaultValue = "0.0")
    public double price;

    @ColumnInfo(name = "qty", defaultValue = "0")
    public int qty;

    @ColumnInfo(name = "location")
    public String location;

    @ColumnInfo(name = "supplier")
    public String supplier;

    @ColumnInfo(name = "expiry")
    public String expiry;

    @ColumnInfo(name = "image_path")
    public String imagePath;

    @ColumnInfo(name = "reorder_level", defaultValue = "5")
    public int reorderLevel = 5;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "batch_number", defaultValue = "''")
    public String batchNumber = "";

    @ColumnInfo(name = "supplier_reference", defaultValue = "''")
    public String supplierReference = "";

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
