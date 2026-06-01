package com.pos.system;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * GlobalExceptionHandler - معالج الأخطاء العام للتطبيق
 * يلتقط جميع الأخطاء غير المُعالجة ويرسلها إلى DebugActivity
 * 
 * الاستخدام:
 * في MainActivity.onCreate() ضع:
 * GlobalExceptionHandler.setup(this);
 * 
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "GlobalExceptionHandler";
    
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    
    /**
     * تفعيل معالج الأخطاء العام
     * @param context سياق التطبيق
     */
    public static void setup(@NonNull Context context) {
        Thread.UncaughtExceptionHandler currentHandler = 
            Thread.getDefaultUncaughtExceptionHandler();
        
        if (!(currentHandler instanceof GlobalExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(
                new GlobalExceptionHandler(context.getApplicationContext())
            );
            Log.d(TAG, "✓ Global exception handler installed");
        }
    }
    
    private GlobalExceptionHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            Log.e(TAG, "⚠️ UNCAUGHT EXCEPTION in thread: " + thread.getName(), throwable);
            
            // جمع معلومات الخطأ
            String errorReport = buildErrorReport(thread, throwable);
            
            // حفظ التقرير في SharedPreferences للوصول إليه لاحقاً
            saveErrorReport(errorReport);
            
            // فتح DebugActivity
            launchDebugActivity(throwable, errorReport);
            
            // إنهاء العملية بعد تأخير بسيط
            new android.os.Handler().postDelayed(() -> {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }, 1000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in exception handler!", e);
            
            // في حالة فشل المعالج، استخدم المعالج الافتراضي
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        }
    }
    
    /**
     * بناء تقرير شامل عن الخطأ
     */
    @NonNull
    private String buildErrorReport(@NonNull Thread thread, @NonNull Throwable throwable) {
        StringBuilder report = new StringBuilder();
        
        // معلومات الوقت
        report.append("═══════════════════════════════════════════\n");
        report.append("⚠️ تقرير الخطأ - POS System\n");
        report.append("═══════════════════════════════════════════\n\n");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        report.append("📅 التاريخ: ").append(sdf.format(new Date())).append("\n\n");
        
        // معلومات الخطأ
        report.append("🔴 نوع الخطأ: ").append(throwable.getClass().getSimpleName()).append("\n");
        report.append("📝 الرسالة: ").append(throwable.getMessage() != null ? throwable.getMessage() : "لا توجد رسالة").append("\n");
        report.append("🧵 Thread: ").append(thread.getName()).append("\n\n");
        
        // معلومات الجهاز
        report.append("═══════════════════════════════════════════\n");
        report.append("📱 معلومات الجهاز\n");
        report.append("═══════════════════════════════════════════\n\n");
        report.append("نظام التشغيل: Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        report.append("الشركة: ").append(Build.MANUFACTURER).append("\n");
        report.append("الموديل: ").append(Build.MODEL).append("\n");
        report.append("الجهاز: ").append(Build.DEVICE).append("\n\n");
        
        // معلومات التطبيق
        report.append("═══════════════════════════════════════════\n");
        report.append("📦 معلومات التطبيق\n");
        report.append("═══════════════════════════════════════════\n\n");
        
        try {
            android.content.pm.PackageInfo pInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            report.append("الإصدار: ").append(pInfo.versionName).append("\n");
            report.append("رقم الإصدار: ").append(pInfo.versionCode).append("\n");
        } catch (Exception e) {
            report.append("لا يمكن الحصول على معلومات التطبيق\n");
        }
        report.append("Package: ").append(context.getPackageName()).append("\n\n");
        
        // معلومات الذاكرة
        report.append("═══════════════════════════════════════════\n");
        report.append("💾 معلومات الذاكرة\n");
        report.append("═══════════════════════════════════════════\n\n");
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1048576L; // MB
        long totalMemory = runtime.totalMemory() / 1048576L;
        long freeMemory = runtime.freeMemory() / 1048576L;
        long usedMemory = totalMemory - freeMemory;
        
        report.append("الحد الأقصى: ").append(maxMemory).append(" MB\n");
        report.append("المستخدمة: ").append(usedMemory).append(" MB\n");
        report.append("المتاحة: ").append(freeMemory).append(" MB\n\n");
        
        // Stack Trace
        report.append("═══════════════════════════════════════════\n");
        report.append("📋 تفاصيل الخطأ (Stack Trace)\n");
        report.append("═══════════════════════════════════════════\n\n");
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        report.append(sw.toString());
        
        // Caused by
        Throwable cause = throwable.getCause();
        if (cause != null) {
            report.append("\n═══════════════════════════════════════════\n");
            report.append("🔍 السبب (Caused By)\n");
            report.append("═══════════════════════════════════════════\n\n");
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            cause.printStackTrace(pw);
            report.append(sw.toString());
        }
        
        report.append("\n═══════════════════════════════════════════\n");
        report.append("نهاية التقرير\n");
        report.append("═══════════════════════════════════════════\n");
        
        return report.toString();
    }
    
    /**
     * حفظ تقرير الخطأ
     */
    private void saveErrorReport(@NonNull String report) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences(
                "error_reports", Context.MODE_PRIVATE
            );
            
            // حفظ آخر 5 أخطاء
            for (int i = 4; i > 0; i--) {
                String prev = prefs.getString("error_" + (i-1), null);
                if (prev != null) {
                    prefs.edit().putString("error_" + i, prev).apply();
                }
            }
            
            prefs.edit()
                .putString("error_0", report)
                .putLong("error_0_time", System.currentTimeMillis())
                .apply();
            
            Log.d(TAG, "✓ Error report saved");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save error report", e);
        }
    }
    
    /**
     * فتح صفحة Debug
     */
    private void launchDebugActivity(@NonNull Throwable throwable, @NonNull String fullReport) {
        try {
            Intent intent = new Intent(context, DebugActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // إرسال رسالة مختصرة + التقرير الكامل
            String shortMessage = throwable.getClass().getSimpleName() + ": " + 
                                 (throwable.getMessage() != null ? throwable.getMessage() : "خطأ غير معروف");
            
            intent.putExtra(DebugActivity.EXTRA_ERROR, shortMessage);
            intent.putExtra(DebugActivity.EXTRA_STACK, fullReport);
            intent.putExtra(DebugActivity.EXTRA_TIMESTAMP, System.currentTimeMillis());
            
            context.startActivity(intent);
            
            Log.d(TAG, "✓ DebugActivity launched");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch DebugActivity", e);
        }
    }
    
    /**
     * استرجاع آخر تقرير خطأ محفوظ
     */
    public static String getLastErrorReport(@NonNull Context context) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences(
                "error_reports", Context.MODE_PRIVATE
            );
            return prefs.getString("error_0", null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * مسح جميع تقارير الأخطاء المحفوظة
     */
    public static void clearErrorReports(@NonNull Context context) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences(
                "error_reports", Context.MODE_PRIVATE
            );
            prefs.edit().clear().apply();
            Log.d(TAG, "✓ Error reports cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear error reports", e);
        }
    }
    
    public static void handle(Context context, Exception e) {
    Log.e("Error", e.getMessage());
}

}
