package com.pos.system.managers;

import android.content.Context;
import com.pos.system.DBHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuditLogger {

    private final DBHelper       dbHelper;
    private final UserManager    userManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final String ACTION_CREATE  = "CREATE";
    public static final String ACTION_UPDATE  = "UPDATE";
    public static final String ACTION_DELETE  = "DELETE";
    public static final String ACTION_LOGIN   = "LOGIN";
    public static final String ACTION_LOGOUT  = "LOGOUT";
    public static final String ACTION_BACKUP  = "BACKUP";
    public static final String ACTION_RESTORE = "RESTORE";
    public static final String ACTION_SALE    = "SALE";
    public static final String ACTION_RETURN  = "RETURN";
    public static final String ACTION_PAYMENT = "PAYMENT";

    public AuditLogger(Context context) {
        Context app    = context.getApplicationContext();
        this.dbHelper  = new DBHelper(app);
        this.userManager = new UserManager(app);
    }

    public void log(String action, String tableName, String recordId, String description) {
        long   userId   = userManager.getCurrentUserId();
        String userName = userManager.getCurrentUserName();
        executor.execute(() ->
            dbHelper.logAction(userId, userName, action, tableName, recordId, description));
    }

    public void logCreate(String tableName, String recordId, String description) {
        log(ACTION_CREATE, tableName, recordId, description);
    }

    public void logUpdate(String tableName, String recordId, String description) {
        log(ACTION_UPDATE, tableName, recordId, description);
    }

    public void logDelete(String tableName, String recordId, String description) {
        log(ACTION_DELETE, tableName, recordId, description);
    }

    public void logSale(String invoiceNumber, double total) {
        log(ACTION_SALE, "invoices", invoiceNumber,
            "فاتورة رقم: " + invoiceNumber + " - الإجمالي: " + total);
    }

    public void logReturn(String returnNumber, double total) {
        log(ACTION_RETURN, "returns", returnNumber,
            "مرتجع رقم: " + returnNumber + " - القيمة: " + total);
    }

    public void logLogin(String userName) {
        log(ACTION_LOGIN, "users", "0", "تسجيل دخول: " + userName);
    }

    public void logLogout(String userName) {
        log(ACTION_LOGOUT, "users", "0", "تسجيل خروج: " + userName);
    }

    public void logBackup(String filePath) {
        log(ACTION_BACKUP, "backup", "0", "نسخة احتياطية: " + filePath);
    }

    public void logRestore(String filePath) {
        log(ACTION_RESTORE, "backup", "0", "استعادة نسخة: " + filePath);
    }
}
