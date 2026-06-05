package com.pos.system.di;

import android.content.Context;

import com.pos.system.DBHelper;
import com.pos.system.data.local.AppDatabase;
import com.pos.system.data.local.dao.CustomerDao;
import com.pos.system.data.local.dao.ExpenseDao;
import com.pos.system.data.local.dao.InvoiceDao;
import com.pos.system.data.local.dao.LocationDao;
import com.pos.system.data.local.dao.ProductDao;
import com.pos.system.data.local.dao.PurchaseOrderDao;
import com.pos.system.data.local.dao.ShiftDao;
import com.pos.system.data.local.dao.SupplierDao;
import com.pos.system.data.repository.ProductRepositoryImpl;
import com.pos.system.domain.repository.ProductRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DatabaseModule {

    @Provides
    @Singleton
    static DBHelper provideDbHelper(@ApplicationContext Context context) {
        return new DBHelper(context);
    }

    @Provides
    @Singleton
    static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    @Singleton
    static ProductDao provideProductDao(AppDatabase db) {
        return db.productDao();
    }

    @Provides
    @Singleton
    static CustomerDao provideCustomerDao(AppDatabase db) {
        return db.customerDao();
    }

    @Provides
    @Singleton
    static SupplierDao provideSupplierDao(AppDatabase db) {
        return db.supplierDao();
    }

    @Provides
    @Singleton
    static InvoiceDao provideInvoiceDao(AppDatabase db) {
        return db.invoiceDao();
    }

    @Provides
    @Singleton
    static ExpenseDao provideExpenseDao(AppDatabase db) {
        return db.expenseDao();
    }

    @Provides
    @Singleton
    static ShiftDao provideShiftDao(AppDatabase db) {
        return db.shiftDao();
    }

    @Provides
    @Singleton
    static LocationDao provideLocationDao(AppDatabase db) {
        return db.locationDao();
    }

    @Provides
    @Singleton
    static PurchaseOrderDao providePurchaseOrderDao(AppDatabase db) {
        return db.purchaseOrderDao();
    }

    @Binds
    @Singleton
    abstract ProductRepository bindProductRepository(ProductRepositoryImpl impl);
}
