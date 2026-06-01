package com.pos.system.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import androidx.annotation.NonNull;
import java.util.Locale;

/**
 * LanguageManager - مدير اللغات (مُصحَّح)
 *
 * الإصلاحات:
 * - دعم Android 13+ بشكل صحيح
 * - إعادة تشغيل التطبيق عبر MainActivity بدلاً من killProcess
 * - updateResources مُصحَّح لـ BaseActivity.attachBaseContext
 *
 * الميزات:
 * - دعم العربية والإنجليزية
 * - RTL / LTR تلقائي
 * - حفظ اللغة المختارة
 *
 * الاستخدام:
 * LanguageManager.init(context);                          // في Application.onCreate()
 * LanguageManager.setLanguage(context, LANG_ARABIC);     // لتغيير اللغة
 *
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class LanguageManager {

    private static final String PREFS_NAME   = "language_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static final String LANG_ARABIC  = "ar";
    public static final String LANG_ENGLISH = "en";

    private static Context           appContext;
    private static SharedPreferences prefs;

    // ════════════════════════════════════════════════════════════
    // تهيئة - يجب استدعاؤها في Application.onCreate()
    // ════════════════════════════════════════════════════════════
    public static void init(@NonNull Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // تطبيق اللغة المحفوظة على السياق العام
        applyLanguage(appContext, getCurrentLanguage());
    }

    // ════════════════════════════════════════════════════════════
    // Getters
    // ════════════════════════════════════════════════════════════
    @NonNull
    public static String getCurrentLanguage() {
        checkInit();
        return prefs.getString(KEY_LANGUAGE, LANG_ARABIC);
    }

    @NonNull
    public static Locale getCurrentLocale() {
        return new Locale(getCurrentLanguage());
    }

    @NonNull
    public static String getCurrentLanguageName() {
        return getLanguageName(getCurrentLanguage());
    }

    @NonNull
    public static String getLanguageName(@NonNull String code) {
        switch (code) {
            case LANG_ARABIC:  return "العربية";
            case LANG_ENGLISH: return "English";
            default:           return "Unknown";
        }
    }

    public static boolean isArabic()  { return getCurrentLanguage().equals(LANG_ARABIC); }
    public static boolean isEnglish() { return getCurrentLanguage().equals(LANG_ENGLISH); }
    public static boolean isRTL()     { return isArabic(); }

    @NonNull
    public static String[] getAvailableLanguages() {
        return new String[]{"العربية", "English"};
    }

    @NonNull
    public static String[] getAvailableLanguageCodes() {
        return new String[]{LANG_ARABIC, LANG_ENGLISH};
    }

    // ════════════════════════════════════════════════════════════
    // Setters
    // ════════════════════════════════════════════════════════════

    /**
     * تعيين اللغة وإعادة تشغيل التطبيق
     */
    public static void setLanguage(@NonNull Context context, @NonNull String languageCode) {
        checkInit();
        if (!isValidLanguage(languageCode)) {
            throw new IllegalArgumentException("Invalid language code: " + languageCode);
        }
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        applyLanguage(context, languageCode);
        restartApp(context);
    }

    /**
     * تبديل اللغة بين العربية والإنجليزية
     */
    public static void toggleLanguage(@NonNull Context context) {
        String current = getCurrentLanguage();
        setLanguage(context, current.equals(LANG_ARABIC) ? LANG_ENGLISH : LANG_ARABIC);
    }

    // ════════════════════════════════════════════════════════════
    // التطبيق على Context (للـ BaseActivity.attachBaseContext)
    // ════════════════════════════════════════════════════════════

    /**
     * تحديث Resources لـ Context - يُستخدم في attachBaseContext
     */
    @NonNull
    public static Context updateResources(@NonNull Context context) {
        // إذا لم يتم التهيئة بعد، نستخدم الافتراضي
        String language = LANG_ARABIC;
        try {
            checkInit();
            language = getCurrentLanguage();
        } catch (IllegalStateException ignored) {
            // LanguageManager لم يُهيَّأ بعد، نستخدم Arabic كافتراضي
        }
        return applyLanguageToContext(context, language);
    }

    /**
     * تطبيق اللغة على Activity (بديل قديم - مستخدم من بعض Activities)
     */
    public static void applyLanguageToActivity(@NonNull Activity activity) {
        checkInit();
        applyLanguage(activity, getCurrentLanguage());
    }

    // ════════════════════════════════════════════════════════════
    // Internal
    // ════════════════════════════════════════════════════════════

    private static void applyLanguage(@NonNull Context context, @NonNull String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(locale);
            }
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }

    @NonNull
    private static Context applyLanguageToContext(@NonNull Context context,
                                                   @NonNull String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(locale);
            }
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }

    /**
     * إعادة تشغيل التطبيق من البداية
     */
    private static void restartApp(@NonNull Context context) {
        try {
            // مسح كل الـ backstack وإعادة التشغيل من MainActivity
            Intent intent = new Intent(context, com.pos.system.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
                // تطبيق animation بدون تأثير (فوري)
                ((Activity) context).overridePendingTransition(0, 0);
            }
        } catch (Exception e) {
            // fallback: إعادة تشغيل من الـ launcher
            try {
                Intent fallback = context.getPackageManager()
                        .getLaunchIntentForPackage(context.getPackageName());
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(fallback);
                }
            } catch (Exception ignored) {}
        }
    }

    private static boolean isValidLanguage(@NonNull String code) {
        return code.equals(LANG_ARABIC) || code.equals(LANG_ENGLISH);
    }

    private static void checkInit() {
        if (appContext == null || prefs == null) {
            throw new IllegalStateException(
                "LanguageManager not initialized. Call LanguageManager.init(context) first."
            );
        }
    }

    /**
     * تطبيق اللغة على Application (مساعد)
     */
    public static void applyToApplication(@NonNull Context context) {
        checkInit();
        applyLanguage(context, getCurrentLanguage());
    }
}
