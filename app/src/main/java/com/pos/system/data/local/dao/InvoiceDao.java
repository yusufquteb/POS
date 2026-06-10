package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.InvoiceEntity;
import com.pos.system.data.local.entity.InvoiceItemEntity;

import java.util.List;

@Dao
public interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY created_at DESC")
    List<InvoiceEntity> getAll();

    @Query("SELECT * FROM invoices WHERE status != 'returned' ORDER BY created_at DESC")
    List<InvoiceEntity> getAllExcludeReturns();

    @Query("SELECT * FROM invoices WHERE remaining_amount > 0 AND status='completed' ORDER BY created_at DESC")
    List<InvoiceEntity> getUnsettled();

    @Query("SELECT * FROM invoices WHERE customer_id=:customerId ORDER BY created_at DESC")
    List<InvoiceEntity> getByCustomer(long customerId);

    @Query("SELECT * FROM invoices WHERE supplier_id=:supplierId ORDER BY created_at DESC")
    List<InvoiceEntity> getBySupplier(long supplierId);

    @Query("SELECT * FROM invoices WHERE created_at BETWEEN :from AND :to ORDER BY created_at DESC")
    List<InvoiceEntity> getByDateRange(String from, String to);

    @Query("SELECT * FROM invoices WHERE created_at BETWEEN :from AND :to AND status != 'returned' ORDER BY created_at DESC")
    List<InvoiceEntity> getByDateRangeExcludeReturns(String from, String to);

    @Query("SELECT * FROM invoices WHERE id=:id")
    InvoiceEntity getById(long id);

    @Query("SELECT * FROM invoices WHERE invoice_number=:num")
    InvoiceEntity getByNumber(String num);

    @Query("SELECT * FROM invoice_items WHERE invoice_id=:invoiceId")
    List<InvoiceItemEntity> getItems(long invoiceId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertInvoice(InvoiceEntity invoice);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertItems(List<InvoiceItemEntity> items);

    @Update
    int updateInvoice(InvoiceEntity invoice);

    @Query("UPDATE invoices SET status=:status WHERE id=:id")
    int updateStatus(long id, String status);

    @Query("UPDATE invoices SET paid_amount=:paid, remaining_amount=:remaining WHERE id=:id")
    int updatePayment(long id, double paid, double remaining);

    @Query("DELETE FROM invoices WHERE id=:id")
    int deleteInvoice(long id);

    @Query("DELETE FROM invoice_items WHERE invoice_id=:invoiceId")
    int deleteItems(long invoiceId);

    // ── إحصائيات ──────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(total),0) FROM invoices WHERE status='completed' AND created_at BETWEEN :from AND :to")
    double getTotalSales(String from, String to);

    @Query("SELECT COUNT(*) FROM invoices WHERE status='completed' AND created_at BETWEEN :from AND :to")
    int getInvoiceCount(String from, String to);

    @Query("SELECT COALESCE(SUM(total),0) FROM invoices WHERE status='completed'")
    double getTotalSalesAllTime();

    @Query("SELECT COALESCE(SUM(remaining_amount),0) FROM invoices WHERE status='completed' AND customer_id=:customerId")
    double getRemainingForCustomer(long customerId);

    @Query("SELECT COALESCE(SUM(remaining_amount),0) FROM invoices WHERE status='completed' AND supplier_id=:supplierId")
    double getRemainingForSupplier(long supplierId);

    // ── تقرير الأرباح (بيع - تكلفة) ──────────────────────

    @Query("SELECT COALESCE(SUM(ii.total - ii.qty * p.cost),0) " +
           "FROM invoice_items ii " +
           "JOIN invoices i ON ii.invoice_id = i.id " +
           "JOIN products p ON ii.product_id = p.id " +
           "WHERE i.status='completed' AND i.created_at BETWEEN :from AND :to")
    double getTotalProfit(String from, String to);

    @Query("SELECT COALESCE(SUM(ii.total - ii.qty * p.cost),0) " +
           "FROM invoice_items ii " +
           "JOIN invoices i ON ii.invoice_id = i.id " +
           "JOIN products p ON ii.product_id = p.id " +
           "WHERE i.status='completed'")
    double getTotalProfitAllTime();

    // ── تقرير مبيعات حسب التصنيف ──────────────────────────

    @Query("SELECT p.category, COALESCE(SUM(ii.total),0) as total_sales " +
           "FROM invoice_items ii " +
           "JOIN invoices i ON ii.invoice_id = i.id " +
           "JOIN products p ON ii.product_id = p.id " +
           "WHERE i.status='completed' AND i.created_at BETWEEN :from AND :to " +
           "GROUP BY p.category ORDER BY total_sales DESC")
    List<CategorySalesRow> getSalesByCategory(String from, String to);

    class CategorySalesRow {
        public String category;
        public double total_sales;
    }

    // ── تقرير المنتجات المباعة لعميل محدد ─────────────────

    @Query("SELECT ii.name, COALESCE(SUM(ii.qty),0) as total_qty, COALESCE(SUM(ii.total),0) as total_amount " +
           "FROM invoice_items ii " +
           "JOIN invoices i ON ii.invoice_id = i.id " +
           "WHERE i.customer_id=:customerId AND i.status='completed' " +
           "GROUP BY ii.name ORDER BY total_amount DESC")
    List<ProductSalesRow> getProductsSoldToCustomer(long customerId);

    class ProductSalesRow {
        public String name;
        public int total_qty;
        public double total_amount;
    }
}
