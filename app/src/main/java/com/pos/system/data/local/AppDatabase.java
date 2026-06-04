package com.pos.system.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.pos.system.data.local.dao.CustomerDao;
import com.pos.system.data.local.dao.ExpenseDao;
import com.pos.system.data.local.dao.InvoiceDao;
import com.pos.system.data.local.dao.LocationDao;
import com.pos.system.data.local.dao.ProductDao;
import com.pos.system.data.local.dao.PurchaseOrderDao;
import com.pos.system.data.local.dao.ShiftDao;
import com.pos.system.data.local.dao.SupplierDao;
import com.pos.system.data.local.entity.CustomerEntity;
import com.pos.system.data.local.entity.ExpenseEntity;
import com.pos.system.data.local.entity.InvoiceEntity;
import com.pos.system.data.local.entity.InvoiceItemEntity;
import com.pos.system.data.local.entity.LocationEntity;
import com.pos.system.data.local.entity.ProductEntity;
import com.pos.system.data.local.entity.PurchaseOrderEntity;
import com.pos.system.data.local.entity.ShiftEntity;
import com.pos.system.data.local.entity.SupplierEntity;

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
        PurchaseOrderEntity.class
    },
    version = 1,
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

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DB_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
