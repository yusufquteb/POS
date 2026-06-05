package com.pos.system;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleBackupHelper {

    private static final String TAG = "SimpleBackupHelper";
    
    private Activity activity;
    private String dbPath;

    public SimpleBackupHelper(Activity activity, String dbPath) {
        this.activity = activity;
        this.dbPath = dbPath;
    }

    /**
     * نسخ احتياطي محلي
     */
    public boolean backupLocally() {
        try {
            File dbFile = new File(dbPath);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String backupName = "POS_Backup_" + timestamp + ".db";
            
            File backupDir = new File(activity.getExternalFilesDir(null), "Backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            File backupFile = new File(backupDir, backupName);
            
            FileInputStream fis = new FileInputStream(dbFile);
            FileOutputStream fos = new FileOutputStream(backupFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            fos.flush();
            fos.close();
            fis.close();
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * مشاركة النسخة الاحتياطية
     */
    public void shareBackup() {
        try {
            File dbFile = new File(dbPath);
            Uri uri = FileProvider.getUriForFile(
                activity, 
                activity.getPackageName() + ".provider", 
                dbFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/octet-stream");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - نظام POS");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            activity.startActivity(Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing backup", e);
        }
    }

    /**
     * رفع إلى Google Drive عبر تطبيق Drive
     */
    public void uploadToDrive() {
        try {
            // نسخ محلي أولاً
            backupLocally();
            
            // فتح تطبيق Google Drive
            File backupDir = new File(activity.getExternalFilesDir(null), "Backups");
            File[] backups = backupDir.listFiles();
            
            if (backups != null && backups.length > 0) {
                // أحدث نسخة
                File latestBackup = backups[backups.length - 1];
                
                Uri uri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    latestBackup
                );
                
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setPackage("com.google.android.apps.docs"); // Google Drive
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                activity.startActivity(intent);
            }
            
        } catch (Exception e) {
            // إذا لم يكن Drive مثبت، استخدم المشاركة العادية
            shareBackup();
        }
    }

    /**
     * الحصول على عدد النسخ الاحتياطية
     */
    public int getBackupCount() {
        File backupDir = new File(activity.getExternalFilesDir(null), "Backups");
        if (backupDir.exists()) {
            File[] backups = backupDir.listFiles();
            return backups != null ? backups.length : 0;
        }
        return 0;
    }

    /**
     * حذف النسخ القديمة (الاحتفاظ بآخر 5)
     */
    public void cleanOldBackups() {
        try {
            File backupDir = new File(activity.getExternalFilesDir(null), "Backups");
            if (backupDir.exists()) {
                File[] backups = backupDir.listFiles();
                if (backups != null && backups.length > 5) {
                    // ترتيب حسب التاريخ
                    java.util.Arrays.sort(backups, (f1, f2) -> 
                        Long.compare(f1.lastModified(), f2.lastModified()));
                    
                    // حذف الأقدم
                    for (int i = 0; i < backups.length - 5; i++) {
                        backups[i].delete();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning old backups", e);
        }
    }
}