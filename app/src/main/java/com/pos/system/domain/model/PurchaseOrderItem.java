package com.pos.system.domain.model;

public class PurchaseOrderItem {
    public long id;
    public long orderId;
    public long productId;
    public String productName;
    public String barcode;
    public double unitCost;
    public int qty;
    public double total;

    public PurchaseOrderItem() {}
}
