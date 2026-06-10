package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "price_quote_items",
    indices = {@Index("quote_id")},
    foreignKeys = @ForeignKey(
        entity = PriceQuoteEntity.class,
        parentColumns = "id",
        childColumns = "quote_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class PriceQuoteItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "quote_id")
    public long quoteId;

    @ColumnInfo(name = "product_id", defaultValue = "0")
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
