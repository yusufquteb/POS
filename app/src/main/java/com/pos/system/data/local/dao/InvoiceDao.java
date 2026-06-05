package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.pos.system.data.local.entity.InvoiceEntity;
import com.pos.system.data.local.entity.InvoiceItemEntity;

import java.util.List;

@Dao
public interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY created_at DESC")
    List<InvoiceEntity> getAll();

    @Query("SELECT * FROM invoices WHERE created_at BETWEEN :from AND :to ORDER BY created_at DESC")
    List<InvoiceEntity> getByDateRange(String from, String to);

    @Query("SELECT * FROM invoices WHERE customer_id = :customerId ORDER BY created_at DESC")
    List<InvoiceEntity> getByCustomer(long customerId);

    @Query("SELECT * FROM invoices WHERE id = :id")
    InvoiceEntity getById(long id);

    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    List<InvoiceItemEntity> getItems(long invoiceId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertInvoice(InvoiceEntity invoice);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertItems(List<InvoiceItemEntity> items);

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    int updateStatus(long id, String status);

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM invoices WHERE status = 'completed' AND created_at BETWEEN :from AND :to")
    double getTotalSales(String from, String to);

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'completed' AND created_at BETWEEN :from AND :to")
    int getInvoiceCount(String from, String to);
}
