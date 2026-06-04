package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shifts")
public class ShiftEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "opened_at")
    public String openedAt;

    @ColumnInfo(name = "closed_at")
    public String closedAt;

    @ColumnInfo(name = "opening_cash", defaultValue = "0.0")
    public double openingCash;

    @ColumnInfo(name = "closing_cash", defaultValue = "0.0")
    public double closingCash;

    @ColumnInfo(name = "total_sales", defaultValue = "0.0")
    public double totalSales;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "status", defaultValue = "open")
    public String status = "open";
}
