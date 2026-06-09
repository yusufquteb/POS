package com.pos.system.managers;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pos.system.DBHelper;
import com.pos.system.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * SafeBackupManager - مدير النسخ الاحتياطي الآمن
 * 
 * الميزات:
 * - استعادة آمنة (Merge بدون حذف)
 * - اختيار جداول محددة للاستعادة
 * - نسخ احتياطي شامل
 * - معالجة الأخطاء
 * 
 * @author POS System
 * @version 1.0
 * @since 2026-02-17
 */
public class SafeBackupManager {

    private static final String TAG = "SafeBackupManager";
    private static final String BACKUP_DIR = "POSBackups";
    private static final String BACKUP_EXTENSION = ".json";
    
    private final Context context;
    private final DBHelper dbHelper;
    
    // الجداول المتاحة
    public static final int TABLE_PRODUCTS = 0;
    public static final int TABLE_CUSTOMERS = 1;
    public static final int TABLE_SUPPLIERS = 2;
    public static final int TABLE_INVOICES = 3;
    public static final int TABLE_EXPENSES = 4;
    
    public SafeBackupManager(@NonNull Context context, @NonNull DBHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }
    
    /**
     * إنشاء نسخة احتياطية
     */
    public File createBackup() throws IOException {
        try {
            // إنشاء مجلد النسخ الاحتياطي
            File backupDir = new File(context.getExternalFilesDir(null), BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // إنشاء اسم الملف
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String fileName = "backup_" + sdf.format(new Date()) + BACKUP_EXTENSION;
            File backupFile = new File(backupDir, fileName);
            
            // جمع البيانات
            JSONObject backupData = new JSONObject();
            backupData.put("version", "1.0");
            backupData.put("app_name", "POS System");
            backupData.put("backup_date", System.currentTimeMillis());
            
            // إضافة البيانات
            backupData.put("products", dbHelper.getAllProductsAsJSON());
            backupData.put("customers", dbHelper.getAllCustomersAsJSON());
            backupData.put("suppliers", dbHelper.getAllSuppliersAsJSON());
            backupData.put("invoices", dbHelper.getAllInvoicesAsJSON());
            backupData.put("expenses", dbHelper.getAllExpensesAsJSON());
            
            // حفظ الملف
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(backupData.toString(2).getBytes());
            fos.close();
            
            return backupFile;
            
        } catch (Exception e) {
            throw new IOException("فشل إنشاء النسخة الاحتياطية: " + e.getMessage(), e);
        }
    }
    
    /**
     * عرض Dialog لاختيار البيانات المراد استعادتها
     */
    public void showRestoreDialog(@NonNull File backupFile, @NonNull RestoreCallback callback) {
        String[] tables = {
            "المنتجات",
            "العملاء",
            "الموردين",
            "الفواتير",
            "المصروفات"
        };
        
        boolean[] selectedTables = new boolean[tables.length];
        // تحديد الكل افتراضياً
        for (int i = 0; i < selectedTables.length; i++) {
            selectedTables[i] = true;
        }
        
        new MaterialAlertDialogBuilder(context)
                .setTitle("اختر البيانات للاستعادة")
                .setMultiChoiceItems(tables, selectedTables, 
                        (dialog, which, isChecked) -> selectedTables[which] = isChecked)
                .setPositiveButton("استعادة", (d, w) -> {
                    try {
                        restoreBackup(backupFile, selectedTables, callback);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton("تحديد الكل", (d, w) -> {
                    // لا شيء - سيتم التعامل معه في onShow
                })
                .show();
    }
    
    /**
     * استعادة النسخة الاحتياطية
     */
    private void restoreBackup(@NonNull File backupFile, 
                               @NonNull boolean[] selectedTables,
                               @NonNull RestoreCallback callback) {
        try {
            // قراءة الملف
            FileInputStream fis = new FileInputStream(backupFile);
            byte[] buffer = new byte[(int) backupFile.length()];
            fis.read(buffer);
            fis.close();
            
            String jsonStr = new String(buffer);
            JSONObject backupData = new JSONObject(jsonStr);
            
            int totalRestored = 0;
            int totalSkipped = 0;
            
            // استعادة المنتجات
            if (selectedTables[TABLE_PRODUCTS]) {
                JSONArray products = backupData.getJSONArray("products");
                int[] result = mergeProducts(products);
                totalRestored += result[0];
                totalSkipped += result[1];
            }
            
            // استعادة العملاء
            if (selectedTables[TABLE_CUSTOMERS]) {
                JSONArray customers = backupData.getJSONArray("customers");
                int[] result = mergeCustomers(customers);
                totalRestored += result[0];
                totalSkipped += result[1];
            }
            
            // استعادة الموردين
            if (selectedTables[TABLE_SUPPLIERS]) {
                JSONArray suppliers = backupData.getJSONArray("suppliers");
                int[] result = mergeSuppliers(suppliers);
                totalRestored += result[0];
                totalSkipped += result[1];
            }
            
            // استعادة الفواتير
            if (selectedTables[TABLE_INVOICES]) {
                JSONArray invoices = backupData.getJSONArray("invoices");
                int[] result = mergeInvoices(invoices);
                totalRestored += result[0];
                totalSkipped += result[1];
            }
            
            // استعادة المصروفات
            if (selectedTables[TABLE_EXPENSES]) {
                JSONArray expenses = backupData.getJSONArray("expenses");
                int[] result = mergeExpenses(expenses);
                totalRestored += result[0];
                totalSkipped += result[1];
            }
            
            callback.onSuccess(totalRestored, totalSkipped);
            
        } catch (Exception e) {
            callback.onError(e);
        }
    }
    
    /**
     * دمج المنتجات (بدون حذف الموجود)
     * @return [restored, skipped]
     */
    private int[] mergeProducts(@NonNull JSONArray products) throws Exception {
        int restored = 0;
        int skipped = 0;
        
        for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);
            String barcode = product.getString("barcode");
            
            // فحص إذا كان المنتج موجود
            if (dbHelper.productExists(barcode)) {
                // تخطي المنتج الموجود
                skipped++;
            } else {
                // إضافة منتج جديد
                dbHelper.insertProductFromJSON(product);
                restored++;
            }
        }
        
        return new int[]{restored, skipped};
    }
    
    /**
     * دمج العملاء
     */
    private int[] mergeCustomers(@NonNull JSONArray customers) throws Exception {
        int restored = 0;
        int skipped = 0;
        
        for (int i = 0; i < customers.length(); i++) {
            JSONObject customer = customers.getJSONObject(i);
            String phone = customer.getString("phone");
            
            if (dbHelper.customerExistsByPhone(phone)) {
                skipped++;
            } else {
                dbHelper.insertCustomerFromJSON(customer);
                restored++;
            }
        }
        
        return new int[]{restored, skipped};
    }
    
    /**
     * دمج الموردين
     */
    private int[] mergeSuppliers(@NonNull JSONArray suppliers) throws Exception {
        int restored = 0;
        int skipped = 0;
        
        for (int i = 0; i < suppliers.length(); i++) {
            JSONObject supplier = suppliers.getJSONObject(i);
            String phone = supplier.getString("phone");
            
            if (dbHelper.supplierExistsByPhone(phone)) {
                skipped++;
            } else {
                dbHelper.insertSupplierFromJSON(supplier);
                restored++;
            }
        }
        
        return new int[]{restored, skipped};
    }
    
    /**
     * دمج الفواتير
     */
    private int[] mergeInvoices(@NonNull JSONArray invoices) throws Exception {
        int restored = 0;
        int skipped = 0;
        
        for (int i = 0; i < invoices.length(); i++) {
            JSONObject invoice = invoices.getJSONObject(i);
            String invoiceNumber = invoice.getString("invoice_number");
            
            if (dbHelper.invoiceExists(invoiceNumber)) {
                skipped++;
            } else {
                dbHelper.insertInvoiceFromJSON(invoice);
                restored++;
            }
        }
        
        return new int[]{restored, skipped};
    }
    
    /**
     * دمج المصروفات
     */
    private int[] mergeExpenses(@NonNull JSONArray expenses) throws Exception {
        int restored = 0;
        int skipped = 0;
        
        for (int i = 0; i < expenses.length(); i++) {
            JSONObject expense = expenses.getJSONObject(i);
            // المصروفات ليس لها معرّف فريد، نضيف الكل
            dbHelper.insertExpenseFromJSON(expense);
            restored++;
        }
        
        return new int[]{restored, skipped};
    }
    
    /**
     * الحصول على قائمة النسخ الاحتياطية المتاحة
     */
    @NonNull
    public File[] getAvailableBackups() {
        File backupDir = new File(context.getExternalFilesDir(null), BACKUP_DIR);
        
        if (!backupDir.exists()) {
            return new File[0];
        }
        
        File[] files = backupDir.listFiles((dir, name) -> 
                name.endsWith(BACKUP_EXTENSION));
        
        return files != null ? files : new File[0];
    }
    
    /**
     * حذف نسخة احتياطية
     */
    public boolean deleteBackup(@NonNull File backupFile) {
        return backupFile.delete();
    }
    
    /**
     * الحصول على حجم النسخة الاحتياطية
     */
    public long getBackupSize(@NonNull File backupFile) {
        return backupFile.length();
    }
    
    /**
     * الحصول على تاريخ النسخة الاحتياطية
     */
    @NonNull
    public String getBackupDate(@NonNull File backupFile) {
        try {
            String fileName = backupFile.getName();
            // backup_2026-02-17_15-30-00.json
            String dateStr = fileName.replace("backup_", "").replace(BACKUP_EXTENSION, "");
            dateStr = dateStr.replace("_", " ");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return "تاريخ غير معروف";
        }
    }
    
    /**
     * واجهة Callback للاستعادة
     */
    public interface RestoreCallback {
        void onSuccess(int totalRestored, int totalSkipped);
        void onError(Exception e);
    }
    
    /**
     * مساعد: تنسيق حجم الملف
     */
    @NonNull
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
