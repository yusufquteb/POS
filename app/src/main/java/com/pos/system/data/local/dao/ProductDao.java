package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    List<ProductEntity> getAll();

    @Query("SELECT * FROM products WHERE name LIKE :q OR barcode LIKE :q OR category LIKE :q ORDER BY name ASC")
    List<ProductEntity> search(String q);

    @Query("SELECT * FROM products WHERE qty <= reorder_level ORDER BY qty ASC")
    List<ProductEntity> getLowStock();

    @Query("SELECT * FROM products WHERE qty <= :threshold ORDER BY qty ASC")
    List<ProductEntity> getLowStock(int threshold);

    @Query("SELECT * FROM products WHERE expiry IS NOT NULL AND expiry != '' ORDER BY expiry ASC")
    List<ProductEntity> getWithExpiry();

    @Query("SELECT * FROM products WHERE id = :id")
    ProductEntity getById(long id);

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getByBarcode(String barcode);

    @Query("SELECT COUNT(*) FROM products WHERE barcode = :barcode")
    int barcodeExists(String barcode);

    @Query("SELECT COUNT(*) FROM products")
    int getTotalCount();

    @Query("SELECT COALESCE(SUM(price * qty), 0.0) FROM products")
    double getTotalInventoryValue();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(ProductEntity product);

    @Update
    int update(ProductEntity product);

    @Query("DELETE FROM products WHERE id = :id")
    int delete(long id);

    @Query("UPDATE products SET qty = qty - :amount WHERE barcode = :barcode AND qty >= :amount")
    int decreaseQty(String barcode, int amount);

    @Query("UPDATE products SET qty = qty + :amount WHERE barcode = :barcode")
    int increaseQty(String barcode, int amount);
}
