package com.pos.system.utils;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * ErrorHandler - معالج شامل للأخطاء
 * يوفر طريقة موحدة لمعالجة وعرض الأخطاء في التطبيق
 * 
 * @author POS System
 * @version 1.0
 * @since 2026-02-13
 */
public class ErrorHandler {

    private static final String TAG = "ErrorHandler";
    private static final boolean DEBUG_MODE = com.pos.system.BuildConfig.DEBUG;

    /**
     * معالجة الخطأ وعرض رسالة مناسبة
     * @param context السياق
     * @param e الاستثناء
     */
    public static void handle(@NonNull Context context, @NonNull Exception e) {
        handle(context, e, null);
    }

    /**
     * معالجة الخطأ وعرض رسالة مناسبة مع Snackbar
     * @param context السياق
     * @param e الاستثناء
     * @param rootView العنصر الرئيسي لعرض Snackbar
     */
    public static void handle(@NonNull Context context, 
                             @NonNull Exception e, 
                             @Nullable View rootView) {
        // تسجيل الخطأ في Logcat
        logError(e);
        
        // الحصول على الرسالة المناسبة للمستخدم
        String message = getUserFriendlyMessage(e);
        
        // عرض الرسالة
        if (rootView != null) {
            showSnackbar(rootView, message, e);
        } else {
            showToast(context, message);
        }
        
        // إرسال تقرير للتحليلات (إذا كان مفعلاً)
        reportToAnalytics(e);
    }

    /**
     * معالجة خطأ بسيط وعرض Toast
     * @param context السياق
     * @param message الرسالة
     */
    public static void showError(@NonNull Context context, @NonNull String message) {
        Log.e(TAG, message);
        showToast(context, message);
    }

    /**
     * معالجة خطأ وعرض Snackbar مع إمكانية إعادة المحاولة
     * @param rootView العنصر الرئيسي
     * @param message الرسالة
     * @param retryAction إجراء إعادة المحاولة
     */
    public static void showErrorWithRetry(@NonNull View rootView, 
                                         @NonNull String message,
                                         @Nullable Runnable retryAction) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        
        if (retryAction != null) {
            snackbar.setAction("إعادة المحاولة", v -> retryAction.run());
        }
        
        snackbar.show();
    }

    /**
     * تسجيل الخطأ في Logcat
     * @param e الاستثناء
     */
    private static void logError(@NonNull Exception e) {
        if (DEBUG_MODE) {
            Log.e(TAG, "Error occurred: " + e.getClass().getSimpleName(), e);
        } else {
            Log.e(TAG, "Error occurred: " + e.getMessage());
        }
    }

    /**
     * الحصول على رسالة مفهومة للمستخدم
     * @param e الاستثناء
     * @return الرسالة المناسبة
     */
    @NonNull
    private static String getUserFriendlyMessage(@NonNull Exception e) {
        // أخطاء قاعدة البيانات
        if (e instanceof SQLiteException) {
            return handleDatabaseError((SQLiteException) e);
        }
        
        // أخطاء الإدخال/الإخراج
        if (e instanceof IOException) {
            return handleIOError((IOException) e);
        }
        
        // أخطاء التنسيق
        if (e instanceof NumberFormatException) {
            return "قيمة رقمية غير صحيحة. تأكد من إدخال أرقام فقط.";
        }
        
        // أخطاء فارغة
        if (e instanceof NullPointerException) {
            return "خطأ في البيانات. حاول مرة أخرى.";
        }
        
        // أخطاء غير متوقعة
        return "حدث خطأ غير متوقع. حاول مرة أخرى.";
    }

    /**
     * معالجة أخطاء قاعدة البيانات
     * @param e استثناء قاعدة البيانات
     * @return الرسالة المناسبة
     */
    @NonNull
    private static String handleDatabaseError(@NonNull SQLiteException e) {
        String message = e.getMessage();
        
        if (message != null) {
            if (message.contains("UNIQUE constraint failed")) {
                return "هذا العنصر موجود مسبقاً. استخدم باركود أو رقم مختلف.";
            }
            
            if (message.contains("no such table")) {
                return "خطأ في قاعدة البيانات. قد تحتاج إلى إعادة تثبيت التطبيق.";
            }
            
            if (message.contains("disk I/O error")) {
                return "خطأ في القراءة من الذاكرة. تأكد من وجود مساحة كافية.";
            }
            
            if (message.contains("database is locked")) {
                return "قاعدة البيانات مشغولة. انتظر قليلاً وحاول مرة أخرى.";
            }
        }
        
        return "خطأ في قاعدة البيانات. حاول مرة أخرى.";
    }

    /**
     * معالجة أخطاء الإدخال/الإخراج
     * @param e استثناء IO
     * @return الرسالة المناسبة
     */
    @NonNull
    private static String handleIOError(@NonNull IOException e) {
        if (e instanceof UnknownHostException) {
            return "لا يوجد اتصال بالإنترنت. تحقق من الاتصال وحاول مرة أخرى.";
        }
        
        String message = e.getMessage();
        
        if (message != null) {
            if (message.contains("Permission denied")) {
                return "لا توجد صلاحية للوصول إلى الملف. تحقق من الأذونات.";
            }
            
            if (message.contains("No space left")) {
                return "لا توجد مساحة كافية على الجهاز. احذف بعض الملفات.";
            }
            
            if (message.contains("Read-only")) {
                return "الملف للقراءة فقط. لا يمكن التعديل عليه.";
            }
        }
        
        return "خطأ في القراءة أو الكتابة. حاول مرة أخرى.";
    }

    /**
     * عرض Toast
     * @param context السياق
     * @param message الرسالة
     */
    private static void showToast(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * عرض Snackbar
     * @param rootView العنصر الرئيسي
     * @param message الرسالة
     * @param e الاستثناء
     */
    private static void showSnackbar(@NonNull View rootView, 
                                    @NonNull String message, 
                                    @NonNull Exception e) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        
        // إضافة زر لعرض التفاصيل في وضع التطوير
        if (DEBUG_MODE) {
            snackbar.setAction("التفاصيل", v -> {
                Log.d(TAG, "Error details", e);
                showToast(rootView.getContext(), 
                    "التفاصيل في Logcat: " + e.getClass().getSimpleName());
            });
        }
        
        snackbar.show();
    }

    /**
     * إرسال تقرير للتحليلات (يمكن دمجه مع Firebase Crashlytics)
     * @param e الاستثناء
     */
    private static void reportToAnalytics(@NonNull Exception e) {
        // TODO: دمج مع Firebase Crashlytics أو أي أداة تحليلات
        // مثال:
        // FirebaseCrashlytics.getInstance().recordException(e);
        
        if (DEBUG_MODE) {
            Log.d(TAG, "Error reported to analytics: " + e.getClass().getSimpleName());
        }
    }

    /**
     * رسائل خطأ مخصصة لحالات محددة
     */
    public static class ErrorMessages {
        public static final String BARCODE_EMPTY = "الرجاء إدخال الباركود";
        public static final String BARCODE_INVALID = "الباركود غير صحيح";
        public static final String BARCODE_EXISTS = "هذا الباركود موجود مسبقاً";
        
        public static final String NAME_EMPTY = "الرجاء إدخال الاسم";
        public static final String NAME_TOO_SHORT = "الاسم قصير جداً";
        
        public static final String PRICE_EMPTY = "الرجاء إدخال السعر";
        public static final String PRICE_INVALID = "السعر غير صحيح";
        public static final String PRICE_NEGATIVE = "السعر لا يمكن أن يكون سالباً";
        
        public static final String QUANTITY_EMPTY = "الرجاء إدخال الكمية";
        public static final String QUANTITY_INVALID = "الكمية غير صحيحة";
        public static final String QUANTITY_INSUFFICIENT = "الكمية المتوفرة غير كافية";
        
        public static final String CUSTOMER_NOT_SELECTED = "الرجاء اختيار عميل";
        public static final String CART_EMPTY = "السلة فارغة";
        
        public static final String BACKUP_FAILED = "فشل إنشاء النسخة الاحتياطية";
        public static final String RESTORE_FAILED = "فشلت عملية الاستعادة";
        
        public static final String PRINT_FAILED = "فشلت عملية الطباعة";
        public static final String PRINTER_NOT_CONNECTED = "الطابعة غير متصلة";
        
        public static final String PERMISSION_DENIED = "تم رفض الإذن";
        public static final String STORAGE_PERMISSION_REQUIRED = "مطلوب إذن الوصول إلى الملفات";
        
        public static final String NETWORK_ERROR = "خطأ في الاتصال بالشبكة";
        public static final String NO_INTERNET = "لا يوجد اتصال بالإنترنت";
    }

    /**
     * أنواع الأخطاء
     */
    public enum ErrorType {
        DATABASE,
        NETWORK,
        VALIDATION,
        PERMISSION,
        FILE_IO,
        UNKNOWN
    }

    /**
     * تحديد نوع الخطأ
     * @param e الاستثناء
     * @return نوع الخطأ
     */
    @NonNull
    public static ErrorType getErrorType(@NonNull Exception e) {
        if (e instanceof SQLiteException) {
            return ErrorType.DATABASE;
        } else if (e instanceof IOException) {
            return ErrorType.FILE_IO;
        } else if (e instanceof NumberFormatException || 
                   e instanceof IllegalArgumentException) {
            return ErrorType.VALIDATION;
        } else if (e instanceof SecurityException) {
            return ErrorType.PERMISSION;
        }
        return ErrorType.UNKNOWN;
    }

    /**
     * عرض رسالة نجاح
     * @param context السياق
     * @param message الرسالة
     */
    public static void showSuccess(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, "✓ " + message, Toast.LENGTH_SHORT).show();
    }

    /**
     * عرض رسالة نجاح مع Snackbar
     * @param rootView العنصر الرئيسي
     * @param message الرسالة
     */
    public static void showSuccess(@NonNull View rootView, @NonNull String message) {
        Snackbar.make(rootView, "✓ " + message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * عرض رسالة تحذير
     * @param context السياق
     * @param message الرسالة
     */
    public static void showWarning(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, "⚠ " + message, Toast.LENGTH_LONG).show();
    }

    /**
     * عرض رسالة تحذير مع Snackbar
     * @param rootView العنصر الرئيسي
     * @param message الرسالة
     */
    public static void showWarning(@NonNull View rootView, @NonNull String message) {
        Snackbar.make(rootView, "⚠ " + message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * عرض رسالة معلومات
     * @param context السياق
     * @param message الرسالة
     */
    public static void showInfo(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, "ℹ " + message, Toast.LENGTH_SHORT).show();
    }
}
