package com.pos.system.managers;

import android.content.Context;
import com.pos.system.DBHelper;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupManager {
    
    private static final String TAG = "BackupManager";
    private static final String BACKUP_FOLDER = "SmartPOS_Backups";
    
    private Context context;
    private DBHelper dbHelper;
    
    public BackupManager(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }
    
    /**
     * إنشاء نسخة احتياطية
     */
    public boolean createBackup() {
        try {
            // 1. الحصول على مسار قاعدة البيانات
            String dbPath = dbHelper.getDatabasePath();
            File dbFile = new File(dbPath);
            
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file not found");
                return false;
            }
            
            // 2. إنشاء مجلد النسخ الاحتياطية (app-scoped storage, no special permission needed)
            File backupFolder = new File(
                context.getExternalFilesDir(null),
                BACKUP_FOLDER
            );
            
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            // 3. إنشاء اسم ملف النسخة الاحتياطية
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String backupFileName = "backup_" + timestamp + ".db";
            
            File backupFile = new File(backupFolder, backupFileName);
            
            // 4. نسخ الملف
            copyFile(dbFile, backupFile);
            
            Log.d(TAG, "Backup created successfully: " + backupFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating backup: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * استعادة نسخة احتياطية
     */
    public boolean restoreBackup(File backupFile) {
        try {
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file not found");
                return false;
            }
            
            // إغلاق قاعدة البيانات الحالية
            dbHelper.close();
            
            // الحصول على مسار قاعدة البيانات
            String dbPath = dbHelper.getDatabasePath();
            File dbFile = new File(dbPath);
            
            // نسخ النسخة الاحتياطية
            copyFile(backupFile, dbFile);
            
            Log.d(TAG, "Backup restored successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error restoring backup: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * نسخ ملف
     */
    private void copyFile(File source, File destination) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            
            byte[] buffer = new byte[1024];
            int length;
            
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            fos.flush();
            
        } finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
    }
    
    /**
     * الحصول على قائمة النسخ الاحتياطية
     */
    public File[] getBackupFiles() {
        File backupFolder = new File(
            context.getExternalFilesDir(null),
            BACKUP_FOLDER
        );

        if (!backupFolder.exists()) {
            return new File[0];
        }

        return backupFolder.listFiles((dir, name) -> name.endsWith(".db"));
    }

    /**
     * يتحقق من سلامة ملف النسخة الاحتياطية بفتحه كـ SQLite وقراءة الجداول الأساسية.
     * يُعيد true إذا كانت النسخة صالحة.
     */
    public boolean verifyBackup(File backupFile) {
        if (backupFile == null || !backupFile.exists() || backupFile.length() == 0) {
            Log.e(TAG, "verifyBackup: file missing or empty");
            return false;
        }
        android.database.sqlite.SQLiteDatabase testDb = null;
        try {
            testDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                backupFile.getAbsolutePath(), null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY);

            // تحقق من وجود الجداول الأساسية
            String[] required = {"products", "invoices", "customers", "invoice_items"};
            for (String table : required) {
                android.database.Cursor c = testDb.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{table});
                boolean exists = c.moveToFirst() && c.getInt(0) > 0;
                c.close();
                if (!exists) {
                    Log.e(TAG, "verifyBackup: missing table: " + table);
                    return false;
                }
            }

            Log.d(TAG, "verifyBackup: OK — " + backupFile.getName()
                + " (" + backupFile.length() / 1024 + " KB)");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "verifyBackup: " + e.getMessage(), e);
            return false;
        } finally {
            if (testDb != null) try { testDb.close(); } catch (Exception ignored) {}
        }
    }
}