package com.pos.system.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;

/**
 * ReviewManager - إدارة تقييمات التطبيق داخل التطبيق
 * النسخة 3.0 - مُفعَّل بالكامل مع Google Play In-App Review API
 *
 * المعايير لطلب التقييم:
 * - 5 مرات فتح التطبيق على الأقل
 * - 10 فواتير على الأقل
 * - 3 أيام من التثبيت
 * - لا يُطلب مرة أخرى لمدة 30 يوماً
 */
public class ReviewManager {

    private static final String TAG = "ReviewManager";
    private static final String PREFS = "review_prefs";

    private static final String KEY_INSTALL_DATE      = "install_date";
    private static final String KEY_LAUNCH_COUNT      = "launch_count";
    private static final String KEY_INVOICES_COUNT    = "invoices_count";
    private static final String KEY_LAST_REQUEST      = "last_review_request";
    private static final String KEY_REVIEW_COMPLETED  = "review_completed";

    private static final int  MIN_LAUNCHES       = 5;
    private static final int  MIN_INVOICES        = 10;
    private static final long MIN_INSTALL_DAYS    = 3L;
    private static final long REQUEST_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000; // 30 days

    private final Context context;
    private final SharedPreferences prefs;
    private com.google.android.play.core.review.ReviewManager playReviewManager;

    public ReviewManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs   = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_INSTALL_DATE)) {
            prefs.edit().putLong(KEY_INSTALL_DATE, System.currentTimeMillis()).apply();
        }
        try {
            playReviewManager = ReviewManagerFactory.create(context);
        } catch (Exception e) {
            Log.e(TAG, "ReviewManager init: " + e.getMessage());
        }
    }

    /** يُستدعى عند كل فتح للتطبيق */
    public void onAppLaunched() {
        int count = prefs.getInt(KEY_LAUNCH_COUNT, 0);
        prefs.edit().putInt(KEY_LAUNCH_COUNT, count + 1).apply();
    }

    /** يُستدعى بعد كل فاتورة ناجحة */
    public void onInvoiceCreated() {
        int count = prefs.getInt(KEY_INVOICES_COUNT, 0);
        prefs.edit().putInt(KEY_INVOICES_COUNT, count + 1).apply();
    }

    /** هل الشروط مستوفاة لطلب التقييم؟ */
    public boolean shouldRequestReview() {
        if (prefs.getBoolean(KEY_REVIEW_COMPLETED, false)) return false;
        long lastRequest = prefs.getLong(KEY_LAST_REQUEST, 0);
        if (lastRequest > 0 && (System.currentTimeMillis() - lastRequest) < REQUEST_INTERVAL_MS) return false;
        return prefs.getInt(KEY_LAUNCH_COUNT, 0) >= MIN_LAUNCHES
            && prefs.getInt(KEY_INVOICES_COUNT, 0) >= MIN_INVOICES
            && getDaysSinceInstall() >= MIN_INSTALL_DAYS;
    }

    /**
     * طلب التقييم عبر Google Play In-App Review API
     * يعرض sheet التقييم مباشرة داخل التطبيق
     */
    public void requestReview(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        if (!shouldRequestReview()) return;

        prefs.edit().putLong(KEY_LAST_REQUEST, System.currentTimeMillis()).apply();

        if (playReviewManager == null) {
            fallbackToPlayStore(activity);
            return;
        }

        try {
            Task<ReviewInfo> request = playReviewManager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Task<Void> flow = playReviewManager.launchReviewFlow(activity, task.getResult());
                    flow.addOnCompleteListener(flowTask -> {
                        prefs.edit().putBoolean(KEY_REVIEW_COMPLETED, true).apply();
                        Log.d(TAG, "In-App Review flow completed");
                    });
                    flow.addOnFailureListener(e -> {
                        Log.w(TAG, "Review flow failed: " + e.getMessage());
                        fallbackToPlayStore(activity);
                    });
                } else {
                    Log.w(TAG, "Review request failed, falling back to Play Store");
                    fallbackToPlayStore(activity);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "requestReview exception: " + e.getMessage());
            fallbackToPlayStore(activity);
        }
    }

    /** فتح صفحة Google Play كـ fallback */
    private void fallbackToPlayStore(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName())));
            } catch (Exception ex) {
                Log.e(TAG, "Cannot open Play Store: " + ex.getMessage());
            }
        }
    }

    /** طلب لطيف بعد حدث مهم */
    public void gentleNudge(Activity activity) {
        if (shouldRequestReview()) requestReview(activity);
    }

    /** للاختبار فقط */
    public void resetForTesting() {
        prefs.edit()
            .putBoolean(KEY_REVIEW_COMPLETED, false)
            .putLong(KEY_LAST_REQUEST, 0)
            .apply();
    }

    private long getDaysSinceInstall() {
        long install = prefs.getLong(KEY_INSTALL_DATE, System.currentTimeMillis());
        return (System.currentTimeMillis() - install) / (24 * 60 * 60 * 1000L);
    }
}
