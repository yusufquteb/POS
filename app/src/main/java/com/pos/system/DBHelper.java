package com.pos.system;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DBHelper - النسخة النهائية الكاملة المصححة
 *
 * الإصلاحات المضافة:
 * - isProductExists(String barcode)
 * - insertProduct(String, String, String, String, double, double, int, String, String, String, String, int, String, String)
 * - getAllLocations() → List<HashMap<String,String>>
 * - getAllProductsList() → List<HashMap<String,String>>
 * - createInvoice(String, String, List, double, double, double)
 * - decreaseProductQuantity(String, int)
 * - addExpense overload (String,double,String,String,String,String)
 * - deleteExpense(long id)
 * - getInvoiceById(long id) / getInvoiceItems(long id)
 * - getCustomerById(int id)
 * - getTotalProductsCount() / getTotalInventoryValue()
 * - updateStoreSettings(String,String,String,String,String)
 * - updatePrinterSettings(String,String,int,int) / getPrinterSettings()
 * - getDatabasePath()
 * - updateSubscription(boolean,String,String,String) / isPremiumUser()
 * - getAllProductsAsJSON / getAllCustomersAsJSON / getAllSuppliersAsJSON
 *   / getAllInvoicesAsJSON / getAllExpensesAsJSON
 * - productExists / customerExistsByPhone / supplierExistsByPhone / invoiceExists
 * - insertProductFromJSON / insertCustomerFromJSON / insertSupplierFromJSON
 *   / insertInvoiceFromJSON / insertExpenseFromJSON
 *
 * @author POS System
 * @version 1.0 (First Release - Google Play)
 * @since 2026-02-22
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    public static final String DATABASE_NAME    = "SmartPOS.db";
    public static final int    DATABASE_VERSION = 1;

    private final Context mContext;

    // أسماء الجداول
    private static final String TABLE_PRODUCTS         = "products";
    private static final String TABLE_SUPPLIERS        = "suppliers";
    private static final String TABLE_CUSTOMERS        = "customers";
    private static final String TABLE_INVOICES         = "invoices";
    private static final String TABLE_INVOICE_ITEMS    = "invoice_items";
    private static final String TABLE_LOCATIONS        = "locations";
    private static final String TABLE_CATEGORIES       = "categories";
    private static final String TABLE_STORE_SETTINGS   = "store_settings";
    private static final String TABLE_PRINTER_SETTINGS = "printer_settings";
    private static final String TABLE_EXPENSES         = "expenses";
    private static final String TABLE_BACKUP_LOG       = "backup_log";

    // ════════════════════════════════════════════════════════════
    // Constructor
    // ════════════════════════════════════════════════════════════
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    // ════════════════════════════════════════════════════════════
    // onCreate
    // ════════════════════════════════════════════════════════════
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.d(TAG, "Creating database version " + DATABASE_VERSION);
            createAllTables(db);
        } catch (Exception e) {
            Log.e(TAG, "Error creating database: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════
    // onUpgrade
    // ════════════════════════════════════════════════════════════
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from v" + oldVersion + " to v" + newVersion);
        try {
            if (oldVersion < 2) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "amount REAL DEFAULT 0.0, " +
                    "category TEXT, " +
                    "note TEXT, " +
                    "date TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            }
            if (oldVersion < 3) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BACKUP_LOG + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "file_path TEXT NOT NULL, " +
                    "file_size INTEGER DEFAULT 0, " +
                    "backup_type TEXT DEFAULT 'local', " +
                    "status TEXT DEFAULT 'success', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
                try { db.execSQL("ALTER TABLE " + TABLE_SUPPLIERS + " ADD COLUMN company TEXT"); }
                catch (Exception ignored) {}
                try { db.execSQL("ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN debt REAL DEFAULT 0.0"); }
                catch (Exception ignored) {}
                try { db.execSQL("ALTER TABLE " + TABLE_STORE_SETTINGS + " ADD COLUMN is_premium INTEGER DEFAULT 0"); }
                catch (Exception ignored) {}
                try { db.execSQL("ALTER TABLE " + TABLE_STORE_SETTINGS + " ADD COLUMN subscription_end TEXT"); }
                catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database: " + e.getMessage(), e);
            dropAllTables(db);
            createAllTables(db);
        }
    }

    // ════════════════════════════════════════════════════════════
    // إنشاء جميع الجداول
    // ════════════════════════════════════════════════════════════
    private void createAllTables(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "barcode TEXT UNIQUE NOT NULL, " +
            "name TEXT NOT NULL, " +
            "brand TEXT, " +
            "unit TEXT, " +
            "cost REAL DEFAULT 0.0, " +
            "price REAL DEFAULT 0.0, " +
            "qty INTEGER DEFAULT 0, " +
            "location TEXT, " +
            "supplier TEXT, " +
            "expiry TEXT, " +
            "image_path TEXT, " +
            "reorder_level INTEGER DEFAULT 5, " +
            "category TEXT, " +
            "notes TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SUPPLIERS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "company TEXT, " +
            "phone TEXT, " +
            "address TEXT, " +
            "notes TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOMERS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "phone TEXT, " +
            "email TEXT, " +
            "address TEXT, " +
            "debt REAL DEFAULT 0.0, " +
            "notes TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INVOICES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "invoice_number TEXT UNIQUE, " +
            "customer_id INTEGER DEFAULT 0, " +
            "customer_name TEXT, " +
            "subtotal REAL DEFAULT 0.0, " +
            "discount REAL DEFAULT 0.0, " +
            "tax REAL DEFAULT 0.0, " +
            "total REAL DEFAULT 0.0, " +
            "payment_method TEXT DEFAULT 'نقدي', " +
            "status TEXT DEFAULT 'completed', " +
            "notes TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INVOICE_ITEMS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "invoice_id INTEGER NOT NULL, " +
            "product_id INTEGER, " +
            "barcode TEXT, " +
            "name TEXT NOT NULL, " +
            "price REAL DEFAULT 0.0, " +
            "qty INTEGER DEFAULT 1, " +
            "total REAL DEFAULT 0.0, " +
            "FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOCATIONS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "address TEXT, " +
            "is_main INTEGER DEFAULT 0, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL UNIQUE, " +
            "color TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STORE_SETTINGS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "key TEXT NOT NULL UNIQUE, " +
            "value TEXT, " +
            "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRINTER_SETTINGS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "printer_type TEXT DEFAULT 'bluetooth', " +
            "printer_address TEXT, " +
            "printer_name TEXT, " +
            "paper_width INTEGER DEFAULT 80, " +
            "copies INTEGER DEFAULT 1, " +
            "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "title TEXT NOT NULL, " +
            "amount REAL DEFAULT 0.0, " +
            "category TEXT, " +
            "note TEXT, " +
            "date TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BACKUP_LOG + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_path TEXT NOT NULL, " +
            "file_size INTEGER DEFAULT 0, " +
            "backup_type TEXT DEFAULT 'local', " +
            "status TEXT DEFAULT 'success', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        insertDefaultSettings(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        String[] tables = {
            TABLE_INVOICE_ITEMS, TABLE_INVOICES, TABLE_PRODUCTS,
            TABLE_CUSTOMERS, TABLE_SUPPLIERS, TABLE_LOCATIONS,
            TABLE_CATEGORIES, TABLE_STORE_SETTINGS, TABLE_PRINTER_SETTINGS,
            TABLE_EXPENSES, TABLE_BACKUP_LOG
        };
        for (String t : tables) db.execSQL("DROP TABLE IF EXISTS " + t);
    }

    private void insertDefaultSettings(SQLiteDatabase db) {
        String[][] defaults = {
            {"name", "متجري"}, {"phone", ""}, {"address", ""},
            {"tax_rate", "15.0"}, {"tax_enabled", "true"},
            {"currency", "ر.س"}, {"country_code", "SA"},
            {"is_premium", "false"}, {"subscription_end", ""}
        };
        for (String[] kv : defaults) {
            ContentValues cv = new ContentValues();
            cv.put("key", kv[0]);
            cv.put("value", kv[1]);
            db.insertWithOnConflict(TABLE_STORE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ════════════════════════════════════════════════════════════
    // Products CRUD
    // ════════════════════════════════════════════════════════════

    /** الدالة الأصلية – تقبل HashMap */
    public long addProduct(HashMap<String, String> product) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("barcode",       safeGet(product, "barcode"));
            cv.put("name",          safeGet(product, "name"));
            cv.put("brand",         safeGet(product, "brand"));
            cv.put("unit",          safeGet(product, "unit"));
            cv.put("cost",          safeDouble(product, "cost"));
            cv.put("price",         safeDouble(product, "price"));
            cv.put("qty",           safeInt(product, "qty"));
            cv.put("location",      safeGet(product, "location"));
            cv.put("supplier",      safeGet(product, "supplier"));
            cv.put("expiry",        safeGet(product, "expiry"));
            cv.put("image_path",    safeGet(product, "image_path"));
            cv.put("reorder_level", safeInt(product, "reorder_level", 5));
            cv.put("category",      safeGet(product, "category"));
            cv.put("notes",         safeGet(product, "notes"));
            return db.insertWithOnConflict(TABLE_PRODUCTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "addProduct: " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * ✅ إضافة منتج بمعاملات مباشرة (مطلوب من ActivityAddProductActivity)
     */
    public boolean insertProduct(String barcode, String name, String brand, String unit,
                                 double cost, double price, int qty,
                                 String location, String supplier, String expiry,
                                 String imagePath, int reorderLevel,
                                 String category, String notes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("barcode",       barcode != null ? barcode : "");
            cv.put("name",          name    != null ? name    : "");
            cv.put("brand",         brand   != null ? brand   : "");
            cv.put("unit",          unit    != null ? unit    : "");
            cv.put("cost",          cost);
            cv.put("price",         price);
            cv.put("qty",           qty);
            cv.put("location",      location   != null ? location   : "");
            cv.put("supplier",      supplier   != null ? supplier   : "");
            cv.put("expiry",        expiry     != null ? expiry     : "");
            cv.put("image_path",    imagePath  != null ? imagePath  : "");
            cv.put("reorder_level", reorderLevel);
            cv.put("category",      category   != null ? category   : "");
            cv.put("notes",         notes      != null ? notes      : "");
            return db.insertWithOnConflict(TABLE_PRODUCTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE) != -1;
        } catch (Exception e) {
            Log.e(TAG, "insertProduct: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ التحقق من وجود منتج بالباركود (مطلوب من ActivityAddProductActivity)
     */
    public boolean isProductExists(String barcode) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE barcode=?",
                new String[]{barcode});
            boolean exists = false;
            if (c.moveToFirst()) exists = c.getInt(0) > 0;
            c.close();
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "isProductExists: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ التحقق من وجود منتج (alias مطلوب من SafeBackupManager)
     */
    public boolean productExists(String barcode) {
        return isProductExists(barcode);
    }

    public int updateProduct(String id, HashMap<String, String> product) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("barcode",       safeGet(product, "barcode"));
            cv.put("name",          safeGet(product, "name"));
            cv.put("brand",         safeGet(product, "brand"));
            cv.put("unit",          safeGet(product, "unit"));
            cv.put("cost",          safeDouble(product, "cost"));
            cv.put("price",         safeDouble(product, "price"));
            cv.put("qty",           safeInt(product, "qty"));
            cv.put("location",      safeGet(product, "location"));
            cv.put("supplier",      safeGet(product, "supplier"));
            cv.put("expiry",        safeGet(product, "expiry"));
            cv.put("image_path",    safeGet(product, "image_path"));
            cv.put("reorder_level", safeInt(product, "reorder_level", 5));
            cv.put("category",      safeGet(product, "category"));
            cv.put("notes",         safeGet(product, "notes"));
            return db.update(TABLE_PRODUCTS, cv, "id=?", new String[]{id});
        } catch (Exception e) {
            Log.e(TAG, "updateProduct: " + e.getMessage(), e);
            return 0;
        }
    }

    public boolean deleteProduct(String id) {
        try {
            return getWritableDatabase().delete(TABLE_PRODUCTS, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteProduct: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ إرجاع List (مستخدمة داخلياً وفي أماكن صحيحة) */
    public List<HashMap<String, String>> getAllProducts() {
        return queryProducts("SELECT * FROM " + TABLE_PRODUCTS + " ORDER BY name ASC", null);
    }

    /**
     * ✅ alias مطلوب من ActivityProductsActivity
     */
    public List<HashMap<String, String>> getAllProductsList() {
        return getAllProducts();
    }

    /**
     * ✅ عدد المنتجات الكلي (مطلوب من ActivityReportsActivity)
     */
    public int getTotalProductsCount() {
        return getProductsCount();
    }

    /**
     * ✅ قيمة المخزون الكلية (مطلوب من ActivityReportsActivity)
     */
    public double getTotalInventoryValue() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT SUM(cost * qty) FROM " + TABLE_PRODUCTS, null);
            double val = 0;
            if (c.moveToFirst() && !c.isNull(0)) val = c.getDouble(0);
            c.close();
            return val;
        } catch (Exception e) {
            Log.e(TAG, "getTotalInventoryValue: " + e.getMessage(), e);
            return 0;
        }
    }

    public HashMap<String, String> getProductById(String id) {
        List<HashMap<String, String>> list = queryProducts(
            "SELECT * FROM " + TABLE_PRODUCTS + " WHERE id=?", new String[]{id});
        return list.isEmpty() ? null : list.get(0);
    }

    public HashMap<String, String> getProductByBarcode(String barcode) {
        List<HashMap<String, String>> list = queryProducts(
            "SELECT * FROM " + TABLE_PRODUCTS + " WHERE barcode=?", new String[]{barcode});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<HashMap<String, String>> searchProducts(String query) {
        return queryProducts(
            "SELECT * FROM " + TABLE_PRODUCTS +
            " WHERE name LIKE ? OR barcode LIKE ? ORDER BY name ASC",
            new String[]{"%" + query + "%", "%" + query + "%"});
    }

    public List<HashMap<String, String>> getLowStockProducts(int threshold) {
        return queryProducts(
            "SELECT * FROM " + TABLE_PRODUCTS + " WHERE qty <= reorder_level ORDER BY qty ASC", null);
    }

    public int getProductsCount() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
            int count = 0;
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
            return count;
        } catch (Exception e) {
            Log.e(TAG, "getProductsCount: " + e.getMessage(), e);
            return 0;
        }
    }

    public boolean updateProductQuantity(String productId, int newQty) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("qty", newQty);
            return db.update(TABLE_PRODUCTS, cv, "id=?", new String[]{productId}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateProductQuantity: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ تقليل كمية منتج (مطلوب من ActivityCartActivity)
     */
    public boolean decreaseProductQuantity(String productId, int decreaseBy) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(
                "UPDATE " + TABLE_PRODUCTS +
                " SET qty = MAX(0, qty - " + decreaseBy + ") WHERE id = ?",
                new String[]{productId});
            return true;
        } catch (Exception e) {
            Log.e(TAG, "decreaseProductQuantity: " + e.getMessage(), e);
            return false;
        }
    }

    private List<HashMap<String, String>> queryProducts(String sql, String[] args) {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(sql, args);
            while (c.moveToNext()) {
                HashMap<String, String> row = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    row.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                }
                list.add(row);
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "queryProducts: " + e.getMessage(), e);
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    // Customers CRUD
    // ════════════════════════════════════════════════════════════
    public long addCustomer(HashMap<String, String> customer) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name",    safeGet(customer, "name"));
            cv.put("phone",   safeGet(customer, "phone"));
            cv.put("email",   safeGet(customer, "email"));
            cv.put("address", safeGet(customer, "address"));
            cv.put("debt",    safeDouble(customer, "debt"));
            cv.put("notes",   safeGet(customer, "notes"));
            return db.insert(TABLE_CUSTOMERS, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addCustomer: " + e.getMessage(), e);
            return -1;
        }
    }

    public int updateCustomer(String id, HashMap<String, String> customer) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name",    safeGet(customer, "name"));
            cv.put("phone",   safeGet(customer, "phone"));
            cv.put("email",   safeGet(customer, "email"));
            cv.put("address", safeGet(customer, "address"));
            cv.put("debt",    safeDouble(customer, "debt"));
            cv.put("notes",   safeGet(customer, "notes"));
            return db.update(TABLE_CUSTOMERS, cv, "id=?", new String[]{id});
        } catch (Exception e) {
            Log.e(TAG, "updateCustomer: " + e.getMessage(), e);
            return 0;
        }
    }

    public boolean deleteCustomer(String id) {
        try {
            return getWritableDatabase().delete(TABLE_CUSTOMERS, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteCustomer: " + e.getMessage(), e);
            return false;
        }
    }

    public List<HashMap<String, String>> getAllCustomers() {
        return queryTable("SELECT * FROM " + TABLE_CUSTOMERS + " ORDER BY name ASC", null);
    }

    public List<HashMap<String, String>> searchCustomers(String query) {
        return queryTable(
            "SELECT * FROM " + TABLE_CUSTOMERS +
            " WHERE name LIKE ? OR phone LIKE ? ORDER BY name ASC",
            new String[]{"%" + query + "%", "%" + query + "%"});
    }

    /** بمعامل String */
    public HashMap<String, String> getCustomerById(String id) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_CUSTOMERS + " WHERE id=?", new String[]{id});
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * ✅ overload بمعامل int (مطلوب من ActivityInvoiceDetailsActivity و InvoicePrinter)
     * يُرجع HashMap<String, Object> لتوافق الاستخدام
     */
    public HashMap<String, Object> getCustomerById(int id) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_CUSTOMERS + " WHERE id=?",
            new String[]{String.valueOf(id)});
        if (list.isEmpty()) return null;
        HashMap<String, Object> result = new HashMap<>();
        result.putAll(list.get(0));
        return result;
    }

    /**
     * ✅ التحقق من وجود عميل بالهاتف (مطلوب من SafeBackupManager)
     */
    public boolean customerExistsByPhone(String phone) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_CUSTOMERS + " WHERE phone=?",
                new String[]{phone});
            boolean exists = false;
            if (c.moveToFirst()) exists = c.getInt(0) > 0;
            c.close();
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "customerExistsByPhone: " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Suppliers CRUD
    // ════════════════════════════════════════════════════════════
    public long addSupplier(HashMap<String, String> supplier) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name",    safeGet(supplier, "name"));
            cv.put("company", safeGet(supplier, "company"));
            cv.put("phone",   safeGet(supplier, "phone"));
            cv.put("address", safeGet(supplier, "address"));
            cv.put("notes",   safeGet(supplier, "notes"));
            return db.insert(TABLE_SUPPLIERS, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addSupplier: " + e.getMessage(), e);
            return -1;
        }
    }

    public int updateSupplier(String id, HashMap<String, String> supplier) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name",    safeGet(supplier, "name"));
            cv.put("company", safeGet(supplier, "company"));
            cv.put("phone",   safeGet(supplier, "phone"));
            cv.put("address", safeGet(supplier, "address"));
            cv.put("notes",   safeGet(supplier, "notes"));
            return db.update(TABLE_SUPPLIERS, cv, "id=?", new String[]{id});
        } catch (Exception e) {
            Log.e(TAG, "updateSupplier: " + e.getMessage(), e);
            return 0;
        }
    }

    public boolean deleteSupplier(String id) {
        try {
            return getWritableDatabase().delete(TABLE_SUPPLIERS, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteSupplier: " + e.getMessage(), e);
            return false;
        }
    }

    public List<HashMap<String, String>> getAllSuppliers() {
        return queryTable("SELECT * FROM " + TABLE_SUPPLIERS + " ORDER BY name ASC", null);
    }

    public List<HashMap<String, String>> searchSuppliers(String query) {
        return queryTable(
            "SELECT * FROM " + TABLE_SUPPLIERS +
            " WHERE name LIKE ? OR company LIKE ? ORDER BY name ASC",
            new String[]{"%" + query + "%", "%" + query + "%"});
    }

    /**
     * ✅ التحقق من وجود مورد بالهاتف (مطلوب من SafeBackupManager)
     */
    public boolean supplierExistsByPhone(String phone) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_SUPPLIERS + " WHERE phone=?",
                new String[]{phone});
            boolean exists = false;
            if (c.moveToFirst()) exists = c.getInt(0) > 0;
            c.close();
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "supplierExistsByPhone: " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Locations
    // ════════════════════════════════════════════════════════════
    public long addLocation(String name, String address, boolean isMain) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name",    name);
            cv.put("address", address);
            cv.put("is_main", isMain ? 1 : 0);
            return db.insert(TABLE_LOCATIONS, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addLocation: " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * ✅ الحصول على جميع المواقع (مطلوب من ActivityAddProductActivity)
     */
    public List<HashMap<String, String>> getAllLocations() {
        return queryTable("SELECT * FROM " + TABLE_LOCATIONS + " ORDER BY name ASC", null);
    }

    public boolean deleteLocation(String id) {
        try {
            return getWritableDatabase().delete(TABLE_LOCATIONS, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteLocation: " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Invoices CRUD
    // ════════════════════════════════════════════════════════════

    /** الدالة الأصلية بـ HashMap */
    public long addInvoice(HashMap<String, Object> invoice, List<HashMap<String, String>> items) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("invoice_number", String.valueOf(invoice.get("invoice_number")));
            cv.put("customer_id",    invoice.containsKey("customer_id") ?
                    ((Number) invoice.get("customer_id")).intValue() : 0);
            cv.put("customer_name",  String.valueOf(invoice.get("customer_name")));
            cv.put("subtotal",       invoice.containsKey("subtotal") ?
                    ((Number) invoice.get("subtotal")).doubleValue() : 0.0);
            cv.put("discount",       invoice.containsKey("discount") ?
                    ((Number) invoice.get("discount")).doubleValue() : 0.0);
            cv.put("tax",            invoice.containsKey("tax") ?
                    ((Number) invoice.get("tax")).doubleValue() : 0.0);
            cv.put("total",          invoice.containsKey("total") ?
                    ((Number) invoice.get("total")).doubleValue() : 0.0);
            cv.put("payment_method", String.valueOf(invoice.get("payment_method")));
            cv.put("status",         "completed");
            cv.put("notes",          invoice.containsKey("notes") ?
                    String.valueOf(invoice.get("notes")) : "");

            long invoiceId = db.insert(TABLE_INVOICES, null, cv);
            if (invoiceId == -1) throw new Exception("Failed to insert invoice");

            for (HashMap<String, String> item : items) {
                ContentValues itemCv = new ContentValues();
                itemCv.put("invoice_id", invoiceId);
                itemCv.put("product_id", safeGet(item, "product_id"));
                itemCv.put("barcode",    safeGet(item, "barcode"));
                itemCv.put("name",       safeGet(item, "name"));
                itemCv.put("price",      safeDouble(item, "price"));
                itemCv.put("qty",        safeInt(item, "qty", 1));
                itemCv.put("total",      safeDouble(item, "total"));
                db.insert(TABLE_INVOICE_ITEMS, null, itemCv);

                String pid = safeGet(item, "product_id");
                int qty = safeInt(item, "qty", 1);
                if (!pid.isEmpty()) {
                    db.execSQL("UPDATE " + TABLE_PRODUCTS +
                        " SET qty = MAX(0, qty - " + qty + ") WHERE id = ?",
                        new String[]{pid});
                }
            }

            db.setTransactionSuccessful();
            return invoiceId;
        } catch (Exception e) {
            Log.e(TAG, "addInvoice: " + e.getMessage(), e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * ✅ createInvoiceWithDetails - الإصدار الكامل
     * يدعم: اسم العميل، الضريبة، طريقة الدفع
     */
    public <T> long createInvoiceWithDetails(String invoiceNumber, String customerId,
                                              String customerName, List<T> cartItems,
                                              double subtotal, double discount,
                                              double tax, double total, String paymentMethod) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("invoice_number", invoiceNumber);
            cv.put("customer_id",    customerId != null ? customerId : "0");
            cv.put("customer_name",  customerName != null ? customerName : "");
            cv.put("subtotal",       subtotal);
            cv.put("discount",       discount);
            cv.put("tax",            tax);
            cv.put("total",          total);
            cv.put("payment_method", paymentMethod != null ? paymentMethod : "cash");
            cv.put("status",         "completed");
            cv.put("notes",          "");

            long invoiceId = db.insert(TABLE_INVOICES, null, cv);
            if (invoiceId == -1) throw new Exception("Failed to insert invoice");

            for (T item : cartItems) {
                try {
                    java.lang.reflect.Field idF    = item.getClass().getDeclaredField("id");
                    java.lang.reflect.Field nameF  = item.getClass().getDeclaredField("name");
                    java.lang.reflect.Field priceF = item.getClass().getDeclaredField("price");
                    java.lang.reflect.Field qtyF   = item.getClass().getDeclaredField("quantity");
                    idF.setAccessible(true); nameF.setAccessible(true);
                    priceF.setAccessible(true); qtyF.setAccessible(true);

                    String pid    = String.valueOf(idF.get(item));
                    String pname  = String.valueOf(nameF.get(item));
                    double pprice = ((Number) priceF.get(item)).doubleValue();
                    int    pqty   = ((Number) qtyF.get(item)).intValue();

                    ContentValues icv = new ContentValues();
                    icv.put("invoice_id",  invoiceId);
                    icv.put("product_id",  pid);
                    icv.put("barcode",     "");
                    icv.put("name",        pname);
                    icv.put("price",       pprice);
                    icv.put("qty",         pqty);
                    icv.put("total",       pprice * pqty);
                    db.insert(TABLE_INVOICE_ITEMS, null, icv);
                } catch (Exception ex) {
                    Log.w(TAG, "createInvoiceWithDetails: cart item error: " + ex.getMessage());
                }
            }
            db.setTransactionSuccessful();
            return invoiceId;
        } catch (Exception e) {
            Log.e(TAG, "createInvoiceWithDetails: " + e.getMessage(), e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /**
    @SuppressWarnings("unchecked")
    public <T> long createInvoice(String invoiceNumber, String customerId,
                                   List<T> cartItems,
                                   double subtotal, double discount, double total) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("invoice_number", invoiceNumber);
            cv.put("customer_id",    customerId != null ? customerId : "0");
            cv.put("customer_name",  "");
            cv.put("subtotal",       subtotal);
            cv.put("discount",       discount);
            cv.put("tax",            0.0);
            cv.put("total",          total);
            cv.put("payment_method", "نقدي");
            cv.put("status",         "completed");
            cv.put("notes",          "");

            long invoiceId = db.insert(TABLE_INVOICES, null, cv);
            if (invoiceId == -1) throw new Exception("Failed to insert invoice");

            // نحاول التعامل مع CartItems عبر reflection أو duck typing
            for (T item : cartItems) {
                try {
                    java.lang.reflect.Field idF  = item.getClass().getDeclaredField("id");
                    java.lang.reflect.Field nameF = item.getClass().getDeclaredField("name");
                    java.lang.reflect.Field priceF = item.getClass().getDeclaredField("price");
                    java.lang.reflect.Field qtyF   = item.getClass().getDeclaredField("quantity");
                    idF.setAccessible(true);
                    nameF.setAccessible(true);
                    priceF.setAccessible(true);
                    qtyF.setAccessible(true);

                    String pid   = String.valueOf(idF.get(item));
                    String pname = String.valueOf(nameF.get(item));
                    double pprice = ((Number) priceF.get(item)).doubleValue();
                    int    pqty   = ((Number) qtyF.get(item)).intValue();

                    ContentValues itemCv = new ContentValues();
                    itemCv.put("invoice_id",  invoiceId);
                    itemCv.put("product_id",  pid);
                    itemCv.put("barcode",     "");
                    itemCv.put("name",        pname);
                    itemCv.put("price",       pprice);
                    itemCv.put("qty",         pqty);
                    itemCv.put("total",       pprice * pqty);
                    db.insert(TABLE_INVOICE_ITEMS, null, itemCv);

                    // تحديث المخزون
                    db.execSQL("UPDATE " + TABLE_PRODUCTS +
                        " SET qty = MAX(0, qty - " + pqty + ") WHERE id = ?",
                        new String[]{pid});
                } catch (Exception ex) {
                    Log.w(TAG, "createInvoice: cannot read cart item fields: " + ex.getMessage());
                }
            }

            db.setTransactionSuccessful();
            return invoiceId;
        } catch (Exception e) {
            Log.e(TAG, "createInvoice: " + e.getMessage(), e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public List<HashMap<String, String>> getAllInvoices() {
        return queryTable("SELECT * FROM " + TABLE_INVOICES + " ORDER BY created_at DESC", null);
    }

    public List<HashMap<String, String>> searchInvoices(String query) {
        return queryTable(
            "SELECT * FROM " + TABLE_INVOICES +
            " WHERE invoice_number LIKE ? OR customer_name LIKE ? ORDER BY created_at DESC",
            new String[]{"%" + query + "%", "%" + query + "%"});
    }

    /** بمعامل String */
    public HashMap<String, String> getInvoiceByIdStr(String id) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_INVOICES + " WHERE id=?", new String[]{id});
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * ✅ overload بمعامل long → HashMap<String,Object>
     * (مطلوب من ActivityInvoiceDetailsActivity و InvoicePrinter)
     */
    public HashMap<String, Object> getInvoiceById(long id) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_INVOICES + " WHERE id=?",
            new String[]{String.valueOf(id)});
        if (list.isEmpty()) return null;
        HashMap<String, Object> result = new HashMap<>();
        HashMap<String, String> row = list.get(0);
        result.putAll(row);
        // تحويل الحقول الرقمية
        try { result.put("total",    Double.parseDouble(row.getOrDefault("total",    "0"))); } catch (Exception ignored) {}
        try { result.put("discount", Double.parseDouble(row.getOrDefault("discount", "0"))); } catch (Exception ignored) {}
        try { result.put("tax",      Double.parseDouble(row.getOrDefault("tax",      "0"))); } catch (Exception ignored) {}
        try { result.put("customer_id", Integer.parseInt(row.getOrDefault("customer_id", "0"))); } catch (Exception ignored) {}
        return result;
    }

    /** بمعامل String → List<HashMap<String,String>> */
    public List<HashMap<String, String>> getInvoiceItems(String invoiceId) {
        return queryTable(
            "SELECT * FROM " + TABLE_INVOICE_ITEMS + " WHERE invoice_id=?",
            new String[]{invoiceId});
    }

    /**
     * ✅ overload بمعامل long → ArrayList<HashMap<String,Object>>
     * (مطلوب من ActivityInvoiceDetailsActivity و InvoicePrinter)
     */
    public ArrayList<HashMap<String, Object>> getInvoiceItems(long invoiceId) {
        List<HashMap<String, String>> raw = queryTable(
            "SELECT * FROM " + TABLE_INVOICE_ITEMS + " WHERE invoice_id=?",
            new String[]{String.valueOf(invoiceId)});
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        for (HashMap<String, String> row : raw) {
            HashMap<String, Object> item = new HashMap<>();
            item.putAll(row);
            try { item.put("price", Double.parseDouble(row.getOrDefault("price", "0"))); } catch (Exception ignored) {}
            try { item.put("qty",   Integer.parseInt(row.getOrDefault("qty",   "1"))); } catch (Exception ignored) {}
            try { item.put("total", Double.parseDouble(row.getOrDefault("total", "0"))); } catch (Exception ignored) {}
            result.add(item);
        }
        return result;
    }

    /**
     * ✅ التحقق من وجود فاتورة (مطلوب من SafeBackupManager)
     */
    public boolean invoiceExists(String invoiceNumber) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_INVOICES + " WHERE invoice_number=?",
                new String[]{invoiceNumber});
            boolean exists = false;
            if (c.moveToFirst()) exists = c.getInt(0) > 0;
            c.close();
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "invoiceExists: " + e.getMessage(), e);
            return false;
        }
    }

    public HashMap<String, Object> getInvoicesStatistics() {
        HashMap<String, Object> stats = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();

            Cursor c = db.rawQuery(
                "SELECT COUNT(*), SUM(total) FROM " + TABLE_INVOICES +
                " WHERE DATE(created_at) = DATE('now')", null);
            if (c.moveToFirst()) {
                stats.put("today_count", c.getInt(0));
                stats.put("today_total", c.isNull(1) ? 0.0 : c.getDouble(1));
            }
            c.close();

            c = db.rawQuery(
                "SELECT COUNT(*), SUM(total) FROM " + TABLE_INVOICES +
                " WHERE strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now')", null);
            if (c.moveToFirst()) {
                stats.put("month_count", c.getInt(0));
                stats.put("month_total", c.isNull(1) ? 0.0 : c.getDouble(1));
            }
            c.close();

            c = db.rawQuery("SELECT COUNT(*), SUM(total) FROM " + TABLE_INVOICES, null);
            if (c.moveToFirst()) {
                stats.put("all_count", c.getInt(0));
                stats.put("all_total", c.isNull(1) ? 0.0 : c.getDouble(1));
            }
            c.close();

        } catch (Exception e) {
            Log.e(TAG, "getInvoicesStatistics: " + e.getMessage(), e);
        }
        return stats;
    }

    // ════════════════════════════════════════════════════════════
    // Expenses CRUD
    // ════════════════════════════════════════════════════════════

    /** الدالة الأصلية بـ HashMap */
    public long addExpense(HashMap<String, String> expense) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("title",    safeGet(expense, "title"));
            cv.put("amount",   safeDouble(expense, "amount"));
            cv.put("category", safeGet(expense, "category"));
            cv.put("note",     safeGet(expense, "note"));
            cv.put("date",     safeGet(expense, "date"));
            return db.insert(TABLE_EXPENSES, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addExpense(map): " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * ✅ overload بمعاملات مباشرة (مطلوب من ActivityExpensesActivity)
     * addExpense(category, amount, description, date, paymentMethod, notes)
     */
    public long addExpense(String category, double amount, String description,
                           String date, String paymentMethod, String notes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("title",    description != null ? description : category);
            cv.put("amount",   amount);
            cv.put("category", category != null ? category : "");
            cv.put("note",     notes != null ? notes : "");
            cv.put("date",     date  != null ? date  : getCurrentDateTime());
            return db.insert(TABLE_EXPENSES, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addExpense(params): " + e.getMessage(), e);
            return -1;
        }
    }

    public int updateExpense(String id, HashMap<String, String> expense) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("title",    safeGet(expense, "title"));
            cv.put("amount",   safeDouble(expense, "amount"));
            cv.put("category", safeGet(expense, "category"));
            cv.put("note",     safeGet(expense, "note"));
            cv.put("date",     safeGet(expense, "date"));
            return db.update(TABLE_EXPENSES, cv, "id=?", new String[]{id});
        } catch (Exception e) {
            Log.e(TAG, "updateExpense: " + e.getMessage(), e);
            return 0;
        }
    }

    /** حذف بمعامل String */
    public boolean deleteExpense(String id) {
        try {
            return getWritableDatabase().delete(TABLE_EXPENSES, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteExpense(String): " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ overload حذف بمعامل long (مطلوب من ActivityExpensesActivity)
     */
    public boolean deleteExpense(long id) {
        return deleteExpense(String.valueOf(id));
    }

    public List<HashMap<String, String>> getAllExpenses() {
        return queryTable("SELECT * FROM " + TABLE_EXPENSES + " ORDER BY created_at DESC", null);
    }

    public List<HashMap<String, String>> searchExpenses(String query) {
        return queryTable(
            "SELECT * FROM " + TABLE_EXPENSES +
            " WHERE title LIKE ? OR category LIKE ? ORDER BY created_at DESC",
            new String[]{"%" + query + "%", "%" + query + "%"});
    }

    public double getTotalExpenses() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_EXPENSES, null);
            double total = 0;
            if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
            c.close();
            return total;
        } catch (Exception e) {
            Log.e(TAG, "getTotalExpenses: " + e.getMessage(), e);
            return 0;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Store Settings
    // ════════════════════════════════════════════════════════════
    public HashMap<String, String> getStoreSettings() {
        HashMap<String, String> settings = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT key, value FROM " + TABLE_STORE_SETTINGS, null);
            while (c.moveToNext()) {
                settings.put(c.getString(0), c.getString(1) != null ? c.getString(1) : "");
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "getStoreSettings: " + e.getMessage(), e);
        }
        return settings;
    }

    public boolean saveStoreSetting(String key, String value) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("key",        key);
            cv.put("value",      value);
            cv.put("updated_at", getCurrentDateTime());
            return db.insertWithOnConflict(TABLE_STORE_SETTINGS, null, cv,
                    SQLiteDatabase.CONFLICT_REPLACE) != -1;
        } catch (Exception e) {
            Log.e(TAG, "saveStoreSetting: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean saveStoreSettings(HashMap<String, String> settings) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            for (HashMap.Entry<String, String> entry : settings.entrySet()) {
                ContentValues cv = new ContentValues();
                cv.put("key",        entry.getKey());
                cv.put("value",      entry.getValue());
                cv.put("updated_at", getCurrentDateTime());
                db.insertWithOnConflict(TABLE_STORE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "saveStoreSettings: " + e.getMessage(), e);
            return false;
        } finally {
            getWritableDatabase().endTransaction();
        }
    }

    /**
     * ✅ تحديث إعدادات المتجر بمعاملات مباشرة
     * يحفظ: name, phone, tax_rate, logo_path, address
     * (مطلوب من ActivityStoreSettingsActivity)
     */
    public boolean updateStoreSettings(String name, String phone, String taxRate,
                                       String logoPath, String address) {
        try {
            HashMap<String, String> settings = new HashMap<>();
            settings.put("name",      name     != null ? name     : "");
            settings.put("phone",     phone    != null ? phone    : "");
            settings.put("tax_rate",  taxRate  != null ? taxRate  : "0");
            settings.put("logo_path", logoPath != null ? logoPath : "");
            settings.put("address",   address  != null ? address  : "");
            settings.put("currency",  "ر.س");
            return saveStoreSettings(settings);
        } catch (Exception e) {
            Log.e(TAG, "updateStoreSettings: " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Printer Settings
    // ════════════════════════════════════════════════════════════

    /**
     * ✅ تحديث إعدادات الطابعة (مطلوب من ActivityPrinterSettingsActivity)
     */
    public boolean updatePrinterSettings(String connectionType, String paperWidth,
                                         int copies, int extraParam) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            // حذف الإعداد القديم وإعادة إدراجه
            db.delete(TABLE_PRINTER_SETTINGS, null, null);
            ContentValues cv = new ContentValues();
            cv.put("printer_type",  connectionType != null ? connectionType : "bluetooth");
            cv.put("paper_width",   parsePaperWidth(paperWidth));
            cv.put("copies",        copies);
            cv.put("updated_at",    getCurrentDateTime());
            return db.insert(TABLE_PRINTER_SETTINGS, null, cv) != -1;
        } catch (Exception e) {
            Log.e(TAG, "updatePrinterSettings: " + e.getMessage(), e);
            return false;
        }
    }

    private int parsePaperWidth(String pw) {
        if (pw == null) return 80;
        try { return Integer.parseInt(pw.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 80; }
    }

    /**
     * ✅ الحصول على إعدادات الطابعة (مطلوب من InvoicePrinter)
     */
    public HashMap<String, String> getPrinterSettings() {
        HashMap<String, String> settings = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_PRINTER_SETTINGS + " LIMIT 1", null);
            if (c.moveToFirst()) {
                for (int i = 0; i < c.getColumnCount(); i++) {
                    settings.put(c.getColumnName(i),
                        c.getString(i) != null ? c.getString(i) : "");
                }
            }
            c.close();
            // قيم افتراضية إن كان الجدول فارغاً
            if (!settings.containsKey("printer_type"))  settings.put("printer_type",  "bluetooth");
            if (!settings.containsKey("paper_width"))   settings.put("paper_width",   "80");
            if (!settings.containsKey("copies"))        settings.put("copies",        "1");
        } catch (Exception e) {
            Log.e(TAG, "getPrinterSettings: " + e.getMessage(), e);
        }
        return settings;
    }

    // ════════════════════════════════════════════════════════════
    // Categories
    // ════════════════════════════════════════════════════════════
    public long addCategory(String name, String color) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("color", color);
            return db.insertWithOnConflict(TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            Log.e(TAG, "addCategory: " + e.getMessage(), e);
            return -1;
        }
    }

    public List<HashMap<String, String>> getAllCategories() {
        return queryTable("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY name ASC", null);
    }

    public boolean deleteCategory(String id) {
        try {
            return getWritableDatabase().delete(TABLE_CATEGORIES, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteCategory: " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Backup & Database Path
    // ════════════════════════════════════════════════════════════

    /**
     * ✅ مسار قاعدة البيانات (مطلوب من BackupManager)
     */
    public String getDatabasePath() {
        return mContext.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    public long addBackupLog(String filePath, long fileSize, String backupType, String status) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("file_path",   filePath);
            cv.put("file_size",   fileSize);
            cv.put("backup_type", backupType);
            cv.put("status",      status);
            return db.insert(TABLE_BACKUP_LOG, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "addBackupLog: " + e.getMessage(), e);
            return -1;
        }
    }

    public List<HashMap<String, String>> getBackupLogs() {
        return queryTable("SELECT * FROM " + TABLE_BACKUP_LOG + " ORDER BY created_at DESC", null);
    }

    // ════════════════════════════════════════════════════════════
    // Subscription / Premium (مطلوب من BillingManager)
    // ════════════════════════════════════════════════════════════

    /**
     * ✅ تحديث بيانات الاشتراك
     */
    public boolean updateSubscription(boolean isPremium, String subscriptionEnd,
                                      String planId, String orderId) {
        try {
            HashMap<String, String> settings = new HashMap<>();
            settings.put("is_premium",        isPremium ? "true" : "false");
            settings.put("subscription_end",   subscriptionEnd != null ? subscriptionEnd : "");
            settings.put("plan_id",            planId  != null ? planId  : "");
            settings.put("order_id",           orderId != null ? orderId : "");
            return saveStoreSettings(settings);
        } catch (Exception e) {
            Log.e(TAG, "updateSubscription: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ التحقق من كون المستخدم مشتركاً
     */
    public boolean isPremiumUser() {
        try {
            HashMap<String, String> settings = getStoreSettings();
            return "true".equalsIgnoreCase(settings.getOrDefault("is_premium", "false"));
        } catch (Exception e) {
            Log.e(TAG, "isPremiumUser: " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Reports & Statistics
    // ════════════════════════════════════════════════════════════
    public List<HashMap<String, String>> getSalesByPeriod(String startDate, String endDate) {
        return queryTable(
            "SELECT DATE(created_at) as date, COUNT(*) as count, SUM(total) as total " +
            "FROM " + TABLE_INVOICES +
            " WHERE DATE(created_at) BETWEEN ? AND ? GROUP BY DATE(created_at) ORDER BY date ASC",
            new String[]{startDate, endDate});
    }

    public List<HashMap<String, String>> getTopSellingProducts(int limit) {
        return queryTable(
            "SELECT ii.name, SUM(ii.qty) as total_qty, SUM(ii.total) as total_sales " +
            "FROM " + TABLE_INVOICE_ITEMS + " ii " +
            "JOIN " + TABLE_INVOICES + " i ON ii.invoice_id = i.id " +
            "GROUP BY ii.name ORDER BY total_qty DESC LIMIT " + limit,
            null);
    }

    public HashMap<String, Object> getReportSummary(String startDate, String endDate) {
        HashMap<String, Object> summary = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();

            Cursor c = db.rawQuery(
                "SELECT COUNT(*), SUM(total), SUM(discount), SUM(tax) FROM " + TABLE_INVOICES +
                " WHERE DATE(created_at) BETWEEN ? AND ?",
                new String[]{startDate, endDate});

            if (c.moveToFirst()) {
                summary.put("invoice_count",  c.getInt(0));
                summary.put("total_sales",    c.isNull(1) ? 0.0 : c.getDouble(1));
                summary.put("total_discount", c.isNull(2) ? 0.0 : c.getDouble(2));
                summary.put("total_tax",      c.isNull(3) ? 0.0 : c.getDouble(3));
            }
            c.close();

            Cursor expC = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE_EXPENSES +
                " WHERE DATE(created_at) BETWEEN ? AND ?",
                new String[]{startDate, endDate});
            if (expC.moveToFirst()) {
                double expenses = expC.isNull(0) ? 0 : expC.getDouble(0);
                summary.put("total_expenses", expenses);
                Object sales = summary.get("total_sales");
                summary.put("net_profit",
                    (sales != null ? ((Number) sales).doubleValue() : 0) - expenses);
            }
            expC.close();

        } catch (Exception e) {
            Log.e(TAG, "getReportSummary: " + e.getMessage(), e);
        }
        return summary;
    }

    // ════════════════════════════════════════════════════════════
    // JSON Export (مطلوب من SafeBackupManager)
    // ════════════════════════════════════════════════════════════

    /** ✅ تصدير المنتجات كـ JSONArray */
    public JSONArray getAllProductsAsJSON() {
        return getTableAsJson(TABLE_PRODUCTS);
    }

    /** ✅ تصدير العملاء كـ JSONArray */
    public JSONArray getAllCustomersAsJSON() {
        return getTableAsJson(TABLE_CUSTOMERS);
    }

    /** ✅ تصدير الموردين كـ JSONArray */
    public JSONArray getAllSuppliersAsJSON() {
        return getTableAsJson(TABLE_SUPPLIERS);
    }

    /** ✅ تصدير الفواتير كـ JSONArray */
    public JSONArray getAllInvoicesAsJSON() {
        return getTableAsJson(TABLE_INVOICES);
    }

    /** ✅ تصدير المصروفات كـ JSONArray */
    public JSONArray getAllExpensesAsJSON() {
        return getTableAsJson(TABLE_EXPENSES);
    }

    public JSONArray getTableAsJson(String tableName) {
        JSONArray result = new JSONArray();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    row.put(c.getColumnName(i), c.getString(i));
                }
                result.put(row);
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "getTableAsJson(" + tableName + "): " + e.getMessage(), e);
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════
    // JSON Import (مطلوب من SafeBackupManager)
    // ════════════════════════════════════════════════════════════

    /** ✅ استيراد منتج من JSON */
    public boolean insertProductFromJSON(JSONObject product) {
        try {
            HashMap<String, String> map = jsonToMap(product);
            map.remove("id"); // تجنب تعارض المفتاح الأساسي
            return addProduct(map) != -1;
        } catch (Exception e) {
            Log.e(TAG, "insertProductFromJSON: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ استيراد عميل من JSON */
    public boolean insertCustomerFromJSON(JSONObject customer) {
        try {
            HashMap<String, String> map = jsonToMap(customer);
            map.remove("id");
            return addCustomer(map) != -1;
        } catch (Exception e) {
            Log.e(TAG, "insertCustomerFromJSON: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ استيراد مورد من JSON */
    public boolean insertSupplierFromJSON(JSONObject supplier) {
        try {
            HashMap<String, String> map = jsonToMap(supplier);
            map.remove("id");
            return addSupplier(map) != -1;
        } catch (Exception e) {
            Log.e(TAG, "insertSupplierFromJSON: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ استيراد فاتورة من JSON */
    public boolean insertInvoiceFromJSON(JSONObject invoice) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("invoice_number", invoice.optString("invoice_number", ""));
            cv.put("customer_id",    invoice.optInt("customer_id", 0));
            cv.put("customer_name",  invoice.optString("customer_name", ""));
            cv.put("subtotal",       invoice.optDouble("subtotal", 0));
            cv.put("discount",       invoice.optDouble("discount", 0));
            cv.put("tax",            invoice.optDouble("tax",      0));
            cv.put("total",          invoice.optDouble("total",    0));
            cv.put("payment_method", invoice.optString("payment_method", "نقدي"));
            cv.put("status",         invoice.optString("status", "completed"));
            cv.put("notes",          invoice.optString("notes", ""));
            return db.insertWithOnConflict(TABLE_INVOICES, null, cv,
                    SQLiteDatabase.CONFLICT_IGNORE) != -1;
        } catch (Exception e) {
            Log.e(TAG, "insertInvoiceFromJSON: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ استيراد مصروف من JSON */
    public boolean insertExpenseFromJSON(JSONObject expense) {
        try {
            HashMap<String, String> map = jsonToMap(expense);
            map.remove("id");
            return addExpense(map) != -1;
        } catch (Exception e) {
            Log.e(TAG, "insertExpenseFromJSON: " + e.getMessage(), e);
            return false;
        }
    }

    private HashMap<String, String> jsonToMap(JSONObject obj) {
        HashMap<String, String> map = new HashMap<>();
        try {
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, obj.optString(key, ""));
            }
        } catch (Exception e) {
            Log.e(TAG, "jsonToMap: " + e.getMessage(), e);
        }
        return map;
    }

    // ════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════
    private List<HashMap<String, String>> queryTable(String sql, String[] args) {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(sql, args);
            while (c.moveToNext()) {
                HashMap<String, String> row = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    row.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                }
                list.add(row);
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "queryTable: " + e.getMessage(), e);
        }
        return list;
    }

    private String safeGet(HashMap<String, String> map, String key) {
        String v = map.get(key);
        return v != null ? v.trim() : "";
    }

    private double safeDouble(HashMap<String, String> map, String key) {
        try {
            String v = safeGet(map, key);
            return v.isEmpty() ? 0.0 : Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int safeInt(HashMap<String, String> map, String key) {
        return safeInt(map, key, 0);
    }

    private int safeInt(HashMap<String, String> map, String key, int defaultVal) {
        try {
            String v = safeGet(map, key);
            return v.isEmpty() ? defaultVal : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private String getCurrentDateTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }

    // ════════════════════════════════════════════════════════════
    // ✅ Overloads مُضافة لدعم الـ Activities الجديدة
    // ════════════════════════════════════════════════════════════

    /** addCustomer بمعاملات مباشرة */
    public long addCustomer(String name, String phone, String address, String notes) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name",    name    != null ? name    : "");
        map.put("phone",   phone   != null ? phone   : "");
        map.put("address", address != null ? address : "");
        map.put("notes",   notes   != null ? notes   : "");
        return addCustomer(map);
    }

    /** updateCustomer بمعامل long id */
    public boolean updateCustomer(long id, String name, String phone, String address, String notes) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name",    name    != null ? name    : "");
        map.put("phone",   phone   != null ? phone   : "");
        map.put("address", address != null ? address : "");
        map.put("notes",   notes   != null ? notes   : "");
        return updateCustomer(String.valueOf(id), map) > 0;
    }

    /** deleteCustomer بمعامل long id */
    public boolean deleteCustomer(long id) {
        return deleteCustomer(String.valueOf(id));
    }

    /** addSupplier بمعاملات مباشرة */
    public long addSupplier(String name, String phone, String address, String notes) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name",    name    != null ? name    : "");
        map.put("phone",   phone   != null ? phone   : "");
        map.put("address", address != null ? address : "");
        map.put("notes",   notes   != null ? notes   : "");
        return addSupplier(map);
    }

    /** updateSupplier بمعامل long id */
    public boolean updateSupplier(long id, String name, String phone, String address, String notes) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name",    name    != null ? name    : "");
        map.put("phone",   phone   != null ? phone   : "");
        map.put("address", address != null ? address : "");
        map.put("notes",   notes   != null ? notes   : "");
        return updateSupplier(String.valueOf(id), map) > 0;
    }

    /** deleteSupplier بمعامل long id */
    public boolean deleteSupplier(long id) {
        return deleteSupplier(String.valueOf(id));
    }

    /**
     * ✅ updateProduct بمعاملات مباشرة (مطلوب من ActivityAddProductActivity)
     */
    public boolean updateProduct(String id, String barcode, String name, String brand,
                                 String unit, double cost, double price, int qty,
                                 String location, String supplier, String expiry,
                                 String imagePath, int reorderLevel, String category, String notes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("barcode",       barcode       != null ? barcode       : "");
            cv.put("name",          name          != null ? name          : "");
            cv.put("brand",         brand         != null ? brand         : "");
            cv.put("unit",          unit          != null ? unit          : "");
            cv.put("cost",          cost);
            cv.put("price",         price);
            cv.put("qty",           qty);
            cv.put("location",      location      != null ? location      : "");
            cv.put("supplier",      supplier      != null ? supplier      : "");
            cv.put("expiry",        expiry        != null ? expiry        : "");
            cv.put("image_path",    imagePath     != null ? imagePath     : "");
            cv.put("reorder_level", reorderLevel);
            cv.put("category",      category      != null ? category      : "");
            cv.put("notes",         notes         != null ? notes         : "");
            return db.update(TABLE_PRODUCTS, cv, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateProduct(params): " + e.getMessage(), e);
            return false;
        }
    }
}
