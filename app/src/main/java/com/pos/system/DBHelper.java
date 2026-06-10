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
    public static final int    DATABASE_VERSION = 9;

    private final Context mContext;

    // أسماء الجداول
    private static final String TABLE_PRODUCTS                = "products";
    private static final String TABLE_SUPPLIERS               = "suppliers";
    private static final String TABLE_CUSTOMERS               = "customers";
    private static final String TABLE_INVOICES                = "invoices";
    private static final String TABLE_INVOICE_ITEMS           = "invoice_items";
    private static final String TABLE_LOCATIONS               = "locations";
    private static final String TABLE_CATEGORIES              = "categories";
    private static final String TABLE_STORE_SETTINGS          = "store_settings";
    private static final String TABLE_PRINTER_SETTINGS        = "printer_settings";
    private static final String TABLE_EXPENSES                = "expenses";
    private static final String TABLE_BACKUP_LOG              = "backup_log";
    private static final String TABLE_SHIFTS                  = "shifts";
    private static final String TABLE_PURCHASE_ORDERS         = "purchase_orders";
    private static final String TABLE_PURCHASE_ORDER_ITEMS    = "purchase_order_items";
    private static final String TABLE_CUSTOMER_DEBT_PAYMENTS  = "customer_debt_payments";
    private static final String TABLE_SUPPLIER_DEBT_PAYMENTS  = "supplier_debt_payments";
    private static final String TABLE_EXPENSE_CATEGORIES      = "expense_categories";
    private static final String TABLE_USERS                     = "users";
    private static final String TABLE_USER_PERMISSIONS          = "user_permissions";
    private static final String TABLE_AUDIT_LOG                 = "audit_log";
    private static final String TABLE_CUSTOMER_CHECKS           = "customer_checks";
    private static final String TABLE_SUPPLIER_CHECKS           = "supplier_checks";
    private static final String TABLE_INSTALLMENT_CONTRACTS     = "installment_contracts";
    private static final String TABLE_INSTALLMENT_PAYMENTS      = "installment_payments";
    private static final String TABLE_CASH_DRAWERS              = "cash_drawers";
    private static final String TABLE_CASH_TRANSACTIONS         = "cash_transactions";
    private static final String TABLE_STOCK_COUNT_SESSIONS      = "stock_count_sessions";
    private static final String TABLE_STOCK_COUNT_ITEMS         = "stock_count_items";
    private static final String TABLE_WALLET_TRANSACTIONS        = "wallet_transactions";
    private static final String TABLE_DIRECT_PAYMENTS            = "direct_payments";
    private static final String TABLE_PRICE_QUOTES               = "price_quotes";
    private static final String TABLE_PRICE_QUOTE_ITEMS          = "price_quote_items";

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
            safeAlter(db, "ALTER TABLE " + TABLE_SUPPLIERS + " ADD COLUMN company TEXT");
            safeAlter(db, "ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN debt REAL DEFAULT 0.0");
            safeAlter(db, "ALTER TABLE " + TABLE_STORE_SETTINGS + " ADD COLUMN is_premium INTEGER DEFAULT 0");
            safeAlter(db, "ALTER TABLE " + TABLE_STORE_SETTINGS + " ADD COLUMN subscription_end TEXT");
            createReturnsTable(db);
            createLoyaltyTable(db);
            createShiftsTable(db);
            createPurchaseOrdersTable(db);
        }
        if (oldVersion < 4) {
            safeAlter(db, "ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN batch_number TEXT DEFAULT ''");
            safeAlter(db, "ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN supplier_reference TEXT DEFAULT ''");
            safeAlter(db, "ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN last_purchase_at TEXT DEFAULT ''");
            safeAlter(db, "ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN total_spent REAL DEFAULT 0.0");
        }
        if (oldVersion < 5) {
            safeAlter(db, "ALTER TABLE " + TABLE_INVOICES + " ADD COLUMN created_by TEXT DEFAULT 'admin'");
            safeAlter(db, "ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN updated_by TEXT DEFAULT 'admin'");
        }
        if (oldVersion < 6) {
            safeAlter(db, "ALTER TABLE " + TABLE_SUPPLIERS + " ADD COLUMN debt REAL DEFAULT 0.0");
            createCustomerDebtPaymentsTable(db);
            createSupplierDebtPaymentsTable(db);
        }
        if (oldVersion < 7) {
            createExpenseCategoriesTable(db);
            createIndexes(db);
        }
        if (oldVersion < 8) {
            createUsersTable(db);
            createAuditLogTable(db);
            createCustomerChecksTable(db);
            createSupplierChecksTable(db);
            createInstallmentContractsTable(db);
            createCashDrawersTable(db);
            createStockCountTable(db);
            insertDefaultAdminUser(db);
            insertDefaultCashDrawer(db);
        }
        if (oldVersion < 9) {
            // New columns on existing tables
            safeAlter(db, "ALTER TABLE invoices ADD COLUMN paid_amount REAL DEFAULT 0.0");
            safeAlter(db, "ALTER TABLE invoices ADD COLUMN remaining_amount REAL DEFAULT 0.0");
            safeAlter(db, "ALTER TABLE invoices ADD COLUMN invoice_note TEXT DEFAULT ''");
            safeAlter(db, "ALTER TABLE invoices ADD COLUMN invoice_date TEXT DEFAULT ''");
            safeAlter(db, "ALTER TABLE invoices ADD COLUMN supplier_id INTEGER DEFAULT 0");
            safeAlter(db, "ALTER TABLE invoices ADD COLUMN supplier_name TEXT DEFAULT ''");
            safeAlter(db, "ALTER TABLE expenses ADD COLUMN expense_type TEXT DEFAULT 'OUT'");
            safeAlter(db, "ALTER TABLE invoice_items ADD COLUMN buy_cost_per_unit REAL DEFAULT 0.0");
            // New tables
            createWalletTransactionsTable(db);
            createDirectPaymentsTable(db);
            createPriceQuotesTable(db);
        }
    }

    /** ALTER TABLE آمن — يتجاهل فقط خطأ "duplicate column name" */
    private static void safeAlter(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (android.database.sqlite.SQLiteException e) {
            String msg = e.getMessage();
            if (msg == null || !msg.contains("duplicate column name")) {
                Log.e(TAG, "safeAlter failed: " + sql + " — " + msg, e);
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON");
            db.execSQL("PRAGMA journal_mode=WAL");
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
            "batch_number TEXT DEFAULT '', " +
            "supplier_reference TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SUPPLIERS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "company TEXT, " +
            "phone TEXT, " +
            "address TEXT, " +
            "debt REAL DEFAULT 0.0, " +
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
            "last_purchase_at TEXT DEFAULT '', " +
            "total_spent REAL DEFAULT 0.0, " +
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
            "created_by TEXT DEFAULT 'admin', " +
            "paid_amount REAL DEFAULT 0.0, " +
            "remaining_amount REAL DEFAULT 0.0, " +
            "invoice_note TEXT DEFAULT '', " +
            "invoice_date TEXT DEFAULT '', " +
            "supplier_id INTEGER DEFAULT 0, " +
            "supplier_name TEXT DEFAULT '', " +
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
            "buy_cost_per_unit REAL DEFAULT 0.0, " +
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
            "expense_type TEXT DEFAULT 'OUT', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BACKUP_LOG + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_path TEXT NOT NULL, " +
            "file_size INTEGER DEFAULT 0, " +
            "backup_type TEXT DEFAULT 'local', " +
            "status TEXT DEFAULT 'success', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        insertDefaultSettings(db);
        createReturnsTable(db);
        createLoyaltyTable(db);
        createShiftsTable(db);
        createPurchaseOrdersTable(db);
        createCustomerDebtPaymentsTable(db);
        createSupplierDebtPaymentsTable(db);
        createExpenseCategoriesTable(db);
        createIndexes(db);
        createUsersTable(db);
        createAuditLogTable(db);
        createCustomerChecksTable(db);
        createSupplierChecksTable(db);
        createInstallmentContractsTable(db);
        createCashDrawersTable(db);
        createStockCountTable(db);
        createWalletTransactionsTable(db);
        createDirectPaymentsTable(db);
        createPriceQuotesTable(db);
        insertDefaultAdminUser(db);
        insertDefaultCashDrawer(db);
    }

    private void createExpenseCategoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSE_CATEGORIES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL UNIQUE, " +
            "icon TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        // الفئات الافتراضية
        String[][] defaults = {
            {"إيجار", "🏠"}, {"كهرباء وماء", "💡"}, {"رواتب", "💼"},
            {"مواصلات", "🚗"}, {"تسويق وإعلان", "📣"}, {"صيانة", "🔧"},
            {"مستلزمات مكتبية", "📋"}, {"ضرائب ورسوم", "🏛️"},
            {"خسارة وتالف", "❌"}, {"متنوع", "📦"}
        };
        for (String[] row : defaults) {
            try {
                ContentValues cv = new ContentValues();
                cv.put("name", row[0]);
                cv.put("icon", row[1]);
                db.insertWithOnConflict(TABLE_EXPENSE_CATEGORIES, null, cv,
                    SQLiteDatabase.CONFLICT_IGNORE);
            } catch (Exception ignored) {}
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        // indexes لتسريع الاستعلامات الأكثر استخداماً
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_qty ON products(qty)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_expiry ON products(expiry)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_date ON invoices(created_at)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_debt ON customers(debt)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_suppliers_debt ON suppliers(debt)"); } catch (Exception ignored) {}
    }

    private void dropAllTables(SQLiteDatabase db) {
        // Drop child tables before parent tables to respect foreign key constraints
        String[] tables = {
            TABLE_PURCHASE_ORDER_ITEMS, TABLE_PURCHASE_ORDERS,
            "return_items", "returns",
            "loyalty_points",
            TABLE_SHIFTS,
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
            {"tax_rate", "14.0"}, {"tax_enabled", "true"},
            {"currency", "ج.م"}, {"country_code", "EG"},
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
            cv.put("barcode",            safeGet(product, "barcode"));
            cv.put("name",               safeGet(product, "name"));
            cv.put("brand",              safeGet(product, "brand"));
            cv.put("unit",               safeGet(product, "unit"));
            cv.put("cost",               safeDouble(product, "cost"));
            cv.put("price",              safeDouble(product, "price"));
            cv.put("qty",                safeInt(product, "qty"));
            cv.put("location",           safeGet(product, "location"));
            cv.put("supplier",           safeGet(product, "supplier"));
            cv.put("expiry",             safeGet(product, "expiry"));
            cv.put("image_path",         safeGet(product, "image_path"));
            cv.put("reorder_level",      safeInt(product, "reorder_level", 5));
            cv.put("category",           safeGet(product, "category"));
            cv.put("notes",              safeGet(product, "notes"));
            cv.put("batch_number",       safeGet(product, "batch_number"));
            cv.put("supplier_reference", safeGet(product, "supplier_reference"));
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
                                 String category, String notes, String batchNumber,
                                 String supplierReference) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("barcode",            barcode            != null ? barcode            : "");
            cv.put("name",               name               != null ? name               : "");
            cv.put("brand",              brand              != null ? brand              : "");
            cv.put("unit",               unit               != null ? unit               : "");
            cv.put("cost",               cost);
            cv.put("price",              price);
            cv.put("qty",                qty);
            cv.put("location",           location           != null ? location           : "");
            cv.put("supplier",           supplier           != null ? supplier           : "");
            cv.put("expiry",             expiry             != null ? expiry             : "");
            cv.put("image_path",         imagePath          != null ? imagePath          : "");
            cv.put("reorder_level",      reorderLevel);
            cv.put("category",           category           != null ? category           : "");
            cv.put("notes",              notes              != null ? notes              : "");
            cv.put("batch_number",       batchNumber        != null ? batchNumber        : "");
            cv.put("supplier_reference", supplierReference  != null ? supplierReference  : "");
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
            cv.put("barcode",            safeGet(product, "barcode"));
            cv.put("name",               safeGet(product, "name"));
            cv.put("brand",              safeGet(product, "brand"));
            cv.put("unit",               safeGet(product, "unit"));
            cv.put("cost",               safeDouble(product, "cost"));
            cv.put("price",              safeDouble(product, "price"));
            cv.put("qty",                safeInt(product, "qty"));
            cv.put("location",           safeGet(product, "location"));
            cv.put("supplier",           safeGet(product, "supplier"));
            cv.put("expiry",             safeGet(product, "expiry"));
            cv.put("image_path",         safeGet(product, "image_path"));
            cv.put("reorder_level",      safeInt(product, "reorder_level", 5));
            cv.put("category",           safeGet(product, "category"));
            cv.put("notes",              safeGet(product, "notes"));
            cv.put("batch_number",       safeGet(product, "batch_number"));
            cv.put("supplier_reference", safeGet(product, "supplier_reference"));
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
    public int getTotalCustomersCount() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CUSTOMERS, null);
            int count = 0;
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

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
        String custId = invoice.containsKey("customer_id") ?
            String.valueOf(((Number) invoice.get("customer_id")).intValue()) : "0";
        long invoiceId = -1;
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

            invoiceId = db.insert(TABLE_INVOICES, null, cv);
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
        } catch (Exception e) {
            Log.e(TAG, "addInvoice: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
        if (invoiceId > 0) updateCustomerStats(custId);
        return invoiceId;
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
        long invoiceId = -1;
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

            invoiceId = db.insert(TABLE_INVOICES, null, cv);
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
        } catch (Exception e) {
            Log.e(TAG, "createInvoiceWithDetails: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
        if (invoiceId > 0) updateCustomerStats(customerId);
        return invoiceId;
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
        try { result.put("subtotal",    Double.parseDouble(row.getOrDefault("subtotal",  "0"))); } catch (Exception ignored) {}
        try { result.put("total",       Double.parseDouble(row.getOrDefault("total",     "0"))); } catch (Exception ignored) {}
        try { result.put("discount",    Double.parseDouble(row.getOrDefault("discount",  "0"))); } catch (Exception ignored) {}
        try { result.put("tax",         Double.parseDouble(row.getOrDefault("tax",       "0"))); } catch (Exception ignored) {}
        try { result.put("customer_id", Integer.parseInt(row.getOrDefault("customer_id","0"))); } catch (Exception ignored) {}
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
    // Expense Categories
    // ════════════════════════════════════════════════════════════
    public List<HashMap<String, String>> getExpenseCategories() {
        return queryTable("SELECT * FROM " + TABLE_EXPENSE_CATEGORIES + " ORDER BY name ASC", null);
    }

    public long addExpenseCategory(String name, String icon) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("icon", icon != null ? icon : "");
            return getWritableDatabase().insertWithOnConflict(
                TABLE_EXPENSE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            Log.e(TAG, "addExpenseCategory: " + e.getMessage(), e);
            return -1;
        }
    }

    // ════════════════════════════════════════════════════════════
    // COGS — تكلفة البضاعة المباعة والربح الحقيقي
    // ════════════════════════════════════════════════════════════

    /**
     * تحسب: إجمالي المبيعات، تكلفة البضاعة المباعة (COGS)،
     * مجمل الربح (Gross Profit)، المصروفات، وصافي الربح.
     * المعادلة: Gross Profit = Revenue - COGS
     *           Net Profit   = Gross Profit - Expenses
     */
    public HashMap<String, Double> getFullProfitReport(String startDate, String endDate) {
        HashMap<String, Double> result = new HashMap<>();
        result.put("revenue", 0.0);
        result.put("cogs", 0.0);
        result.put("gross_profit", 0.0);
        result.put("expenses", 0.0);
        result.put("net_profit", 0.0);
        result.put("invoice_count", 0.0);

        try {
            SQLiteDatabase db = getReadableDatabase();

            // 1. إجمالي المبيعات وعدد الفواتير
            Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(total), 0), COUNT(*) FROM invoices " +
                "WHERE DATE(created_at) BETWEEN ? AND ?",
                new String[]{startDate, endDate});
            if (c.moveToFirst()) {
                result.put("revenue",       c.getDouble(0));
                result.put("invoice_count", (double) c.getInt(1));
            }
            c.close();

            // 2. COGS: نضرب سعر التكلفة لكل منتج × الكمية المباعة
            c = db.rawQuery(
                "SELECT COALESCE(SUM(p.cost * ii.qty), 0) " +
                "FROM invoice_items ii " +
                "JOIN invoices i ON ii.invoice_id = i.id " +
                "LEFT JOIN products p ON CAST(ii.product_id AS INTEGER) = p.id " +
                "WHERE DATE(i.created_at) BETWEEN ? AND ? " +
                "AND p.cost IS NOT NULL AND p.cost > 0",
                new String[]{startDate, endDate});
            if (c.moveToFirst()) result.put("cogs", c.getDouble(0));
            c.close();

            // 3. المصروفات
            c = db.rawQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
                "WHERE DATE(created_at) BETWEEN ? AND ?",
                new String[]{startDate, endDate});
            if (c.moveToFirst()) result.put("expenses", c.getDouble(0));
            c.close();

            // 4. الحسابات
            double revenue = result.get("revenue");
            double cogs    = result.get("cogs");
            double exp     = result.get("expenses");
            result.put("gross_profit", revenue - cogs);
            result.put("net_profit",   revenue - cogs - exp);

        } catch (Exception e) {
            Log.e(TAG, "getFullProfitReport: " + e.getMessage(), e);
        }
        return result;
    }

    /** حفظ تاريخ بدء التجربة في قاعدة البيانات */
    public void saveTrialStart(long timestamp) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("key",   "trial_start_ts");
            cv.put("value", String.valueOf(timestamp));
            getWritableDatabase().insertWithOnConflict(
                TABLE_STORE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "saveTrialStart: " + e.getMessage(), e);
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

    public String getStoreSetting(String key) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT value FROM " + TABLE_STORE_SETTINGS + " WHERE key=?", new String[]{key});
            String v = null;
            if (c.moveToFirst()) v = c.getString(0);
            c.close();
            return v;
        } catch (Exception e) {
            return null;
        }
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
            if (!"true".equalsIgnoreCase(settings.getOrDefault("is_premium", "false"))) return false;
            // Lifetime purchases have no expiry; monthly/yearly have a date
            String planId = settings.getOrDefault("plan_id", "");
            if ("premium_lifetime".equalsIgnoreCase(planId)) return true;
            String end = settings.getOrDefault("subscription_end", "");
            if (end.isEmpty()) return true; // legacy entries without date → keep unlocked
            // "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss"
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date expiry = sdf.parse(end.length() > 10 ? end.substring(0, 10) : end);
            return expiry != null && !expiry.before(new java.util.Date());
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

    private double safeDouble(HashMap<String, String> map, String key, double def) {
        try { String v = map.getOrDefault(key, ""); return v.isEmpty() ? def : Double.parseDouble(v); }
        catch (Exception e) { return def; }
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
     * ✅ getLowStockProducts no-arg overload (مطلوب من LowStockWorker)
     */
    public List<HashMap<String, String>> getLowStockProducts() {
        return getLowStockProducts(0);
    }

    /**
     * ✅ updateProduct بمعاملات مباشرة (مطلوب من ActivityAddProductActivity)
     */
    public boolean updateProduct(String id, String barcode, String name, String brand,
                                 String unit, double cost, double price, int qty,
                                 String location, String supplier, String expiry,
                                 String imagePath, int reorderLevel, String category, String notes,
                                 String batchNumber, String supplierReference) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("barcode",            barcode           != null ? barcode           : "");
            cv.put("name",               name              != null ? name              : "");
            cv.put("brand",              brand             != null ? brand             : "");
            cv.put("unit",               unit              != null ? unit              : "");
            cv.put("cost",               cost);
            cv.put("price",              price);
            cv.put("qty",                qty);
            cv.put("location",           location          != null ? location          : "");
            cv.put("supplier",           supplier          != null ? supplier          : "");
            cv.put("expiry",             expiry            != null ? expiry            : "");
            cv.put("image_path",         imagePath         != null ? imagePath         : "");
            cv.put("reorder_level",      reorderLevel);
            cv.put("category",           category          != null ? category          : "");
            cv.put("notes",              notes             != null ? notes             : "");
            cv.put("batch_number",       batchNumber       != null ? batchNumber       : "");
            cv.put("supplier_reference", supplierReference != null ? supplierReference : "");
            return db.update(TABLE_PRODUCTS, cv, "id=?", new String[]{id}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateProduct(params): " + e.getMessage(), e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // RETURNS & REFUNDS
    // ════════════════════════════════════════════════════════════

    private static final String TABLE_RETURNS = "returns";
    private static final String TABLE_RETURN_ITEMS = "return_items";

    public void createReturnsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RETURNS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "return_number TEXT UNIQUE NOT NULL,"
            + "original_invoice_id INTEGER,"
            + "original_invoice_number TEXT,"
            + "customer_id TEXT DEFAULT '0',"
            + "customer_name TEXT DEFAULT '',"
            + "total_refund REAL DEFAULT 0,"
            + "reason TEXT DEFAULT '',"
            + "status TEXT DEFAULT 'completed',"
            + "refund_method TEXT DEFAULT 'cash',"
            + "notes TEXT DEFAULT '',"
            + "created_at TEXT NOT NULL"
            + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RETURN_ITEMS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "return_id INTEGER NOT NULL,"
            + "product_id TEXT DEFAULT '',"
            + "barcode TEXT DEFAULT '',"
            + "name TEXT NOT NULL,"
            + "price REAL DEFAULT 0,"
            + "qty INTEGER DEFAULT 1,"
            + "total REAL DEFAULT 0,"
            + "FOREIGN KEY(return_id) REFERENCES " + TABLE_RETURNS + "(id)"
            + ")");
    }

    public long createReturn(String returnNumber, long originalInvoiceId,
                             String originalInvoiceNumber, String customerId,
                             String customerName, List<HashMap<String, String>> items,
                             double totalRefund, String reason, String refundMethod) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues rv = new ContentValues();
            rv.put("return_number", returnNumber);
            rv.put("original_invoice_id", originalInvoiceId);
            rv.put("original_invoice_number", originalInvoiceNumber);
            rv.put("customer_id", customerId != null ? customerId : "0");
            rv.put("customer_name", customerName != null ? customerName : "");
            rv.put("total_refund", totalRefund);
            rv.put("reason", reason != null ? reason : "");
            rv.put("refund_method", refundMethod != null ? refundMethod : "cash");
            rv.put("created_at", getCurrentDateTime());
            long returnId = db.insert(TABLE_RETURNS, null, rv);
            if (returnId < 0) throw new Exception("Failed to insert return");

            for (HashMap<String, String> item : items) {
                ContentValues ri = new ContentValues();
                ri.put("return_id", returnId);
                ri.put("product_id", item.getOrDefault("product_id", ""));
                ri.put("barcode", item.getOrDefault("barcode", ""));
                ri.put("name", item.getOrDefault("name", ""));
                ri.put("price", safeDouble(item, "price", 0));
                ri.put("qty", safeInt(item, "qty", 1));
                ri.put("total", safeDouble(item, "total", 0));
                db.insert(TABLE_RETURN_ITEMS, null, ri);
                // Restore stock
                String productId = item.getOrDefault("product_id", "");
                int qty = safeInt(item, "qty", 1);
                if (!productId.isEmpty() && qty > 0) {
                    db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET qty = qty + ? WHERE id = ?",
                        new Object[]{qty, productId});
                }
            }
            db.setTransactionSuccessful();
            return returnId;
        } catch (Exception e) {
            Log.e(TAG, "createReturn: " + e.getMessage(), e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public List<HashMap<String, String>> getReturns() {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_RETURNS + " ORDER BY created_at DESC", null);
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getReturns: " + e.getMessage()); }
        return list;
    }

    public List<HashMap<String, String>> getReturnItems(long returnId) {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_RETURN_ITEMS + " WHERE return_id=?",
                new String[]{String.valueOf(returnId)});
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getReturnItems: " + e.getMessage()); }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    // LOYALTY POINTS
    // ════════════════════════════════════════════════════════════

    private static final String TABLE_LOYALTY = "loyalty_points";

    public void createLoyaltyTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOYALTY + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "customer_id TEXT NOT NULL,"
            + "customer_name TEXT DEFAULT '',"
            + "points INTEGER DEFAULT 0,"
            + "type TEXT DEFAULT 'earn',"
            + "reference_id TEXT DEFAULT '',"
            + "notes TEXT DEFAULT '',"
            + "created_at TEXT NOT NULL"
            + ")");
    }

    public int getCustomerLoyaltyPoints(String customerId) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT SUM(CASE WHEN type='earn' THEN points ELSE -points END) FROM "
                + TABLE_LOYALTY + " WHERE customer_id=?", new String[]{customerId});
            if (c.moveToFirst()) { int pts = c.getInt(0); c.close(); return Math.max(0, pts); }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getLoyaltyPoints: " + e.getMessage()); }
        return 0;
    }

    public boolean addLoyaltyPoints(String customerId, String customerName,
                                     int points, String type, String referenceId) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("customer_name", customerName != null ? customerName : "");
            cv.put("points", Math.abs(points));
            cv.put("type", type); // "earn" or "redeem"
            cv.put("reference_id", referenceId != null ? referenceId : "");
            cv.put("created_at", getCurrentDateTime());
            return getWritableDatabase().insert(TABLE_LOYALTY, null, cv) > 0;
        } catch (Exception e) { Log.e(TAG, "addLoyaltyPoints: " + e.getMessage()); return false; }
    }

    // ════════════════════════════════════════════════════════════
    // CUSTOMER DEBT MANAGEMENT
    // ════════════════════════════════════════════════════════════

    public boolean updateCustomerDebt(String customerId, double debtDelta) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("UPDATE " + TABLE_CUSTOMERS + " SET debt = debt + ? WHERE id = ?",
                new Object[]{debtDelta, customerId});
            return true;
        } catch (Exception e) { Log.e(TAG, "updateCustomerDebt: " + e.getMessage()); return false; }
    }

    public double getCustomerDebt(String customerId) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT debt FROM " + TABLE_CUSTOMERS + " WHERE id=?",
                new String[]{customerId});
            if (c.moveToFirst()) { double d = c.getDouble(0); c.close(); return d; }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getCustomerDebt: " + e.getMessage()); }
        return 0;
    }

    /** سداد جزء من دين العميل مع تسجيل حركة الدفع */
    public boolean settleCustomerDebt(String customerId, double amount, String note) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE " + TABLE_CUSTOMERS + " SET debt = MAX(0, debt - ?) WHERE id = ?",
                new Object[]{amount, customerId});
            String customerName = "";
            Cursor c = db.rawQuery("SELECT name FROM " + TABLE_CUSTOMERS + " WHERE id=?",
                new String[]{customerId});
            if (c.moveToFirst()) customerName = c.getString(0);
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("customer_id",   Integer.parseInt(customerId));
            cv.put("customer_name", customerName);
            cv.put("amount",        amount);
            cv.put("type",          "payment");
            cv.put("note",          note != null ? note : "");
            db.insert(TABLE_CUSTOMER_DEBT_PAYMENTS, null, cv);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "settleCustomerDebt: " + e.getMessage());
            return false;
        } finally { db.endTransaction(); }
    }

    /** backward compatible */
    public boolean settleCustomerDebt(String customerId, double amount) {
        return settleCustomerDebt(customerId, amount, "");
    }

    /** تسجيل دين جديد على العميل مع حركة */
    public boolean addCustomerDebt(String customerId, double amount, String note) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE " + TABLE_CUSTOMERS + " SET debt = debt + ? WHERE id = ?",
                new Object[]{amount, customerId});
            String customerName = "";
            Cursor c = db.rawQuery("SELECT name FROM " + TABLE_CUSTOMERS + " WHERE id=?",
                new String[]{customerId});
            if (c.moveToFirst()) customerName = c.getString(0);
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("customer_id",   Integer.parseInt(customerId));
            cv.put("customer_name", customerName);
            cv.put("amount",        amount);
            cv.put("type",          "debt");
            cv.put("note",          note != null ? note : "");
            db.insert(TABLE_CUSTOMER_DEBT_PAYMENTS, null, cv);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "addCustomerDebt: " + e.getMessage());
            return false;
        } finally { db.endTransaction(); }
    }

    public List<HashMap<String, String>> getCustomerDebtPayments(String customerId) {
        return queryTable(
            "SELECT * FROM " + TABLE_CUSTOMER_DEBT_PAYMENTS +
            " WHERE customer_id=? ORDER BY created_at DESC",
            new String[]{customerId});
    }

    public double getTotalCustomerDebt() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(debt) FROM " + TABLE_CUSTOMERS + " WHERE debt > 0", null);
            double total = 0;
            if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
            c.close();
            return total;
        } catch (Exception e) { return 0; }
    }

    public List<HashMap<String, String>> getCustomersWithDebt() {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_CUSTOMERS
                + " WHERE debt > 0 ORDER BY debt DESC", null);
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getCustomersWithDebt: " + e.getMessage()); }
        return list;
    }

    private void createCustomerDebtPaymentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOMER_DEBT_PAYMENTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "customer_id INTEGER NOT NULL, " +
            "customer_name TEXT DEFAULT '', " +
            "amount REAL NOT NULL, " +
            "type TEXT DEFAULT 'payment', " +
            "note TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    // ════════════════════════════════════════════════════════════
    // SUPPLIER DEBT MANAGEMENT
    // ════════════════════════════════════════════════════════════

    public double getSupplierDebt(String supplierId) {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT debt FROM " + TABLE_SUPPLIERS + " WHERE id=?",
                new String[]{supplierId});
            if (c.moveToFirst()) { double d = c.getDouble(0); c.close(); return d; }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getSupplierDebt: " + e.getMessage()); }
        return 0;
    }

    /** تسجيل دين جديد على المتجر لصالح المورد (اشتراء بالآجل) */
    public boolean addSupplierDebt(String supplierId, double amount, String note) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE " + TABLE_SUPPLIERS + " SET debt = debt + ? WHERE id = ?",
                new Object[]{amount, supplierId});
            String supplierName = "";
            Cursor c = db.rawQuery("SELECT name FROM " + TABLE_SUPPLIERS + " WHERE id=?",
                new String[]{supplierId});
            if (c.moveToFirst()) supplierName = c.getString(0);
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("supplier_id",   Integer.parseInt(supplierId));
            cv.put("supplier_name", supplierName);
            cv.put("amount",        amount);
            cv.put("type",          "debt");
            cv.put("note",          note != null ? note : "");
            db.insert(TABLE_SUPPLIER_DEBT_PAYMENTS, null, cv);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "addSupplierDebt: " + e.getMessage());
            return false;
        } finally { db.endTransaction(); }
    }

    /** سداد جزء من الدين للمورد مع تسجيل حركة الدفع */
    public boolean settleSupplierDebt(String supplierId, double amount, String note) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE " + TABLE_SUPPLIERS + " SET debt = MAX(0, debt - ?) WHERE id = ?",
                new Object[]{amount, supplierId});
            String supplierName = "";
            Cursor c = db.rawQuery("SELECT name FROM " + TABLE_SUPPLIERS + " WHERE id=?",
                new String[]{supplierId});
            if (c.moveToFirst()) supplierName = c.getString(0);
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("supplier_id",   Integer.parseInt(supplierId));
            cv.put("supplier_name", supplierName);
            cv.put("amount",        amount);
            cv.put("type",          "payment");
            cv.put("note",          note != null ? note : "");
            db.insert(TABLE_SUPPLIER_DEBT_PAYMENTS, null, cv);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "settleSupplierDebt: " + e.getMessage());
            return false;
        } finally { db.endTransaction(); }
    }

    public List<HashMap<String, String>> getSuppliersWithDebt() {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_SUPPLIERS + " WHERE debt > 0 ORDER BY debt DESC", null);
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getSuppliersWithDebt: " + e.getMessage()); }
        return list;
    }

    public List<HashMap<String, String>> getSupplierDebtPayments(String supplierId) {
        return queryTable(
            "SELECT * FROM " + TABLE_SUPPLIER_DEBT_PAYMENTS +
            " WHERE supplier_id=? ORDER BY created_at DESC",
            new String[]{supplierId});
    }

    public double getTotalSupplierDebt() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(debt) FROM " + TABLE_SUPPLIERS + " WHERE debt > 0", null);
            double total = 0;
            if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
            c.close();
            return total;
        } catch (Exception e) { return 0; }
    }

    private void createSupplierDebtPaymentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SUPPLIER_DEBT_PAYMENTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "supplier_id INTEGER NOT NULL, " +
            "supplier_name TEXT DEFAULT '', " +
            "amount REAL NOT NULL, " +
            "type TEXT DEFAULT 'payment', " +
            "note TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    // ════════════════════════════════════════════════════════════
    // SHIFTS
    // ════════════════════════════════════════════════════════════

    public void createShiftsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SHIFTS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "opening_cash REAL DEFAULT 0,"
            + "closing_cash REAL DEFAULT 0,"
            + "total_sales REAL DEFAULT 0,"
            + "invoice_count INTEGER DEFAULT 0,"
            + "status TEXT DEFAULT 'open',"
            + "opened_at TEXT NOT NULL,"
            + "closed_at TEXT DEFAULT ''"
            + ")");
    }

    public long openShift(double openingCash) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("opening_cash", openingCash);
            cv.put("status", "open");
            cv.put("opened_at", getCurrentDateTime());
            return db.insert(TABLE_SHIFTS, null, cv);
        } catch (Exception e) { Log.e(TAG, "openShift: " + e.getMessage()); return -1; }
    }

    public boolean closeShift(long shiftId, double closingCash) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            HashMap<String, String> shift = getShiftById(shiftId);
            if (shift == null) return false;
            String openedAt = shift.getOrDefault("opened_at", getCurrentDateTime());
            // Calculate sales during this shift
            HashMap<String, Object> sales = getShiftSales(openedAt, getCurrentDateTime());
            double totalSales = sales.get("total_sales") != null ? ((Number) sales.get("total_sales")).doubleValue() : 0;
            int invoiceCount = sales.get("invoice_count") != null ? ((Number) sales.get("invoice_count")).intValue() : 0;
            ContentValues cv = new ContentValues();
            cv.put("closing_cash", closingCash);
            cv.put("status", "closed");
            cv.put("closed_at", getCurrentDateTime());
            cv.put("total_sales", totalSales);
            cv.put("invoice_count", invoiceCount);
            return db.update(TABLE_SHIFTS, cv, "id=?", new String[]{String.valueOf(shiftId)}) > 0;
        } catch (Exception e) { Log.e(TAG, "closeShift: " + e.getMessage()); return false; }
    }

    public HashMap<String, String> getCurrentShift() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_SHIFTS
                + " WHERE status='open' ORDER BY opened_at DESC LIMIT 1", null);
            if (c.moveToFirst()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                c.close();
                return m;
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getCurrentShift: " + e.getMessage()); }
        return null;
    }

    public HashMap<String, String> getShiftById(long id) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_SHIFTS + " WHERE id=?",
                new String[]{String.valueOf(id)});
            if (c.moveToFirst()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                c.close();
                return m;
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getShiftById: " + e.getMessage()); }
        return null;
    }

    public List<HashMap<String, String>> getAllShifts() {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_SHIFTS + " ORDER BY opened_at DESC", null);
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getAllShifts: " + e.getMessage()); }
        return list;
    }

    public HashMap<String, Object> getShiftSales(String startDateTime, String endDateTime) {
        HashMap<String, Object> result = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(total),0), COUNT(*) FROM " + TABLE_INVOICES
                + " WHERE created_at BETWEEN ? AND ?",
                new String[]{startDateTime, endDateTime});
            if (c.moveToFirst()) {
                result.put("total_sales", c.getDouble(0));
                result.put("invoice_count", c.getInt(1));
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getShiftSales: " + e.getMessage()); }
        if (!result.containsKey("total_sales")) result.put("total_sales", 0.0);
        if (!result.containsKey("invoice_count")) result.put("invoice_count", 0);
        return result;
    }

    // ════════════════════════════════════════════════════════════
    // PURCHASE ORDERS
    // ════════════════════════════════════════════════════════════

    public void createPurchaseOrdersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PURCHASE_ORDERS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "po_number TEXT UNIQUE NOT NULL,"
            + "supplier_id TEXT DEFAULT '',"
            + "supplier_name TEXT DEFAULT '',"
            + "total REAL DEFAULT 0,"
            + "notes TEXT DEFAULT '',"
            + "status TEXT DEFAULT 'pending',"
            + "created_at TEXT NOT NULL"
            + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PURCHASE_ORDER_ITEMS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "po_id INTEGER NOT NULL,"
            + "product_name TEXT NOT NULL,"
            + "qty INTEGER DEFAULT 1,"
            + "cost REAL DEFAULT 0,"
            + "total REAL DEFAULT 0,"
            + "FOREIGN KEY(po_id) REFERENCES " + TABLE_PURCHASE_ORDERS + "(id)"
            + ")");
    }

    public long addPurchaseOrder(String supplierName, String supplierId,
                                  List<HashMap<String, String>> items,
                                  double total, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String poNumber = "PO-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                java.util.Locale.US).format(new java.util.Date());
            ContentValues pv = new ContentValues();
            pv.put("po_number", poNumber);
            pv.put("supplier_name", supplierName != null ? supplierName : "");
            pv.put("supplier_id", supplierId != null ? supplierId : "");
            pv.put("total", total);
            pv.put("notes", notes != null ? notes : "");
            pv.put("status", "pending");
            pv.put("created_at", getCurrentDateTime());
            long poId = db.insert(TABLE_PURCHASE_ORDERS, null, pv);
            if (poId < 0) throw new Exception("Failed to insert PO");
            for (HashMap<String, String> item : items) {
                ContentValues iv = new ContentValues();
                iv.put("po_id", poId);
                iv.put("product_name", item.getOrDefault("name", ""));
                iv.put("qty", safeInt(item, "qty", 1));
                iv.put("cost", safeDouble(item, "cost", 0));
                iv.put("total", safeDouble(item, "total", 0));
                db.insert(TABLE_PURCHASE_ORDER_ITEMS, null, iv);
            }
            db.setTransactionSuccessful();
            return poId;
        } catch (Exception e) {
            Log.e(TAG, "addPurchaseOrder: " + e.getMessage()); return -1;
        } finally { db.endTransaction(); }
    }

    public List<HashMap<String, String>> getPurchaseOrders() {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PURCHASE_ORDERS
                + " ORDER BY created_at DESC", null);
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getPurchaseOrders: " + e.getMessage()); }
        return list;
    }

    public List<HashMap<String, String>> getPurchaseOrderItems(long poId) {
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PURCHASE_ORDER_ITEMS + " WHERE po_id=?",
                new String[]{String.valueOf(poId)});
            while (c.moveToNext()) {
                HashMap<String, String> m = new HashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++)
                    m.put(c.getColumnName(i), c.getString(i) != null ? c.getString(i) : "");
                list.add(m);
            }
            c.close();
        } catch (Exception e) { Log.e(TAG, "getPurchaseOrderItems: " + e.getMessage()); }
        return list;
    }

    public boolean receivePurchaseOrder(long poId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<HashMap<String, String>> items = getPurchaseOrderItems(poId);
            for (HashMap<String, String> item : items) {
                String productName = item.getOrDefault("product_name", "");
                int qty = safeInt(item, "qty", 1);
                // Try to find product by name and increase stock
                Cursor c = db.rawQuery("SELECT id FROM " + TABLE_PRODUCTS
                    + " WHERE name=? LIMIT 1", new String[]{productName});
                if (c.moveToFirst()) {
                    String productId = c.getString(0);
                    db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET qty = qty + ? WHERE id = ?",
                        new Object[]{qty, productId});
                }
                c.close();
            }
            ContentValues cv = new ContentValues();
            cv.put("status", "received");
            db.update(TABLE_PURCHASE_ORDERS, cv, "id=?", new String[]{String.valueOf(poId)});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "receivePurchaseOrder: " + e.getMessage()); return false;
        } finally { db.endTransaction(); }
    }

    // ════════════════════════════════════════════════════════════
    // Analytics Methods — Phase 4
    // ════════════════════════════════════════════════════════════

    /** المنتجات التي تنتهي صلاحيتها خلال X يوم */
    public List<HashMap<String, String>> getExpiringProducts(int days) {
        return queryProducts(
            "SELECT * FROM " + TABLE_PRODUCTS +
            " WHERE expiry != '' AND expiry IS NOT NULL" +
            " AND DATE(expiry) BETWEEN DATE('now') AND DATE('now', '+' || ? || ' days')" +
            " AND qty > 0 ORDER BY expiry ASC",
            new String[]{String.valueOf(days)});
    }

    /** المنتجات التي لم تُباع خلال X يوم (مخزون راكد) */
    public List<HashMap<String, String>> getDeadStockProducts(int days) {
        return queryProducts(
            "SELECT p.* FROM " + TABLE_PRODUCTS + " p" +
            " WHERE p.qty > 0" +
            " AND p.id NOT IN (" +
            "  SELECT DISTINCT CAST(ii.product_id AS INTEGER) FROM " + TABLE_INVOICE_ITEMS + " ii" +
            "  JOIN " + TABLE_INVOICES + " i ON ii.invoice_id = i.id" +
            "  WHERE DATE(i.created_at) >= DATE('now', '-' || ? || ' days')" +
            "  AND ii.product_id != '' AND ii.product_id IS NOT NULL" +
            " ) ORDER BY p.qty DESC",
            new String[]{String.valueOf(days)});
    }

    /** أكثر منتج مبيعاً هذا الأسبوع */
    public HashMap<String, String> getTopSellerThisWeek() {
        List<HashMap<String, String>> list = queryTable(
            "SELECT ii.name, ii.product_id, SUM(ii.qty) as total_qty, SUM(ii.total) as total_sales" +
            " FROM " + TABLE_INVOICE_ITEMS + " ii" +
            " JOIN " + TABLE_INVOICES + " i ON ii.invoice_id = i.id" +
            " WHERE DATE(i.created_at) >= DATE('now', '-7 days')" +
            " GROUP BY ii.product_id, ii.name ORDER BY total_qty DESC LIMIT 1",
            null);
        return list.isEmpty() ? null : list.get(0);
    }

    /** أفضل عميل هذا الشهر (حسب إجمالي المشتريات) */
    public HashMap<String, String> getTopCustomerThisMonth() {
        List<HashMap<String, String>> list = queryTable(
            "SELECT c.id, c.name, c.phone, CAST(SUM(i.total) AS TEXT) as month_spent" +
            " FROM " + TABLE_CUSTOMERS + " c" +
            " JOIN " + TABLE_INVOICES + " i ON CAST(i.customer_id AS INTEGER) = c.id" +
            " WHERE strftime('%Y-%m', i.created_at) = strftime('%Y-%m', 'now')" +
            " AND c.id != 0" +
            " GROUP BY c.id ORDER BY SUM(i.total) DESC LIMIT 1",
            null);
        return list.isEmpty() ? null : list.get(0);
    }

    /** المنتجات التي انتهت صلاحيتها بالفعل (وما زال لديها مخزون) */
    public List<HashMap<String, String>> getExpiredProducts() {
        return queryProducts(
            "SELECT * FROM " + TABLE_PRODUCTS +
            " WHERE expiry != '' AND expiry IS NOT NULL" +
            " AND DATE(expiry) < DATE('now')" +
            " AND qty > 0 ORDER BY expiry ASC",
            null);
    }

    /** ربحية كل منتج خلال فترة معينة (مرتبة من الأضعف للأقوى هامشاً) */
    public List<HashMap<String, String>> getProfitByProduct(String startDate, String endDate) {
        return queryTable(
            "SELECT ii.name, ii.product_id," +
            " CAST(SUM(ii.qty) AS TEXT) as total_qty," +
            " CAST(ROUND(SUM(ii.total), 2) AS TEXT) as revenue," +
            " CAST(ROUND(COALESCE(SUM(p.cost * ii.qty), 0), 2) AS TEXT) as cogs," +
            " CAST(ROUND(SUM(ii.total) - COALESCE(SUM(p.cost * ii.qty), 0), 2) AS TEXT) as profit," +
            " CAST(CASE WHEN SUM(ii.total) > 0 THEN" +
            "   ROUND(((SUM(ii.total) - COALESCE(SUM(p.cost * ii.qty), 0)) / SUM(ii.total)) * 100, 1)" +
            " ELSE 0 END AS TEXT) as margin_pct" +
            " FROM invoice_items ii" +
            " JOIN invoices i ON ii.invoice_id = i.id" +
            " LEFT JOIN products p ON CAST(ii.product_id AS INTEGER) = p.id" +
            " WHERE DATE(i.created_at) BETWEEN ? AND ?" +
            " GROUP BY ii.product_id, ii.name HAVING SUM(ii.total) > 0" +
            " ORDER BY CAST(margin_pct AS REAL) ASC LIMIT 20",
            new String[]{startDate, endDate});
    }

    /** ملخص قرارات لوحة التحكم الذكية */
    public HashMap<String, Integer> getDecisionSummary() {
        HashMap<String, Integer> r = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c;
            c = db.rawQuery("SELECT COUNT(*) FROM products WHERE expiry!='' AND expiry IS NOT NULL AND DATE(expiry)<DATE('now') AND qty>0", null);
            r.put("expired",     c.moveToFirst() ? c.getInt(0) : 0); c.close();
            c = db.rawQuery("SELECT COUNT(*) FROM products WHERE expiry!='' AND expiry IS NOT NULL AND DATE(expiry) BETWEEN DATE('now') AND DATE('now','+7 days') AND qty>0", null);
            r.put("expiring_7",  c.moveToFirst() ? c.getInt(0) : 0); c.close();
            c = db.rawQuery("SELECT COUNT(*) FROM products WHERE qty<=reorder_level AND qty>0", null);
            r.put("low_stock",   c.moveToFirst() ? c.getInt(0) : 0); c.close();
            c = db.rawQuery(
                "SELECT COUNT(*) FROM products WHERE qty>0 AND id NOT IN (" +
                "SELECT DISTINCT CAST(product_id AS INTEGER) FROM invoice_items ii" +
                " JOIN invoices i ON ii.invoice_id=i.id" +
                " WHERE DATE(i.created_at)>=DATE('now','-30 days')" +
                " AND product_id!='' AND product_id IS NOT NULL)", null);
            r.put("dead_stock",  c.moveToFirst() ? c.getInt(0) : 0); c.close();
        } catch (Exception e) { Log.e(TAG, "getDecisionSummary: " + e.getMessage(), e); }
        return r;
    }

    /** تحديث إحصائيات العميل بعد كل فاتورة */
    public void updateCustomerStats(String customerId) {
        if (customerId == null || customerId.isEmpty() || "0".equals(customerId)) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(
                "UPDATE " + TABLE_CUSTOMERS +
                " SET total_spent = (SELECT COALESCE(SUM(total), 0) FROM " + TABLE_INVOICES +
                "  WHERE CAST(customer_id AS INTEGER) = CAST(? AS INTEGER))," +
                " last_purchase_at = (SELECT MAX(created_at) FROM " + TABLE_INVOICES +
                "  WHERE CAST(customer_id AS INTEGER) = CAST(? AS INTEGER))" +
                " WHERE id = CAST(? AS INTEGER)",
                new String[]{customerId, customerId, customerId});
        } catch (Exception e) {
            Log.e(TAG, "updateCustomerStats: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // USERS & PERMISSIONS
    // ════════════════════════════════════════════════════════════

    private void createUsersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "username TEXT UNIQUE NOT NULL, " +
            "pin TEXT NOT NULL, " +
            "role TEXT DEFAULT 'cashier', " +
            "is_active INTEGER DEFAULT 1, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_PERMISSIONS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "permission TEXT NOT NULL, " +
            "allowed INTEGER DEFAULT 1, " +
            "UNIQUE(user_id, permission), " +
            "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id) ON DELETE CASCADE)");
    }

    private void insertDefaultAdminUser(SQLiteDatabase db) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", "المدير");
            cv.put("username", "admin");
            cv.put("pin", "1234");
            cv.put("role", "admin");
            cv.put("is_active", 1);
            db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) { Log.e(TAG, "insertDefaultAdminUser: " + e.getMessage()); }
    }

    public long addUser(String name, String username, String pin, String role) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("username", username);
            cv.put("pin", pin);
            cv.put("role", role != null ? role : "cashier");
            cv.put("is_active", 1);
            return db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_ABORT);
        } catch (Exception e) { Log.e(TAG, "addUser: " + e.getMessage()); return -1; }
    }

    public HashMap<String, String> getUserByPin(String pin) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_USERS + " WHERE pin=? AND is_active=1 LIMIT 1",
            new String[]{pin});
        return list.isEmpty() ? null : list.get(0);
    }

    public HashMap<String, String> getUserByUsername(String username) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_USERS + " WHERE username=? AND is_active=1 LIMIT 1",
            new String[]{username});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<HashMap<String, String>> getAllUsers() {
        return queryTable("SELECT * FROM " + TABLE_USERS + " ORDER BY role ASC, name ASC", null);
    }

    public boolean updateUser(long id, String name, String role, int isActive) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("role", role);
            cv.put("is_active", isActive);
            return getWritableDatabase().update(TABLE_USERS, cv, "id=?",
                new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) { Log.e(TAG, "updateUser: " + e.getMessage()); return false; }
    }

    public boolean updateUserPin(long userId, String newPin) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("pin", newPin);
            return getWritableDatabase().update(TABLE_USERS, cv, "id=?",
                new String[]{String.valueOf(userId)}) > 0;
        } catch (Exception e) { Log.e(TAG, "updateUserPin: " + e.getMessage()); return false; }
    }

    public boolean deleteUser(long id) {
        try {
            return getWritableDatabase().delete(TABLE_USERS, "id=? AND role!='admin'",
                new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) { Log.e(TAG, "deleteUser: " + e.getMessage()); return false; }
    }

    public boolean hasPermission(long userId, String permission) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            // Admin has all permissions
            Cursor roleC = db.rawQuery("SELECT role FROM " + TABLE_USERS + " WHERE id=?",
                new String[]{String.valueOf(userId)});
            if (roleC.moveToFirst() && "admin".equals(roleC.getString(0))) {
                roleC.close(); return true;
            }
            roleC.close();
            // Check specific permission
            Cursor c = db.rawQuery(
                "SELECT allowed FROM " + TABLE_USER_PERMISSIONS +
                " WHERE user_id=? AND permission=?",
                new String[]{String.valueOf(userId), permission});
            boolean allowed = false;
            if (c.moveToFirst()) allowed = c.getInt(0) == 1;
            c.close();
            return allowed;
        } catch (Exception e) { Log.e(TAG, "hasPermission: " + e.getMessage()); return false; }
    }

    public void setPermission(long userId, String permission, boolean allowed) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);
            cv.put("permission", permission);
            cv.put("allowed", allowed ? 1 : 0);
            db.insertWithOnConflict(TABLE_USER_PERMISSIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) { Log.e(TAG, "setPermission: " + e.getMessage()); }
    }

    public int getUsersCount() {
        try {
            Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS + " WHERE is_active=1", null);
            int count = c.moveToFirst() ? c.getInt(0) : 0;
            c.close();
            return count;
        } catch (Exception e) { return 0; }
    }

    // ════════════════════════════════════════════════════════════
    // AUDIT LOG
    // ════════════════════════════════════════════════════════════

    private void createAuditLogTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AUDIT_LOG + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER DEFAULT 0, " +
            "user_name TEXT DEFAULT 'admin', " +
            "action TEXT NOT NULL, " +
            "table_name TEXT DEFAULT '', " +
            "record_id TEXT DEFAULT '', " +
            "description TEXT, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_date ON " + TABLE_AUDIT_LOG + "(created_at)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_action ON " + TABLE_AUDIT_LOG + "(action)"); } catch (Exception ignored) {}
    }

    public void logAction(long userId, String userName, String action, String tableName,
                           String recordId, String description) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);
            cv.put("user_name", userName != null ? userName : "admin");
            cv.put("action", action);
            cv.put("table_name", tableName != null ? tableName : "");
            cv.put("record_id", recordId != null ? recordId : "");
            cv.put("description", description != null ? description : "");
            getWritableDatabase().insert(TABLE_AUDIT_LOG, null, cv);
        } catch (Exception e) { Log.e(TAG, "logAction: " + e.getMessage()); }
    }

    public List<HashMap<String, String>> getAuditLog(int limit) {
        return queryTable("SELECT * FROM " + TABLE_AUDIT_LOG +
            " ORDER BY created_at DESC LIMIT " + limit, null);
    }

    public List<HashMap<String, String>> getAuditLogForUser(long userId) {
        return queryTable("SELECT * FROM " + TABLE_AUDIT_LOG +
            " WHERE user_id=? ORDER BY created_at DESC LIMIT 200",
            new String[]{String.valueOf(userId)});
    }

    // ════════════════════════════════════════════════════════════
    // CUSTOMER CHECKS (شيكات العملاء)
    // ════════════════════════════════════════════════════════════

    private void createCustomerChecksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOMER_CHECKS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "customer_id INTEGER DEFAULT 0, " +
            "customer_name TEXT DEFAULT '', " +
            "check_number TEXT DEFAULT '', " +
            "bank_name TEXT DEFAULT '', " +
            "amount REAL DEFAULT 0.0, " +
            "issue_date TEXT DEFAULT '', " +
            "due_date TEXT DEFAULT '', " +
            "status TEXT DEFAULT 'pending', " +
            "notes TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_cust_checks_due ON " + TABLE_CUSTOMER_CHECKS + "(due_date)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_cust_checks_status ON " + TABLE_CUSTOMER_CHECKS + "(status)"); } catch (Exception ignored) {}
    }

    private void createSupplierChecksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SUPPLIER_CHECKS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "supplier_id INTEGER DEFAULT 0, " +
            "supplier_name TEXT DEFAULT '', " +
            "check_number TEXT DEFAULT '', " +
            "bank_name TEXT DEFAULT '', " +
            "amount REAL DEFAULT 0.0, " +
            "issue_date TEXT DEFAULT '', " +
            "due_date TEXT DEFAULT '', " +
            "status TEXT DEFAULT 'pending', " +
            "notes TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    public long addCustomerCheck(int customerId, String customerName, String checkNumber,
                                  String bankName, double amount, String issueDate,
                                  String dueDate, String notes) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("customer_name", customerName != null ? customerName : "");
            cv.put("check_number", checkNumber != null ? checkNumber : "");
            cv.put("bank_name", bankName != null ? bankName : "");
            cv.put("amount", amount);
            cv.put("issue_date", issueDate != null ? issueDate : getCurrentDateTime().substring(0,10));
            cv.put("due_date", dueDate != null ? dueDate : "");
            cv.put("status", "pending");
            cv.put("notes", notes != null ? notes : "");
            return getWritableDatabase().insert(TABLE_CUSTOMER_CHECKS, null, cv);
        } catch (Exception e) { Log.e(TAG, "addCustomerCheck: " + e.getMessage()); return -1; }
    }

    public List<HashMap<String, String>> getAllCustomerChecks() {
        return queryTable("SELECT cc.*, c.phone as customer_phone FROM " + TABLE_CUSTOMER_CHECKS +
            " cc LEFT JOIN " + TABLE_CUSTOMERS + " c ON cc.customer_id=c.id" +
            " ORDER BY cc.due_date ASC", null);
    }

    public List<HashMap<String, String>> getCustomerChecks(int customerId) {
        return queryTable("SELECT * FROM " + TABLE_CUSTOMER_CHECKS +
            " WHERE customer_id=? ORDER BY due_date ASC",
            new String[]{String.valueOf(customerId)});
    }

    public List<HashMap<String, String>> getDueCustomerChecks(int days) {
        return queryTable("SELECT cc.*, c.phone as customer_phone FROM " + TABLE_CUSTOMER_CHECKS +
            " cc LEFT JOIN " + TABLE_CUSTOMERS + " c ON cc.customer_id=c.id" +
            " WHERE cc.status='pending' AND cc.due_date!='' " +
            " AND DATE(cc.due_date) <= DATE('now', '+' || ? || ' days')" +
            " ORDER BY cc.due_date ASC",
            new String[]{String.valueOf(days)});
    }

    public boolean updateCustomerCheckStatus(long id, String status) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", status);
            return getWritableDatabase().update(TABLE_CUSTOMER_CHECKS, cv, "id=?",
                new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) { Log.e(TAG, "updateCustomerCheckStatus: " + e.getMessage()); return false; }
    }

    public boolean deleteCustomerCheck(long id) {
        try {
            return getWritableDatabase().delete(TABLE_CUSTOMER_CHECKS, "id=?",
                new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) { Log.e(TAG, "deleteCustomerCheck: " + e.getMessage()); return false; }
    }

    public double getTotalPendingCustomerChecks() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_CUSTOMER_CHECKS + " WHERE status='pending'", null);
            double val = c.moveToFirst() ? c.getDouble(0) : 0;
            c.close(); return val;
        } catch (Exception e) { return 0; }
    }

    public long addSupplierCheck(int supplierId, String supplierName, String checkNumber,
                                  String bankName, double amount, String issueDate,
                                  String dueDate, String notes) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("supplier_id", supplierId);
            cv.put("supplier_name", supplierName != null ? supplierName : "");
            cv.put("check_number", checkNumber != null ? checkNumber : "");
            cv.put("bank_name", bankName != null ? bankName : "");
            cv.put("amount", amount);
            cv.put("issue_date", issueDate != null ? issueDate : getCurrentDateTime().substring(0,10));
            cv.put("due_date", dueDate != null ? dueDate : "");
            cv.put("status", "pending");
            cv.put("notes", notes != null ? notes : "");
            return getWritableDatabase().insert(TABLE_SUPPLIER_CHECKS, null, cv);
        } catch (Exception e) { Log.e(TAG, "addSupplierCheck: " + e.getMessage()); return -1; }
    }

    public List<HashMap<String, String>> getAllSupplierChecks() {
        return queryTable("SELECT * FROM " + TABLE_SUPPLIER_CHECKS + " ORDER BY due_date ASC", null);
    }

    public boolean updateSupplierCheckStatus(long id, String status) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", status);
            return getWritableDatabase().update(TABLE_SUPPLIER_CHECKS, cv, "id=?",
                new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) { return false; }
    }

    // ════════════════════════════════════════════════════════════
    // INSTALLMENTS (الأقساط)
    // ════════════════════════════════════════════════════════════

    private void createInstallmentContractsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INSTALLMENT_CONTRACTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "contract_number TEXT UNIQUE NOT NULL, " +
            "customer_id INTEGER DEFAULT 0, " +
            "customer_name TEXT DEFAULT '', " +
            "invoice_id INTEGER DEFAULT 0, " +
            "invoice_number TEXT DEFAULT '', " +
            "total_amount REAL DEFAULT 0.0, " +
            "down_payment REAL DEFAULT 0.0, " +
            "remaining_amount REAL DEFAULT 0.0, " +
            "paid_amount REAL DEFAULT 0.0, " +
            "installment_count INTEGER DEFAULT 1, " +
            "installment_amount REAL DEFAULT 0.0, " +
            "start_date TEXT DEFAULT '', " +
            "end_date TEXT DEFAULT '', " +
            "status TEXT DEFAULT 'active', " +
            "notes TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INSTALLMENT_PAYMENTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "contract_id INTEGER NOT NULL, " +
            "installment_number INTEGER DEFAULT 1, " +
            "due_date TEXT DEFAULT '', " +
            "paid_date TEXT DEFAULT '', " +
            "amount REAL DEFAULT 0.0, " +
            "status TEXT DEFAULT 'pending', " +
            "notes TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(contract_id) REFERENCES " + TABLE_INSTALLMENT_CONTRACTS + "(id) ON DELETE CASCADE)");
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_installment_customer ON " + TABLE_INSTALLMENT_CONTRACTS + "(customer_id)"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_installment_status ON " + TABLE_INSTALLMENT_CONTRACTS + "(status)"); } catch (Exception ignored) {}
    }

    public long createInstallmentContract(int customerId, String customerName,
                                           double totalAmount, double downPayment,
                                           int installmentCount, String startDate, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long contractId = -1;
        try {
            String contractNumber = "INST-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                java.util.Locale.US).format(new java.util.Date());
            double remaining = totalAmount - downPayment;
            double installmentAmount = installmentCount > 0 ? remaining / installmentCount : remaining;

            ContentValues cv = new ContentValues();
            cv.put("contract_number", contractNumber);
            cv.put("customer_id", customerId);
            cv.put("customer_name", customerName != null ? customerName : "");
            cv.put("total_amount", totalAmount);
            cv.put("down_payment", downPayment);
            cv.put("remaining_amount", remaining);
            cv.put("paid_amount", downPayment);
            cv.put("installment_count", installmentCount);
            cv.put("installment_amount", installmentAmount);
            cv.put("start_date", startDate != null ? startDate : getCurrentDateTime().substring(0,10));
            cv.put("status", "active");
            cv.put("notes", notes != null ? notes : "");
            contractId = db.insert(TABLE_INSTALLMENT_CONTRACTS, null, cv);
            if (contractId < 0) throw new Exception("Failed to create contract");

            // Create installment payment schedule
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            if (startDate != null && !startDate.isEmpty()) {
                try { cal.setTime(sdf.parse(startDate)); } catch (Exception ex) { /* use current date */ }
            }
            for (int i = 1; i <= installmentCount; i++) {
                cal.add(java.util.Calendar.MONTH, 1);
                ContentValues pcv = new ContentValues();
                pcv.put("contract_id", contractId);
                pcv.put("installment_number", i);
                pcv.put("due_date", sdf.format(cal.getTime()));
                pcv.put("amount", installmentAmount);
                pcv.put("status", "pending");
                db.insert(TABLE_INSTALLMENT_PAYMENTS, null, pcv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "createInstallmentContract: " + e.getMessage());
        } finally { db.endTransaction(); }
        return contractId;
    }

    public List<HashMap<String, String>> getAllInstallmentContracts() {
        return queryTable("SELECT * FROM " + TABLE_INSTALLMENT_CONTRACTS +
            " ORDER BY created_at DESC", null);
    }

    public List<HashMap<String, String>> getCustomerInstallments(int customerId) {
        return queryTable("SELECT * FROM " + TABLE_INSTALLMENT_CONTRACTS +
            " WHERE customer_id=? ORDER BY created_at DESC",
            new String[]{String.valueOf(customerId)});
    }

    public List<HashMap<String, String>> getContractPayments(long contractId) {
        return queryTable("SELECT * FROM " + TABLE_INSTALLMENT_PAYMENTS +
            " WHERE contract_id=? ORDER BY installment_number ASC",
            new String[]{String.valueOf(contractId)});
    }

    public boolean payInstallment(long paymentId, String paidDate) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Mark payment as paid
            ContentValues pcv = new ContentValues();
            pcv.put("status", "paid");
            pcv.put("paid_date", paidDate != null ? paidDate : getCurrentDateTime().substring(0,10));
            db.update(TABLE_INSTALLMENT_PAYMENTS, pcv, "id=?", new String[]{String.valueOf(paymentId)});

            // Get payment amount and contract id
            Cursor c = db.rawQuery("SELECT amount, contract_id FROM " + TABLE_INSTALLMENT_PAYMENTS +
                " WHERE id=?", new String[]{String.valueOf(paymentId)});
            if (c.moveToFirst()) {
                double amount = c.getDouble(0);
                long contractId = c.getLong(1);
                c.close();
                // Update contract paid_amount and remaining
                db.execSQL("UPDATE " + TABLE_INSTALLMENT_CONTRACTS +
                    " SET paid_amount = paid_amount + ?, remaining_amount = remaining_amount - ?" +
                    " WHERE id=?", new Object[]{amount, amount, contractId});
                // Check if contract is fully paid
                Cursor cc = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_INSTALLMENT_PAYMENTS +
                    " WHERE contract_id=? AND status='pending'", new String[]{String.valueOf(contractId)});
                if (cc.moveToFirst() && cc.getInt(0) == 0) {
                    ContentValues ccv = new ContentValues();
                    ccv.put("status", "completed");
                    db.update(TABLE_INSTALLMENT_CONTRACTS, ccv, "id=?", new String[]{String.valueOf(contractId)});
                }
                cc.close();
            } else { c.close(); }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "payInstallment: " + e.getMessage()); return false;
        } finally { db.endTransaction(); }
    }

    public List<HashMap<String, String>> getOverdueInstallments() {
        return queryTable(
            "SELECT ip.*, ic.customer_name, ic.contract_number FROM " + TABLE_INSTALLMENT_PAYMENTS +
            " ip JOIN " + TABLE_INSTALLMENT_CONTRACTS + " ic ON ip.contract_id=ic.id" +
            " WHERE ip.status='pending' AND ip.due_date < DATE('now')" +
            " ORDER BY ip.due_date ASC", null);
    }

    public double getTotalInstallmentsReceivable() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(remaining_amount),0) FROM " + TABLE_INSTALLMENT_CONTRACTS +
                " WHERE status='active'", null);
            double val = c.moveToFirst() ? c.getDouble(0) : 0;
            c.close(); return val;
        } catch (Exception e) { return 0; }
    }

    // ════════════════════════════════════════════════════════════
    // CASH DRAWERS (الخزائن)
    // ════════════════════════════════════════════════════════════

    private void createCashDrawersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CASH_DRAWERS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "current_balance REAL DEFAULT 0.0, " +
            "is_main INTEGER DEFAULT 0, " +
            "is_active INTEGER DEFAULT 1, " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CASH_TRANSACTIONS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "drawer_id INTEGER NOT NULL, " +
            "type TEXT DEFAULT 'in', " +
            "amount REAL DEFAULT 0.0, " +
            "balance_after REAL DEFAULT 0.0, " +
            "reason TEXT DEFAULT '', " +
            "user_name TEXT DEFAULT 'admin', " +
            "reference_id TEXT DEFAULT '', " +
            "reference_type TEXT DEFAULT 'manual', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(drawer_id) REFERENCES " + TABLE_CASH_DRAWERS + "(id))");
    }

    private void insertDefaultCashDrawer(SQLiteDatabase db) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", "الخزينة الرئيسية");
            cv.put("current_balance", 0.0);
            cv.put("is_main", 1);
            cv.put("is_active", 1);
            db.insertWithOnConflict(TABLE_CASH_DRAWERS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) { Log.e(TAG, "insertDefaultCashDrawer: " + e.getMessage()); }
    }

    public List<HashMap<String, String>> getAllCashDrawers() {
        return queryTable("SELECT * FROM " + TABLE_CASH_DRAWERS + " WHERE is_active=1 ORDER BY is_main DESC", null);
    }

    public HashMap<String, String> getMainCashDrawer() {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_CASH_DRAWERS + " WHERE is_main=1 AND is_active=1 LIMIT 1", null);
        if (list.isEmpty()) {
            list = queryTable("SELECT * FROM " + TABLE_CASH_DRAWERS + " WHERE is_active=1 LIMIT 1", null);
        }
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean cashDrawerTransaction(long drawerId, String type, double amount,
                                          String reason, String userName, String refId, String refType) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT current_balance FROM " + TABLE_CASH_DRAWERS + " WHERE id=?",
                new String[]{String.valueOf(drawerId)});
            double currentBalance = 0;
            if (c.moveToFirst()) currentBalance = c.getDouble(0);
            c.close();

            double newBalance = "in".equals(type) ? currentBalance + amount : currentBalance - amount;
            if (newBalance < 0 && "out".equals(type)) throw new Exception("Insufficient balance");

            ContentValues cv = new ContentValues();
            cv.put("current_balance", newBalance);
            db.update(TABLE_CASH_DRAWERS, cv, "id=?", new String[]{String.valueOf(drawerId)});

            ContentValues tcv = new ContentValues();
            tcv.put("drawer_id", drawerId);
            tcv.put("type", type);
            tcv.put("amount", amount);
            tcv.put("balance_after", newBalance);
            tcv.put("reason", reason != null ? reason : "");
            tcv.put("user_name", userName != null ? userName : "admin");
            tcv.put("reference_id", refId != null ? refId : "");
            tcv.put("reference_type", refType != null ? refType : "manual");
            db.insert(TABLE_CASH_TRANSACTIONS, null, tcv);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "cashDrawerTransaction: " + e.getMessage()); return false;
        } finally { db.endTransaction(); }
    }

    public List<HashMap<String, String>> getCashTransactions(long drawerId, int limit) {
        return queryTable("SELECT * FROM " + TABLE_CASH_TRANSACTIONS +
            " WHERE drawer_id=? ORDER BY created_at DESC LIMIT " + limit,
            new String[]{String.valueOf(drawerId)});
    }

    public long addCashDrawer(String name) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("current_balance", 0.0);
            cv.put("is_main", 0);
            cv.put("is_active", 1);
            return getWritableDatabase().insert(TABLE_CASH_DRAWERS, null, cv);
        } catch (Exception e) { Log.e(TAG, "addCashDrawer: " + e.getMessage()); return -1; }
    }

    // ════════════════════════════════════════════════════════════
    // STOCK COUNT / INVENTORY COUNT (الجرد)
    // ════════════════════════════════════════════════════════════

    private void createStockCountTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STOCK_COUNT_SESSIONS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "session_number TEXT UNIQUE NOT NULL, " +
            "status TEXT DEFAULT 'open', " +
            "started_by TEXT DEFAULT 'admin', " +
            "notes TEXT DEFAULT '', " +
            "started_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "completed_at DATETIME)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STOCK_COUNT_ITEMS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "session_id INTEGER NOT NULL, " +
            "product_id INTEGER NOT NULL, " +
            "product_name TEXT DEFAULT '', " +
            "barcode TEXT DEFAULT '', " +
            "system_qty INTEGER DEFAULT 0, " +
            "counted_qty INTEGER DEFAULT 0, " +
            "difference INTEGER DEFAULT 0, " +
            "notes TEXT DEFAULT '', " +
            "FOREIGN KEY(session_id) REFERENCES " + TABLE_STOCK_COUNT_SESSIONS + "(id))");
    }

    public long createStockCountSession(String startedBy) {
        try {
            String sessionNumber = "SC-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                java.util.Locale.US).format(new java.util.Date());
            ContentValues cv = new ContentValues();
            cv.put("session_number", sessionNumber);
            cv.put("status", "open");
            cv.put("started_by", startedBy != null ? startedBy : "admin");
            return getWritableDatabase().insert(TABLE_STOCK_COUNT_SESSIONS, null, cv);
        } catch (Exception e) { Log.e(TAG, "createStockCountSession: " + e.getMessage()); return -1; }
    }

    public HashMap<String, String> getActiveStockCountSession() {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_STOCK_COUNT_SESSIONS + " WHERE status='open' LIMIT 1", null);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<HashMap<String, String>> getAllStockCountSessions() {
        return queryTable("SELECT * FROM " + TABLE_STOCK_COUNT_SESSIONS +
            " ORDER BY started_at DESC", null);
    }

    public long addStockCountItem(long sessionId, int productId, String productName,
                                   String barcode, int systemQty, int countedQty, String notes) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("session_id", sessionId);
            cv.put("product_id", productId);
            cv.put("product_name", productName != null ? productName : "");
            cv.put("barcode", barcode != null ? barcode : "");
            cv.put("system_qty", systemQty);
            cv.put("counted_qty", countedQty);
            cv.put("difference", countedQty - systemQty);
            cv.put("notes", notes != null ? notes : "");
            return getWritableDatabase().insertWithOnConflict(TABLE_STOCK_COUNT_ITEMS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) { Log.e(TAG, "addStockCountItem: " + e.getMessage()); return -1; }
    }

    public List<HashMap<String, String>> getStockCountItems(long sessionId) {
        return queryTable("SELECT * FROM " + TABLE_STOCK_COUNT_ITEMS +
            " WHERE session_id=? ORDER BY product_name ASC",
            new String[]{String.valueOf(sessionId)});
    }

    public boolean completeStockCount(long sessionId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<HashMap<String, String>> items = getStockCountItems(sessionId);
            for (HashMap<String, String> item : items) {
                int countedQty = Integer.parseInt(item.getOrDefault("counted_qty", "0"));
                String productId = item.getOrDefault("product_id", "0");
                ContentValues cv = new ContentValues();
                cv.put("qty", countedQty);
                db.update(TABLE_PRODUCTS, cv, "id=?", new String[]{productId});
            }
            ContentValues cv = new ContentValues();
            cv.put("status", "completed");
            cv.put("completed_at", getCurrentDateTime());
            db.update(TABLE_STOCK_COUNT_SESSIONS, cv, "id=?", new String[]{String.valueOf(sessionId)});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "completeStockCount: " + e.getMessage()); return false;
        } finally { db.endTransaction(); }
    }

    public boolean cancelStockCount(long sessionId) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", "cancelled");
            return getWritableDatabase().update(TABLE_STOCK_COUNT_SESSIONS, cv, "id=?",
                new String[]{String.valueOf(sessionId)}) > 0;
        } catch (Exception e) { return false; }
    }

    // ════════════════════════════════════════════════════════════
    // Private table creation methods for new tables
    // ════════════════════════════════════════════════════════════

    private void createWalletTransactionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WALLET_TRANSACTIONS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "type TEXT NOT NULL DEFAULT 'OUT', " +
            "amount REAL DEFAULT 0.0, " +
            "note TEXT DEFAULT '', " +
            "date TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    private void createDirectPaymentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DIRECT_PAYMENTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "entity_type TEXT NOT NULL DEFAULT 'customer', " +
            "entity_id INTEGER DEFAULT 0, " +
            "entity_name TEXT DEFAULT '', " +
            "type TEXT NOT NULL DEFAULT 'OUT', " +
            "amount REAL DEFAULT 0.0, " +
            "note TEXT DEFAULT '', " +
            "date TEXT DEFAULT '', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    private void createPriceQuotesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRICE_QUOTES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "quote_number TEXT UNIQUE, " +
            "customer_id INTEGER DEFAULT 0, " +
            "customer_name TEXT DEFAULT '', " +
            "subtotal REAL DEFAULT 0.0, " +
            "discount REAL DEFAULT 0.0, " +
            "tax REAL DEFAULT 0.0, " +
            "total REAL DEFAULT 0.0, " +
            "notes TEXT DEFAULT '', " +
            "status TEXT DEFAULT 'active', " +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRICE_QUOTE_ITEMS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "quote_id INTEGER NOT NULL, " +
            "product_id INTEGER DEFAULT 0, " +
            "barcode TEXT DEFAULT '', " +
            "name TEXT NOT NULL, " +
            "price REAL DEFAULT 0.0, " +
            "qty INTEGER DEFAULT 1, " +
            "total REAL DEFAULT 0.0, " +
            "FOREIGN KEY(quote_id) REFERENCES " + TABLE_PRICE_QUOTES + "(id) ON DELETE CASCADE)");
    }

    // ════════════════════════════════════════════════════════════
    // Wallet / Treasury
    // ════════════════════════════════════════════════════════════

    public long addWalletTransaction(String type, double amount, String note, String date) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("type",   type   != null ? type   : "OUT");
            cv.put("amount", amount);
            cv.put("note",   note   != null ? note   : "");
            cv.put("date",   date   != null ? date   : getCurrentDate());
            return getWritableDatabase().insert(TABLE_WALLET_TRANSACTIONS, null, cv);
        } catch (Exception e) { Log.e(TAG, "addWalletTransaction: " + e.getMessage()); return -1; }
    }

    public double getWalletBalance() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN type='IN' THEN amount ELSE -amount END),0) FROM " +
                TABLE_WALLET_TRANSACTIONS, null);
            double b = 0; if (c.moveToFirst()) b = c.getDouble(0); c.close(); return b;
        } catch (Exception e) { return 0; }
    }

    public List<HashMap<String, String>> getWalletTransactions(String fromDate, String toDate) {
        if (fromDate != null && toDate != null && !fromDate.isEmpty() && !toDate.isEmpty()) {
            return queryTable("SELECT * FROM " + TABLE_WALLET_TRANSACTIONS +
                " WHERE date BETWEEN ? AND ? ORDER BY created_at DESC",
                new String[]{fromDate, toDate});
        }
        return queryTable("SELECT * FROM " + TABLE_WALLET_TRANSACTIONS +
            " ORDER BY created_at DESC", null);
    }

    public double getWalletIn(String fromDate, String toDate) {
        try {
            String sql; String[] args = null;
            if (fromDate != null && toDate != null && !fromDate.isEmpty()) {
                sql = "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_WALLET_TRANSACTIONS +
                      " WHERE type='IN' AND date BETWEEN ? AND ?";
                args = new String[]{fromDate, toDate};
            } else {
                sql = "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_WALLET_TRANSACTIONS + " WHERE type='IN'";
            }
            Cursor c = getReadableDatabase().rawQuery(sql, args);
            double v = 0; if (c.moveToFirst()) v = c.getDouble(0); c.close(); return v;
        } catch (Exception e) { return 0; }
    }

    public double getWalletOut(String fromDate, String toDate) {
        try {
            String sql; String[] args = null;
            if (fromDate != null && toDate != null && !fromDate.isEmpty()) {
                sql = "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_WALLET_TRANSACTIONS +
                      " WHERE type='OUT' AND date BETWEEN ? AND ?";
                args = new String[]{fromDate, toDate};
            } else {
                sql = "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_WALLET_TRANSACTIONS + " WHERE type='OUT'";
            }
            Cursor c = getReadableDatabase().rawQuery(sql, args);
            double v = 0; if (c.moveToFirst()) v = c.getDouble(0); c.close(); return v;
        } catch (Exception e) { return 0; }
    }

    public boolean deleteWalletTransaction(long id) {
        try { return getWritableDatabase().delete(TABLE_WALLET_TRANSACTIONS, "id=?",
            new String[]{String.valueOf(id)}) > 0; }
        catch (Exception e) { return false; }
    }

    // ════════════════════════════════════════════════════════════
    // Direct Payments (customer/supplier)
    // ════════════════════════════════════════════════════════════

    public long addDirectPayment(String entityType, long entityId, String entityName,
                                  String type, double amount, String note, String date) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("entity_type", entityType != null ? entityType : "customer");
            cv.put("entity_id",   entityId);
            cv.put("entity_name", entityName != null ? entityName : "");
            cv.put("type",        type != null ? type : "OUT");
            cv.put("amount",      amount);
            cv.put("note",        note != null ? note : "");
            cv.put("date",        date != null ? date : getCurrentDate());
            long rowId = getWritableDatabase().insert(TABLE_DIRECT_PAYMENTS, null, cv);
            if (rowId > 0) {
                if ("customer".equals(entityType)) {
                    // IN = customer paid us (reduces debt), OUT = we paid customer (increases debt)
                    double delta = "IN".equals(type) ? -amount : amount;
                    updateCustomerDebt(String.valueOf(entityId), delta);
                } else if ("supplier".equals(entityType)) {
                    double delta = "OUT".equals(type) ? -amount : amount;
                    updateSupplierDebt(String.valueOf(entityId), delta);
                }
            }
            return rowId;
        } catch (Exception e) { Log.e(TAG, "addDirectPayment: " + e.getMessage()); return -1; }
    }

    public List<HashMap<String, String>> getDirectPayments(String entityType, long entityId) {
        return queryTable("SELECT * FROM " + TABLE_DIRECT_PAYMENTS +
            " WHERE entity_type=? AND entity_id=? ORDER BY created_at DESC",
            new String[]{entityType, String.valueOf(entityId)});
    }

    public List<HashMap<String, String>> getAllDirectPayments() {
        return queryTable("SELECT * FROM " + TABLE_DIRECT_PAYMENTS +
            " ORDER BY created_at DESC", null);
    }

    public boolean deleteDirectPayment(long id) {
        try { return getWritableDatabase().delete(TABLE_DIRECT_PAYMENTS, "id=?",
            new String[]{String.valueOf(id)}) > 0; }
        catch (Exception e) { return false; }
    }

    // ════════════════════════════════════════════════════════════
    // Price Quotes
    // ════════════════════════════════════════════════════════════

    public long addPriceQuote(String customerName, long customerId, List<HashMap<String, String>> items,
                               double subtotal, double discount, double tax, double total, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long quoteId = -1;
        try {
            String quoteNumber = "QT-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                java.util.Locale.US).format(new java.util.Date());
            ContentValues cv = new ContentValues();
            cv.put("quote_number",  quoteNumber);
            cv.put("customer_id",   customerId);
            cv.put("customer_name", customerName != null ? customerName : "");
            cv.put("subtotal",      subtotal);
            cv.put("discount",      discount);
            cv.put("tax",           tax);
            cv.put("total",         total);
            cv.put("notes",         notes != null ? notes : "");
            cv.put("status",        "active");
            quoteId = db.insert(TABLE_PRICE_QUOTES, null, cv);
            if (quoteId < 0) throw new Exception("Failed to insert quote");
            for (HashMap<String, String> item : items) {
                ContentValues icv = new ContentValues();
                icv.put("quote_id",   quoteId);
                icv.put("product_id", safeInt(item, "id", 0));
                icv.put("barcode",    safeGet(item, "barcode"));
                icv.put("name",       safeGet(item, "name"));
                icv.put("price",      safeDouble(item, "price"));
                icv.put("qty",        safeInt(item, "qty", 1));
                icv.put("total",      safeDouble(item, "total"));
                db.insert(TABLE_PRICE_QUOTE_ITEMS, null, icv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "addPriceQuote: " + e.getMessage()); quoteId = -1;
        } finally { db.endTransaction(); }
        return quoteId;
    }

    public List<HashMap<String, String>> getAllPriceQuotes() {
        return queryTable("SELECT * FROM " + TABLE_PRICE_QUOTES + " ORDER BY created_at DESC", null);
    }

    public HashMap<String, String> getPriceQuoteById(long id) {
        List<HashMap<String, String>> list = queryTable(
            "SELECT * FROM " + TABLE_PRICE_QUOTES + " WHERE id=?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<HashMap<String, String>> getPriceQuoteItems(long quoteId) {
        return queryTable("SELECT * FROM " + TABLE_PRICE_QUOTE_ITEMS + " WHERE quote_id=?",
            new String[]{String.valueOf(quoteId)});
    }

    public boolean updatePriceQuoteStatus(long quoteId, String status) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", status);
            return getWritableDatabase().update(TABLE_PRICE_QUOTES, cv, "id=?",
                new String[]{String.valueOf(quoteId)}) > 0;
        } catch (Exception e) { return false; }
    }

    public boolean deletePriceQuote(long id) {
        try { return getWritableDatabase().delete(TABLE_PRICE_QUOTES, "id=?",
            new String[]{String.valueOf(id)}) > 0; }
        catch (Exception e) { return false; }
    }

    // ════════════════════════════════════════════════════════════
    // Expense type-aware methods
    // ════════════════════════════════════════════════════════════

    public long addExpenseWithType(String category, double amount, String description,
                                    String date, String notes, String expenseType) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("title",        description != null ? description : category);
            cv.put("amount",       amount);
            cv.put("category",     category != null ? category : "");
            cv.put("note",         notes != null ? notes : "");
            cv.put("date",         date != null ? date : getCurrentDate());
            cv.put("expense_type", expenseType != null ? expenseType : "OUT");
            return getWritableDatabase().insert(TABLE_EXPENSES, null, cv);
        } catch (Exception e) { Log.e(TAG, "addExpenseWithType: " + e.getMessage()); return -1; }
    }

    public double getTotalExpensesOut() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_EXPENSES + " WHERE expense_type='OUT'", null);
            double v = 0; if (c.moveToFirst()) v = c.getDouble(0); c.close(); return v;
        } catch (Exception e) { return 0; }
    }

    public double getTotalExpensesIn() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM " + TABLE_EXPENSES + " WHERE expense_type='IN'", null);
            double v = 0; if (c.moveToFirst()) v = c.getDouble(0); c.close(); return v;
        } catch (Exception e) { return 0; }
    }

    // ════════════════════════════════════════════════════════════
    // Invoice with partial payment
    // ════════════════════════════════════════════════════════════

    public <T> long createInvoiceWithPartialPayment(String invoiceNumber, String customerId,
            String customerName, List<T> cartItems,
            double subtotal, double discount, double tax, double total,
            String paymentMethod, double paidAmount, double remainingAmount,
            String invoiceNote, long supplierId, String supplierName) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long invoiceId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put("invoice_number",   invoiceNumber);
            cv.put("customer_id",      customerId != null ? customerId : "0");
            cv.put("customer_name",    customerName != null ? customerName : "");
            cv.put("subtotal",         subtotal);
            cv.put("discount",         discount);
            cv.put("tax",              tax);
            cv.put("total",            total);
            cv.put("payment_method",   paymentMethod != null ? paymentMethod : "نقدي");
            cv.put("status",           "completed");
            cv.put("notes",            invoiceNote != null ? invoiceNote : "");
            cv.put("paid_amount",      paidAmount);
            cv.put("remaining_amount", remainingAmount);
            cv.put("invoice_note",     invoiceNote != null ? invoiceNote : "");
            cv.put("invoice_date",     getCurrentDate());
            cv.put("supplier_id",      supplierId);
            cv.put("supplier_name",    supplierName != null ? supplierName : "");
            invoiceId = db.insert(TABLE_INVOICES, null, cv);
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
                    icv.put("invoice_id", invoiceId);
                    icv.put("product_id", pid);
                    icv.put("barcode",    "");
                    icv.put("name",       pname);
                    icv.put("price",      pprice);
                    icv.put("qty",        pqty);
                    icv.put("total",      pprice * pqty);
                    db.insert(TABLE_INVOICE_ITEMS, null, icv);
                } catch (Exception ex) {
                    Log.w(TAG, "createInvoiceWithPartialPayment item: " + ex.getMessage());
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "createInvoiceWithPartialPayment: " + e.getMessage()); invoiceId = -1;
        } finally { db.endTransaction(); }
        if (invoiceId > 0) updateCustomerStats(customerId);
        return invoiceId;
    }

    public boolean updateInvoicePayment(long invoiceId, double paidAmount, double remainingAmount) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("paid_amount",      paidAmount);
            cv.put("remaining_amount", remainingAmount);
            return getWritableDatabase().update(TABLE_INVOICES, cv, "id=?",
                new String[]{String.valueOf(invoiceId)}) > 0;
        } catch (Exception e) { return false; }
    }

    public List<HashMap<String, String>> getUnsettledInvoices() {
        return queryTable("SELECT * FROM " + TABLE_INVOICES +
            " WHERE remaining_amount > 0 AND status != 'returned' ORDER BY created_at DESC", null);
    }

    public List<HashMap<String, String>> getUnsettledInvoicesForCustomer(long customerId) {
        return queryTable("SELECT * FROM " + TABLE_INVOICES +
            " WHERE customer_id=? AND remaining_amount > 0 AND status != 'returned' ORDER BY created_at DESC",
            new String[]{String.valueOf(customerId)});
    }

    // ════════════════════════════════════════════════════════════
    // Bulk Price Editor
    // ════════════════════════════════════════════════════════════

    public int bulkUpdatePrice(boolean isSellPrice, boolean isIncrease, String category,
                                double amount, boolean isPercent) {
        try {
            String col = isSellPrice ? "price" : "cost";
            String catFilter = (category == null || category.isEmpty() || "الكل".equals(category))
                ? "" : " WHERE category=?";
            String[] args = catFilter.isEmpty() ? null : new String[]{category};
            String expr;
            if (isPercent) {
                expr = isIncrease
                    ? col + " = " + col + " * (1 + " + amount + "/100.0)"
                    : col + " = MAX(0, " + col + " * (1 - " + amount + "/100.0))";
            } else {
                expr = isIncrease
                    ? col + " = " + col + " + " + amount
                    : col + " = MAX(0, " + col + " - " + amount + ")";
            }
            return getWritableDatabase().rawQuery(
                "UPDATE " + TABLE_PRODUCTS + " SET " + expr + catFilter, args) != null ? 1 : 0;
        } catch (Exception e) { Log.e(TAG, "bulkUpdatePrice: " + e.getMessage()); return 0; }
    }

    public int bulkUpdateSellPrice(boolean isIncrease, String category, double amount, boolean isPercent) {
        try {
            String catWhere = (category == null || category.isEmpty() || "الكل".equals(category))
                ? "" : " WHERE category=?";
            String[] args = catWhere.isEmpty() ? null : new String[]{category};
            String newPrice;
            if (isPercent) {
                newPrice = isIncrease ? "price * (1 + " + amount + "/100.0)"
                                      : "MAX(0, price * (1 - " + amount + "/100.0))";
            } else {
                newPrice = isIncrease ? "price + " + amount : "MAX(0, price - " + amount + ")";
            }
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET price = " + newPrice + catWhere, args);
            Cursor c = db.rawQuery("SELECT changes()", null);
            int rows = 0; if (c.moveToFirst()) rows = c.getInt(0); c.close();
            return rows;
        } catch (Exception e) { Log.e(TAG, "bulkUpdateSellPrice: " + e.getMessage()); return 0; }
    }

    public int bulkUpdateBuyCost(boolean isIncrease, String category, double amount, boolean isPercent) {
        try {
            String catWhere = (category == null || category.isEmpty() || "الكل".equals(category))
                ? "" : " WHERE category=?";
            String[] args = catWhere.isEmpty() ? null : new String[]{category};
            String newCost;
            if (isPercent) {
                newCost = isIncrease ? "cost * (1 + " + amount + "/100.0)"
                                     : "MAX(0, cost * (1 - " + amount + "/100.0))";
            } else {
                newCost = isIncrease ? "cost + " + amount : "MAX(0, cost - " + amount + ")";
            }
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET cost = " + newCost + catWhere, args);
            Cursor c = db.rawQuery("SELECT changes()", null);
            int rows = 0; if (c.moveToFirst()) rows = c.getInt(0); c.close();
            return rows;
        } catch (Exception e) { Log.e(TAG, "bulkUpdateBuyCost: " + e.getMessage()); return 0; }
    }

    public int getProductCountByCategory(String category) {
        try {
            String sql; String[] args;
            if (category == null || category.isEmpty() || "الكل".equals(category)) {
                sql = "SELECT COUNT(*) FROM " + TABLE_PRODUCTS; args = null;
            } else {
                sql = "SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE category=?";
                args = new String[]{category};
            }
            Cursor c = getReadableDatabase().rawQuery(sql, args);
            int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close();
            return count;
        } catch (Exception e) { return 0; }
    }

    public List<String> getAllProductCategories() {
        List<String> cats = new ArrayList<>();
        cats.add("الكل");
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT category FROM " + TABLE_PRODUCTS +
                " WHERE category IS NOT NULL AND category != '' ORDER BY category ASC", null);
            while (c.moveToNext()) cats.add(c.getString(0));
            c.close();
        } catch (Exception e) { Log.e(TAG, "getAllProductCategories: " + e.getMessage()); }
        return cats;
    }

    // ════════════════════════════════════════════════════════════
    // Supplier debt helpers (new overload using long id)
    // ════════════════════════════════════════════════════════════

    public boolean updateSupplierDebt(String supplierId, double delta) {
        try {
            getWritableDatabase().execSQL(
                "UPDATE " + TABLE_SUPPLIERS + " SET debt = debt + ? WHERE id=?",
                new Object[]{delta, supplierId});
            return true;
        } catch (Exception e) { Log.e(TAG, "updateSupplierDebt: " + e.getMessage()); return false; }
    }

    // ════════════════════════════════════════════════════════════
    // Customer / Supplier transaction history
    // ════════════════════════════════════════════════════════════

    public List<HashMap<String, String>> getCustomerTransactionHistory(long customerId) {
        // Merges invoices + direct payments for a customer
        String sql = "SELECT 'invoice' AS tx_type, invoice_number AS ref, " +
            "total AS amount, paid_amount, remaining_amount, '' AS note, " +
            "payment_method, created_at, status FROM " + TABLE_INVOICES +
            " WHERE customer_id=? " +
            "UNION ALL " +
            "SELECT 'payment' AS tx_type, '' AS ref, " +
            "amount, 0 AS paid_amount, 0 AS remaining_amount, note, type AS payment_method, " +
            "created_at, '' AS status FROM " + TABLE_DIRECT_PAYMENTS +
            " WHERE entity_type='customer' AND entity_id=? " +
            "ORDER BY created_at DESC";
        return queryTable(sql, new String[]{String.valueOf(customerId), String.valueOf(customerId)});
    }

    public List<HashMap<String, String>> getSupplierTransactionHistory(long supplierId) {
        String sql = "SELECT 'purchase' AS tx_type, po_number AS ref, " +
            "total AS amount, 0 AS paid_amount, 0 AS remaining_amount, notes AS note, " +
            "'آجل' AS payment_method, created_at, status FROM " + TABLE_PURCHASE_ORDERS +
            " WHERE supplier_id=? " +
            "UNION ALL " +
            "SELECT 'payment' AS tx_type, '' AS ref, " +
            "amount, 0 AS paid_amount, 0 AS remaining_amount, note, type AS payment_method, " +
            "created_at, '' AS status FROM " + TABLE_DIRECT_PAYMENTS +
            " WHERE entity_type='supplier' AND entity_id=? " +
            "ORDER BY created_at DESC";
        return queryTable(sql, new String[]{String.valueOf(supplierId), String.valueOf(supplierId)});
    }

    // ════════════════════════════════════════════════════════════
    // Reports helpers
    // ════════════════════════════════════════════════════════════

    public double getTotalSalesForPeriod(String from, String to) {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total),0) FROM " + TABLE_INVOICES +
                " WHERE status != 'returned' AND DATE(created_at) BETWEEN ? AND ?",
                new String[]{from, to});
            double v = 0; if (c.moveToFirst()) v = c.getDouble(0); c.close(); return v;
        } catch (Exception e) { return 0; }
    }

    public double getTotalProfitForPeriod(String from, String to) {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM((ii.price - ii.buy_cost_per_unit) * ii.qty),0) " +
                "FROM " + TABLE_INVOICE_ITEMS + " ii " +
                "JOIN " + TABLE_INVOICES + " i ON ii.invoice_id = i.id " +
                "WHERE i.status != 'returned' AND DATE(i.created_at) BETWEEN ? AND ?",
                new String[]{from, to});
            double v = 0; if (c.moveToFirst()) v = c.getDouble(0); c.close(); return v;
        } catch (Exception e) { return 0; }
    }

    public List<HashMap<String, String>> getSalesByCategory(String from, String to) {
        String sql = "SELECT p.category, COALESCE(SUM(ii.total),0) AS total_sales, " +
            "COALESCE(SUM(ii.qty),0) AS total_qty " +
            "FROM " + TABLE_INVOICE_ITEMS + " ii " +
            "JOIN " + TABLE_INVOICES + " i ON ii.invoice_id = i.id " +
            "LEFT JOIN " + TABLE_PRODUCTS + " p ON ii.product_id = p.id " +
            "WHERE i.status != 'returned' AND DATE(i.created_at) BETWEEN ? AND ? " +
            "GROUP BY p.category ORDER BY total_sales DESC";
        return queryTable(sql, new String[]{from, to});
    }

    private String getCurrentDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(new java.util.Date());
    }

    public boolean updateProductPrice(String productId, double newPrice) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("price", newPrice);
            return getWritableDatabase().update(TABLE_PRODUCTS, cv, "id=?",
                new String[]{productId}) > 0;
        } catch (Exception e) { Log.e(TAG, "updateProductPrice: " + e.getMessage()); return false; }
    }

    public boolean addProductQuantity(String productId, int qty) {
        try {
            getWritableDatabase().execSQL(
                "UPDATE " + TABLE_PRODUCTS + " SET qty = qty + ? WHERE id = ?",
                new Object[]{qty, productId});
            return true;
        } catch (Exception e) { Log.e(TAG, "addProductQuantity: " + e.getMessage()); return false; }
    }

    public List<HashMap<String, String>> getInvoiceItemsByProduct(String productId) {
        return queryTable(
            "SELECT ii.*, i.created_at FROM " + TABLE_INVOICE_ITEMS + " ii " +
            "JOIN " + TABLE_INVOICES + " i ON ii.invoice_id = i.id " +
            "WHERE ii.product_id = ? ORDER BY i.created_at DESC LIMIT 50",
            new String[]{productId});
    }
}
