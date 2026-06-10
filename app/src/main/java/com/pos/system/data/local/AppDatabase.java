package com.pos.system.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.pos.system.data.local.dao.CustomerDao;
import com.pos.system.data.local.dao.DirectPaymentDao;
import com.pos.system.data.local.dao.ExpenseDao;
import com.pos.system.data.local.dao.InvoiceDao;
import com.pos.system.data.local.dao.LocationDao;
import com.pos.system.data.local.dao.PriceQuoteDao;
import com.pos.system.data.local.dao.ProductDao;
import com.pos.system.data.local.dao.PurchaseOrderDao;
import com.pos.system.data.local.dao.ShiftDao;
import com.pos.system.data.local.dao.SupplierDao;
import com.pos.system.data.local.dao.WalletTransactionDao;
import com.pos.system.data.local.entity.CustomerEntity;
import com.pos.system.data.local.entity.DirectPaymentEntity;
import com.pos.system.data.local.entity.ExpenseEntity;
import com.pos.system.data.local.entity.InvoiceEntity;
import com.pos.system.data.local.entity.InvoiceItemEntity;
import com.pos.system.data.local.entity.LocationEntity;
import com.pos.system.data.local.entity.PriceQuoteEntity;
import com.pos.system.data.local.entity.PriceQuoteItemEntity;
import com.pos.system.data.local.entity.ProductEntity;
import com.pos.system.data.local.entity.PurchaseOrderEntity;
import com.pos.system.data.local.entity.ShiftEntity;
import com.pos.system.data.local.entity.SupplierEntity;
import com.pos.system.data.local.entity.WalletTransactionEntity;

@Database(
    entities = {
        ProductEntity.class,
        CustomerEntity.class,
        SupplierEntity.class,
        InvoiceEntity.class,
        InvoiceItemEntity.class,
        ExpenseEntity.class,
        ShiftEntity.class,
        LocationEntity.class,
        PurchaseOrderEntity.class,
        WalletTransactionEntity.class,
        DirectPaymentEntity.class,
        PriceQuoteEntity.class,
        PriceQuoteItemEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "SmartPOS_Room.db";

    public abstract ProductDao productDao();
    public abstract CustomerDao customerDao();
    public abstract SupplierDao supplierDao();
    public abstract InvoiceDao invoiceDao();
    public abstract ExpenseDao expenseDao();
    public abstract ShiftDao shiftDao();
    public abstract LocationDao locationDao();
    public abstract PurchaseOrderDao purchaseOrderDao();
    public abstract WalletTransactionDao walletTransactionDao();
    public abstract DirectPaymentDao directPaymentDao();
    public abstract PriceQuoteDao priceQuoteDao();

    private static volatile AppDatabase INSTANCE;

    /** Migration 1 → 2: إضافة الجداول والحقول الجديدة */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // جداول جديدة
            db.execSQL("CREATE TABLE IF NOT EXISTS `wallet_transactions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`type` TEXT NOT NULL DEFAULT 'OUT'," +
                "`amount` REAL NOT NULL DEFAULT 0.0," +
                "`note` TEXT," +
                "`date` TEXT," +
                "`created_at` TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `direct_payments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`entity_type` TEXT," +
                "`entity_id` INTEGER NOT NULL DEFAULT 0," +
                "`entity_name` TEXT," +
                "`type` TEXT NOT NULL DEFAULT 'OUT'," +
                "`amount` REAL NOT NULL DEFAULT 0.0," +
                "`note` TEXT," +
                "`date` TEXT," +
                "`created_at` TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `price_quotes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`quote_number` TEXT," +
                "`customer_id` INTEGER NOT NULL DEFAULT 0," +
                "`customer_name` TEXT," +
                "`subtotal` REAL NOT NULL DEFAULT 0.0," +
                "`discount` REAL NOT NULL DEFAULT 0.0," +
                "`tax` REAL NOT NULL DEFAULT 0.0," +
                "`total` REAL NOT NULL DEFAULT 0.0," +
                "`status` TEXT NOT NULL DEFAULT 'pending'," +
                "`notes` TEXT," +
                "`created_at` TEXT)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_price_quotes_quote_number` ON `price_quotes` (`quote_number`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `price_quote_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`quote_id` INTEGER NOT NULL," +
                "`product_id` INTEGER NOT NULL DEFAULT 0," +
                "`barcode` TEXT," +
                "`name` TEXT," +
                "`price` REAL NOT NULL DEFAULT 0.0," +
                "`qty` INTEGER NOT NULL DEFAULT 1," +
                "`total` REAL NOT NULL DEFAULT 0.0," +
                "FOREIGN KEY(`quote_id`) REFERENCES `price_quotes`(`id`) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_quote_items_quote_id` ON `price_quote_items` (`quote_id`)");

            // حقول جديدة في الجداول الموجودة
            db.execSQL("ALTER TABLE `invoices` ADD COLUMN `paid_amount` REAL NOT NULL DEFAULT 0.0");
            db.execSQL("ALTER TABLE `invoices` ADD COLUMN `remaining_amount` REAL NOT NULL DEFAULT 0.0");
            db.execSQL("ALTER TABLE `invoices` ADD COLUMN `invoice_note` TEXT");
            db.execSQL("ALTER TABLE `invoices` ADD COLUMN `invoice_date` TEXT");
            db.execSQL("ALTER TABLE `invoices` ADD COLUMN `supplier_id` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `invoices` ADD COLUMN `supplier_name` TEXT");

            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `expense_type` TEXT NOT NULL DEFAULT 'OUT'");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DB_NAME
                    )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
