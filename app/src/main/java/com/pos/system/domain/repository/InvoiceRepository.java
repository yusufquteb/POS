package com.pos.system.domain.repository;

import com.pos.system.domain.model.Invoice;
import com.pos.system.domain.model.InvoiceItem;
import java.util.List;

public interface InvoiceRepository {
    List<Invoice> getAllInvoices();
    List<Invoice> getInvoicesByDateRange(String fromDate, String toDate);
    List<Invoice> getInvoicesByCustomer(long customerId);
    Invoice getInvoiceById(long id);
    List<InvoiceItem> getInvoiceItems(long invoiceId);
    long createInvoice(Invoice invoice, List<InvoiceItem> items);
    int updateInvoiceStatus(long invoiceId, String status);
    double getTotalSalesForPeriod(String fromDate, String toDate);
}
