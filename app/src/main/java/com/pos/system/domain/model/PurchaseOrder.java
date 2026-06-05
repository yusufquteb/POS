package com.pos.system.domain.model;

import java.util.List;

public class PurchaseOrder {
    public long id;
    public String orderNumber;
    public long supplierId;
    public String supplierName;
    public double total;
    public String status;
    public String notes;
    public String createdAt;

    public List<PurchaseOrderItem> items;

    public PurchaseOrder() {}

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isReceived() {
        return "received".equals(status);
    }
}
