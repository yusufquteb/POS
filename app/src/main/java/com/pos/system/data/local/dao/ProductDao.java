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

    @Query("SELECT * FROM products WHERE id=:id")
    ProductEntity getById(long id);

    @Query("SELECT * FROM products WHERE barcode=:barcode LIMIT 1")
    ProductEntity getByBarcode(String barcode);

    @Query("SELECT COUNT(*) FROM products WHERE barcode=:barcode")
    int barcodeExists(String barcode);

    @Query("SELECT COUNT(*) FROM products")
    int getTotalCount();

    @Query("SELECT COALESCE(SUM(price * qty),0) FROM products")
    double getTotalInventoryValue();

    @Query("SELECT COALESCE(SUM(cost * qty),0) FROM products")
    double getTotalInventoryCost();

    @Query("SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND category != '' ORDER BY category ASC")
    List<String> getAllCategories();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrReplace(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(ProductEntity product);

    @Update
    int update(ProductEntity product);

    @Query("DELETE FROM products WHERE id=:id")
    int delete(long id);

    @Query("DELETE FROM products")
    int deleteAll();

    @Query("UPDATE products SET qty = qty - :amount WHERE barcode=:barcode AND qty >= :amount")
    int decreaseQty(String barcode, int amount);

    @Query("UPDATE products SET qty = qty + :amount WHERE barcode=:barcode")
    int increaseQty(String barcode, int amount);

    @Query("UPDATE products SET qty = qty + :amount WHERE id=:id")
    int increaseQtyById(long id, int amount);

    // ── تعديل أسعار جماعي ──────────────────────────────────────

    @Query("UPDATE products SET price = price + :delta WHERE (:category = '' OR category = :category)")
    int increaseSellPriceByAmount(double delta, String category);

    @Query("UPDATE products SET price = MAX(0, price - :delta) WHERE (:category = '' OR category = :category)")
    int decreaseSellPriceByAmount(double delta, String category);

    @Query("UPDATE products SET price = price * (1 + :pct/100.0) WHERE (:category = '' OR category = :category)")
    int increaseSellPriceByPercent(double pct, String category);

    @Query("UPDATE products SET price = MAX(0, price * (1 - :pct/100.0)) WHERE (:category = '' OR category = :category)")
    int decreaseSellPriceByPercent(double pct, String category);

    @Query("UPDATE products SET cost = cost + :delta WHERE (:category = '' OR category = :category)")
    int increaseBuyCostByAmount(double delta, String category);

    @Query("UPDATE products SET cost = MAX(0, cost - :delta) WHERE (:category = '' OR category = :category)")
    int decreaseBuyCostByAmount(double delta, String category);

    @Query("UPDATE products SET cost = cost * (1 + :pct/100.0) WHERE (:category = '' OR category = :category)")
    int increaseBuyCostByPercent(double pct, String category);

    @Query("UPDATE products SET cost = MAX(0, cost * (1 - :pct/100.0)) WHERE (:category = '' OR category = :category)")
    int decreaseBuyCostByPercent(double pct, String category);

    @Query("UPDATE products SET price = :newPrice WHERE name = :productName")
    int updatePriceForAllWithName(String productName, double newPrice);
}
