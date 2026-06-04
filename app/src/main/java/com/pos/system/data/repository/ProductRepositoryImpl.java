package com.pos.system.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pos.system.DBHelper;
import com.pos.system.domain.model.Product;
import com.pos.system.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductRepositoryImpl implements ProductRepository {

    private static final String TAG = "ProductRepo";
    private final DBHelper dbHelper;

    @Inject
    public ProductRepositoryImpl(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM products ORDER BY name ASC", null);
            while (cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllProducts error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return products;
    }

    @Override
    public List<Product> searchProducts(String query) {
        List<Product> products = new ArrayList<>();
        Cursor cursor = null;
        try {
            String like = "%" + query + "%";
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM products WHERE name LIKE ? OR barcode LIKE ? OR category LIKE ? ORDER BY name ASC",
                new String[]{like, like, like});
            while (cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "searchProducts error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return products;
    }

    @Override
    public List<Product> getLowStockProducts(int threshold) {
        List<Product> products = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM products WHERE qty <= ? ORDER BY qty ASC",
                new String[]{String.valueOf(threshold)});
            while (cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getLowStockProducts error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return products;
    }

    @Override
    public List<Product> getExpiringProducts(int daysAhead) {
        List<Product> products = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM products WHERE expiry IS NOT NULL AND expiry != '' ORDER BY expiry ASC",
                null);
            while (cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getExpiringProducts error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return products;
    }

    @Override
    public Product getProductById(long id) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM products WHERE id = ?", new String[]{String.valueOf(id)});
            if (cursor.moveToFirst()) {
                return cursorToProduct(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getProductById error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    @Override
    public Product getProductByBarcode(String barcode) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM products WHERE barcode = ? LIMIT 1",
                new String[]{barcode});
            if (cursor.moveToFirst()) {
                return cursorToProduct(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getProductByBarcode error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    @Override
    public long insertProduct(Product product) {
        try {
            ContentValues cv = productToContentValues(product);
            return dbHelper.getWritableDatabase().insertOrThrow("products", null, cv);
        } catch (Exception e) {
            Log.e(TAG, "insertProduct error", e);
            return -1;
        }
    }

    @Override
    public int updateProduct(Product product) {
        try {
            ContentValues cv = productToContentValues(product);
            return dbHelper.getWritableDatabase().update(
                "products", cv, "id = ?", new String[]{String.valueOf(product.id)});
        } catch (Exception e) {
            Log.e(TAG, "updateProduct error", e);
            return 0;
        }
    }

    @Override
    public int deleteProduct(long id) {
        try {
            return dbHelper.getWritableDatabase().delete(
                "products", "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e(TAG, "deleteProduct error", e);
            return 0;
        }
    }

    @Override
    public boolean barcodeExists(String barcode) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM products WHERE barcode = ?", new String[]{barcode});
            if (cursor.moveToFirst()) return cursor.getInt(0) > 0;
        } catch (Exception e) {
            Log.e(TAG, "barcodeExists error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    @Override
    public int getTotalCount() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM products", null);
            if (cursor.moveToFirst()) return cursor.getInt(0);
        } catch (Exception e) {
            Log.e(TAG, "getTotalCount error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    @Override
    public double getTotalInventoryValue() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(price * qty), 0.0) FROM products", null);
            if (cursor.moveToFirst()) return cursor.getDouble(0);
        } catch (Exception e) {
            Log.e(TAG, "getTotalInventoryValue error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0.0;
    }

    private Product cursorToProduct(Cursor c) {
        Product p = new Product();
        p.id              = getLong(c, "id");
        p.barcode         = getString(c, "barcode");
        p.name            = getString(c, "name");
        p.brand           = getString(c, "brand");
        p.unit            = getString(c, "unit");
        p.cost            = getDouble(c, "cost");
        p.price           = getDouble(c, "price");
        p.qty             = getInt(c, "qty");
        p.location        = getString(c, "location");
        p.supplier        = getString(c, "supplier");
        p.expiry          = getString(c, "expiry");
        p.imagePath       = getString(c, "image_path");
        p.reorderLevel    = getInt(c, "reorder_level");
        p.category        = getString(c, "category");
        p.notes           = getString(c, "notes");
        p.batchNumber     = getString(c, "batch_number");
        p.supplierReference = getString(c, "supplier_reference");
        p.createdAt       = getString(c, "created_at");
        return p;
    }

    private ContentValues productToContentValues(Product p) {
        ContentValues cv = new ContentValues();
        cv.put("barcode", p.barcode);
        cv.put("name", p.name);
        cv.put("brand", p.brand);
        cv.put("unit", p.unit);
        cv.put("cost", p.cost);
        cv.put("price", p.price);
        cv.put("qty", p.qty);
        cv.put("location", p.location);
        cv.put("supplier", p.supplier);
        cv.put("expiry", p.expiry);
        cv.put("image_path", p.imagePath);
        cv.put("reorder_level", p.reorderLevel);
        cv.put("category", p.category);
        cv.put("notes", p.notes);
        cv.put("batch_number", p.batchNumber != null ? p.batchNumber : "");
        cv.put("supplier_reference", p.supplierReference != null ? p.supplierReference : "");
        return cv;
    }

    private String getString(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getString(idx) : null;
    }

    private long getLong(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getLong(idx) : 0L;
    }

    private int getInt(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getInt(idx) : 0;
    }

    private double getDouble(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getDouble(idx) : 0.0;
    }
}
