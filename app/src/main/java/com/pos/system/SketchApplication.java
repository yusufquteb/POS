package com.pos.system;

import android.app.Application;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import com.pos.system.managers.LanguageManager;
import com.pos.system.managers.ThemeManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

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

        // Global Crash Handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String error = "CRASH في: " + thread.getName() + "\n\n" + sw.toString();
            Log.e(TAG, "FATAL: " + error);

            try {
                File f = new File(getExternalFilesDir(null), "crash_log.txt");
                FileWriter fw = new FileWriter(f, true);
                fw.write(new java.util.Date() + "\n\n" + error + "\n\n");
                fw.close();
            } catch (Exception ignored) {}

            try {
                Intent i = new Intent(getApplicationContext(), CrashActivity.class);
                i.putExtra("error", error);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            } catch (Exception e) {
                Log.e(TAG, "Cannot start CrashActivity: " + e.getMessage());
            }

            try { Thread.sleep(3000); } catch (Exception ignored) {}
            Process.killProcess(Process.myPid());
        });

        Log.d(TAG, "✓ SketchApplication started successfully");
    }
}
