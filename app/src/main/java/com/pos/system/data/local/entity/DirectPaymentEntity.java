package com.pos.system.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * مبالغ بدون فواتير — تُضاف مباشرة على حساب العميل أو المورد
 * مثل: مديونيات سابقة، مدفوعات نقدية مباشرة، تسويات
 */
@Entity(tableName = "direct_payments")
public class DirectPaymentEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** "customer" أو "supplier" */
    @ColumnInfo(name = "entity_type")
    public String entityType;

    @ColumnInfo(name = "entity_id", defaultValue = "0")
    public long entityId;

    @ColumnInfo(name = "entity_name")
    public String entityName;

    /** "IN" = مبلغ له (دائن)، "OUT" = مبلغ عليه (مدين) */
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
