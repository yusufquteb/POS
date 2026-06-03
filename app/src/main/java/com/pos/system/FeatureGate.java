package com.pos.system;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * FeatureGate — controls access to premium features.
 *
 * Free tier (forever):
 *   - Full POS, invoicing, expenses, basic reports (last 30 days)
 *   - Up to FREE_PRODUCTS_LIMIT products
 *   - Up to FREE_CUSTOMERS_LIMIT customers
 *   - Local backup
 *
 * 30-day trial (from first install):
 *   - All premium features unlocked
 *
 * Premium (paid):
 *   - Returns, Shifts, Debt Management, Purchase Orders
 *   - Unlimited products and customers
 *   - Cloud backup
 *   - Full report history
 */
public final class FeatureGate {

    private FeatureGate() {}

    public static final int FREE_PRODUCTS_LIMIT  = 200;
    public static final int FREE_CUSTOMERS_LIMIT = 100;
    public static final int FREE_TRIAL_DAYS      = 30;

    private static final String PREFS_NAME   = "smartpos_gate";
    private static final String KEY_TRIAL    = "trial_start";

    // ── Core check ────────────────────────────────────────────────────────────

    /** Returns true when all features are accessible (premium user OR in trial). */
    public static boolean isUnlocked(Context ctx) {
        return isPremium(ctx) || isInTrial(ctx);
    }

    /** Remaining trial days; -1 if already premium. */
    public static int remainingTrialDays(Context ctx) {
        if (isPremium(ctx)) return -1;
        long start = getOrCreateTrialStart(ctx);
        long elapsed = (System.currentTimeMillis() - start) / (24L * 60 * 60 * 1000);
        return (int) Math.max(0, FREE_TRIAL_DAYS - elapsed);
    }

    // ── Limit checks ─────────────────────────────────────────────────────────

    public static boolean canAddProduct(Context ctx) {
        if (isUnlocked(ctx)) return true;
        try {
            DBHelper db = new DBHelper(ctx);
            int count = db.getTotalProductsCount();
            db.close();
            return count < FREE_PRODUCTS_LIMIT;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean canAddCustomer(Context ctx) {
        if (isUnlocked(ctx)) return true;
        try {
            DBHelper db = new DBHelper(ctx);
            int count = db.getTotalCustomersCount();
            db.close();
            return count < FREE_CUSTOMERS_LIMIT;
        } catch (Exception e) {
            return true;
        }
    }

    // ── Upgrade dialogs ───────────────────────────────────────────────────────

    /**
     * Shows upgrade Bottom Sheet — does NOT finish the calling activity.
     * Call at the top of onCreate() for premium-only screens.
     */
    public static void requirePremium(Activity activity, String featureNameAr) {
        if (isUnlocked(activity)) return;

        new MaterialAlertDialogBuilder(activity)
            .setTitle("🔒 " + featureNameAr)
            .setMessage(
                "هذه الميزة متاحة لمشتركي SmartPOS Premium.\n\n" +
                "✓ إدارة المرتجعات والاستبدال\n" +
                "✓ إدارة الشيفتات وجرد الخزينة\n" +
                "✓ إدارة ديون العملاء والموردين\n" +
                "✓ أوامر الشراء من الموردين\n" +
                "✓ نسخ احتياطي على السحابة\n" +
                "✓ تقارير غير محدودة\n\n" +
                "اشترك الآن بـ 99 ج.م / شهر"
            )
            .setPositiveButton("اشترك الآن", (d, w) ->
                activity.startActivity(new Intent(activity, ActivitySettingsActivity.class)))
            .setNegativeButton("رجوع", null)  // لا يُغلق الشاشة — فقط يغلق الديالوج
            .setCancelable(true)
            .show();
    }

    /** Shows limit-reached dialog for products. */
    public static void showProductLimitDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
            .setTitle("حد المنتجات المجاني")
            .setMessage(
                "وصلت إلى الحد المجاني (" + FREE_PRODUCTS_LIMIT + " منتج).\n\n" +
                "اشترك في Premium للحصول على منتجات غير محدودة."
            )
            .setPositiveButton("اشترك الآن", (d, w) ->
                activity.startActivity(new Intent(activity, ActivitySettingsActivity.class)))
            .setNegativeButton("رجوع", null)
            .show();
    }

    /** Shows limit-reached dialog for customers. */
    public static void showCustomerLimitDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
            .setTitle("حد العملاء المجاني")
            .setMessage(
                "وصلت إلى الحد المجاني (" + FREE_CUSTOMERS_LIMIT + " عميل).\n\n" +
                "اشترك في Premium للحصول على عملاء غير محدودين."
            )
            .setPositiveButton("اشترك الآن", (d, w) ->
                activity.startActivity(new Intent(activity, ActivitySettingsActivity.class)))
            .setNegativeButton("رجوع", null)
            .show();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static boolean isPremium(Context ctx) {
        try {
            DBHelper db = new DBHelper(ctx);
            boolean premium = db.isPremiumUser();
            db.close();
            return premium;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isInTrial(Context ctx) {
        long start = getOrCreateTrialStart(ctx);
        long elapsed = (System.currentTimeMillis() - start) / (24L * 60 * 60 * 1000);
        return elapsed < FREE_TRIAL_DAYS;
    }

    /**
     * يحصل على تاريخ بدء التجربة من ثلاثة مصادر بالترتيب:
     * 1. SharedPreferences الرئيسي
     * 2. SharedPreferences مرتبط بـ ANDROID_ID (يبقى بعد مسح التطبيق على نفس الجهاز)
     * 3. قاعدة البيانات
     * إذا لم يُوجد في أيٍّ منها يُنشأ جديد ويُحفظ في الثلاثة.
     */
    private static long getOrCreateTrialStart(Context ctx) {
        // المصدر 1: SharedPreferences الرئيسي
        SharedPreferences mainPrefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long start = mainPrefs.getLong(KEY_TRIAL, 0L);
        if (start > 0L) return start;

        // المصدر 2: SharedPreferences مرتبط بـ ANDROID_ID
        String deviceId = getDeviceId(ctx);
        SharedPreferences devicePrefs = ctx.getSharedPreferences(
            "spos_" + deviceId, Context.MODE_PRIVATE);
        start = devicePrefs.getLong("t0", 0L);
        if (start > 0L) {
            mainPrefs.edit().putLong(KEY_TRIAL, start).apply();
            return start;
        }

        // المصدر 3: قاعدة البيانات
        try {
            DBHelper db = new DBHelper(ctx);
            String stored = db.getStoreSetting("trial_start_ts");
            db.close();
            if (stored != null && !stored.isEmpty()) {
                start = Long.parseLong(stored);
                mainPrefs.edit().putLong(KEY_TRIAL, start).apply();
                devicePrefs.edit().putLong("t0", start).apply();
                return start;
            }
        } catch (Exception ignored) {}

        // لا يوجد في أي مصدر — إنشاء جديد
        start = System.currentTimeMillis();
        mainPrefs.edit().putLong(KEY_TRIAL, start).apply();
        devicePrefs.edit().putLong("t0", start).apply();
        try {
            DBHelper db = new DBHelper(ctx);
            db.saveTrialStart(start);
            db.close();
        } catch (Exception ignored) {}
        return start;
    }

    private static String getDeviceId(Context ctx) {
        try {
            String id = android.provider.Settings.Secure.getString(
                ctx.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
            if (id != null && !id.isEmpty()) {
                return String.valueOf(Math.abs(id.hashCode()) % 100000);
            }
        } catch (Exception ignored) {}
        return "00000";
    }
}
