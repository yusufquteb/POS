package com.pos.system.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.pos.system.DBHelper;
import java.util.HashMap;

public class UserManager {

    private static final String PREFS_NAME = "user_session";
    private static final String KEY_USER_ID   = "current_user_id";
    private static final String KEY_USER_NAME = "current_user_name";
    private static final String KEY_USER_ROLE = "current_user_role";

    public static final String ROLE_ADMIN   = "admin";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_CASHIER = "cashier";

    public static final String PERM_DELETE_PRODUCT      = "delete_product";
    public static final String PERM_VIEW_REPORTS        = "view_reports";
    public static final String PERM_VIEW_COSTS          = "view_costs";
    public static final String PERM_MANAGE_USERS        = "manage_users";
    public static final String PERM_APPLY_DISCOUNT      = "apply_discount";
    public static final String PERM_CANCEL_INVOICE      = "cancel_invoice";
    public static final String PERM_MANAGE_CHECKS       = "manage_checks";
    public static final String PERM_MANAGE_INSTALLMENTS = "manage_installments";
    public static final String PERM_VIEW_CASH_DRAWER    = "view_cash_drawer";
    public static final String PERM_TRANSFER_CASH       = "transfer_cash";
    public static final String PERM_STOCK_COUNT         = "stock_count";
    public static final String PERM_BACKUP              = "backup";
    public static final String PERM_VIEW_DEBT           = "view_debt";

    private final Context  context;
    private final DBHelper dbHelper;

    public UserManager(Context context) {
        this.context  = context.getApplicationContext();
        this.dbHelper = new DBHelper(this.context);
    }

    public HashMap<String, String> loginByPin(String pin) {
        HashMap<String, String> user = dbHelper.getUserByPin(pin);
        if (user != null) {
            saveSession(
                Long.parseLong(user.getOrDefault("id", "0")),
                user.getOrDefault("name", ""),
                user.getOrDefault("role", ROLE_CASHIER)
            );
        }
        return user;
    }

    public void saveSession(long userId, String userName, String role) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID,   userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_ROLE, role)
            .apply();
    }

    public void clearSession() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply();
    }

    public boolean isSessionActive() {
        return getCurrentUserId() > 0;
    }

    public long getCurrentUserId() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, 0);
    }

    public String getCurrentUserName() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, "admin");
    }

    public String getCurrentUserRole() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ROLE, ROLE_ADMIN);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(getCurrentUserRole());
    }

    public boolean isManager() {
        String role = getCurrentUserRole();
        return ROLE_ADMIN.equals(role) || ROLE_MANAGER.equals(role);
    }

    public boolean hasPermission(String permission) {
        if (isAdmin()) return true;
        long userId = getCurrentUserId();
        if (userId == 0) return false;
        return dbHelper.hasPermission(userId, permission);
    }

    public static String getRoleDisplayName(String role) {
        if (role == null) return "كاشير";
        switch (role) {
            case ROLE_ADMIN:   return "مدير النظام";
            case ROLE_MANAGER: return "مدير";
            case ROLE_CASHIER: return "كاشير";
            default:           return role;
        }
    }

    public static int getRoleIcon(String role) {
        // Return drawable resource id hint (use star for admin, shield for manager, person for cashier)
        return 0;
    }
}
