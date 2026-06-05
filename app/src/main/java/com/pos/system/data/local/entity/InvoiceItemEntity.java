package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "invoice_items",
    indices = {@Index("invoice_id")},
    foreignKeys = @ForeignKey(
        entity = InvoiceEntity.class,
        parentColumns = "id",
        childColumns = "invoice_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class InvoiceItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "invoice_id")
    public long invoiceId;

    @ColumnInfo(name = "product_id")
    public long productId;

    @ColumnInfo(name = "barcode")
    public String barcode;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "price", defaultValue = "0.0")
    public double price;

    @ColumnInfo(name = "qty", defaultValue = "1")
    public int qty = 1;

    @ColumnInfo(name = "total", defaultValue = "0.0")
    public double total;
}
