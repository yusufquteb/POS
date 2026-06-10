package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallet_transactions")
public class WalletTransactionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** "IN" = إيداع، "OUT" = سحب */
    @ColumnInfo(name = "type", defaultValue = "OUT")
    public String type = "OUT";

    @ColumnInfo(name = "amount", defaultValue = "0.0")
    public double amount;

    @ColumnInfo(name = "note")
    public String note;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
