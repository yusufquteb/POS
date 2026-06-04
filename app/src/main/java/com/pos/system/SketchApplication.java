package com.pos.system;

import android.app.Application;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pos.system.managers.LanguageManager;
import com.pos.system.managers.ThemeManager;
import com.pos.system.workers.LowStockWorker;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

/**
 * SketchApplication - Application Class الرئيسية
 *
 * إصلاحات:
 * 1. تهيئة LanguageManager (كانت مفقودة → crash فوري)
 * 2. تهيئة ThemeManager (كانت مفقودة → theme لا يُطبَّق)
 * 3. تحسين Crash Handler
 *
 * @version 2.1 (Fixed)
 */
public class SketchApplication extends Application {

    private static final String TAG = "SketchApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // ═══════════════════════════════════════════════════════
        // إصلاح #1: تهيئة LanguageManager أولاً
        // بدونها: BaseActivity.attachBaseContext() → checkInit()
        // → IllegalStateException → صفحة بيضاء وتوقف فوري
        // ═══════════════════════════════════════════════════════
        try {
            LanguageManager.init(this);
            Log.d(TAG, "✓ LanguageManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "LanguageManager init failed: " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════
        // إصلاح #2: تهيئة ThemeManager
        // ═══════════════════════════════════════════════════════
        try {
            ThemeManager.init(this);
            Log.d(TAG, "✓ ThemeManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "ThemeManager init failed: " + e.getMessage());
        }

        // Material You - Dynamic Colors (Android 12+)
        try {
            com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
            Log.d(TAG, "✓ DynamicColors applied");
        } catch (Exception e) {
            Log.e(TAG, "DynamicColors failed: " + e.getMessage());
        }

        // Schedule daily low-stock background check
        try {
            PeriodicWorkRequest lowStockWork = new PeriodicWorkRequest.Builder(
                LowStockWorker.class, 1, TimeUnit.DAYS)
                .build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "low_stock_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                lowStockWork
            );
            Log.d(TAG, "✓ LowStockWorker scheduled");
        } catch (Exception e) {
            Log.e(TAG, "WorkManager scheduling failed: " + e.getMessage());
        }

        // Crash logger — log to file only, let system show default crash dialog
        Thread.UncaughtExceptionHandler sysHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                String entry = new java.util.Date() + "\nThread: " + thread.getName()
                    + "\n" + sw + "\n---\n";
                File f = new File(getExternalFilesDir(null), "crash_log.txt");
                FileWriter fw = new FileWriter(f, true);
                fw.write(entry);
                fw.close();
                Log.e(TAG, "CRASH: " + entry);
            } catch (Exception ignored) {}
            // delegate to system so Android shows normal crash dialog (no loop)
            if (sysHandler != null) sysHandler.uncaughtException(thread, throwable);
        });

        Log.d(TAG, "✓ SketchApplication started successfully");
    }
}
