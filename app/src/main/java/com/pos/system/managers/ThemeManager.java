package com.pos.system.managers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import com.pos.system.R;

/**
 * ThemeManager - مدير الثيمات والألوان (مُصحَّح)
 *
 * الإصلاحات:
 * - إضافة ثيمات Green / Orange / Teal المفقودة
 * - توحيد أسماء الـ styles مع styles.xml
 *
 * الميزات:
 * - Dark / Light / Auto Mode
 * - 5 مجموعات ألوان: أزرق / بنفسجي / أخضر / برتقالي / تركواز
 * - حفظ التفضيلات تلقائياً
 *
 * الاستخدام:
 * ThemeManager.init(context);      // في Application.onCreate()
 * ThemeManager.applyTheme();       // لتطبيق الوضع (داكن/فاتح)
 * ThemeManager.applyThemeToActivity(this); // في Activity.onCreate() قبل super
 *
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class ThemeManager {

    private static final String PREFS_NAME       = "theme_prefs";
    private static final String KEY_THEME_MODE   = "theme_mode";
    private static final String KEY_COLOR_SCHEME = "color_scheme";

    // ─── أوضاع الثيم ────────────────────────────────────────
    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK  = 1;
    public static final int MODE_AUTO  = 2;

    // ─── مجموعات الألوان ──────────────────────────────────────
    public static final int COLOR_BLUE   = 0;
    public static final int COLOR_PURPLE = 1;
    public static final int COLOR_GREEN  = 2;
    public static final int COLOR_ORANGE = 3;
    public static final int COLOR_TEAL    = 4;
    public static final int COLOR_EGYPT   = 5;
    public static final int COLOR_DYNAMIC = 6;

    private static Context          appContext;
    private static SharedPreferences prefs;

    // ════════════════════════════════════════════════════════════
    // تهيئة - يجب استدعاؤها في Application.onCreate()
    // ════════════════════════════════════════════════════════════
    public static void init(@NonNull Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ════════════════════════════════════════════════════════════
    // تطبيق وضع الثيم (داكن / فاتح / تلقائي)
    // ════════════════════════════════════════════════════════════
    public static void applyTheme() {
        checkInit();
        switch (getThemeMode()) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_AUTO:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // ════════════════════════════════════════════════════════════
    // تطبيق الثيم على Activity (يُستدعى في onCreate قبل super)
    // ════════════════════════════════════════════════════════════
    public static void applyThemeToActivity(@NonNull Activity activity) {
        checkInit();
        activity.setTheme(getThemeResId());
    }

    // ════════════════════════════════════════════════════════════
    // الحصول على Resource ID للثيم الحالي
    // ════════════════════════════════════════════════════════════
    public static int getThemeResId() {
        checkInit();
        int colorScheme = getColorScheme();
        boolean isDark  = isDarkModeActive();

        switch (colorScheme) {
            case COLOR_BLUE:
                return isDark ? R.style.AppTheme_Blue_Dark   : R.style.AppTheme_Blue;
            case COLOR_PURPLE:
                return isDark ? R.style.AppTheme_Purple_Dark : R.style.AppTheme_Purple;
            case COLOR_GREEN:
                return isDark ? R.style.AppTheme_Green_Dark  : R.style.AppTheme_Green;
            case COLOR_ORANGE:
                return isDark ? R.style.AppTheme_Orange_Dark : R.style.AppTheme_Orange;
            case COLOR_TEAL:
                return isDark ? R.style.AppTheme_Teal_Dark   : R.style.AppTheme_Teal;
            case COLOR_EGYPT:
                return isDark ? R.style.AppTheme_Egypt_Dark  : R.style.AppTheme_Egypt;
            case COLOR_DYNAMIC:
                return isDark ? R.style.AppTheme_Blue_Dark   : R.style.AppTheme_Blue;
            default:
                return isDark ? R.style.AppTheme_Blue_Dark   : R.style.AppTheme_Blue;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Getters
    // ════════════════════════════════════════════════════════════
    public static int getThemeMode() {
        checkInit();
        return prefs.getInt(KEY_THEME_MODE, MODE_AUTO);
    }

    public static int getColorScheme() {
        checkInit();
        return prefs.getInt(KEY_COLOR_SCHEME, COLOR_BLUE);
    }

    public static boolean isDarkModeActive() {
        checkInit();
        int mode = getThemeMode();
        if (mode == MODE_DARK)  return true;
        if (mode == MODE_LIGHT) return false;
        // AUTO - فحص إعدادات النظام
        int nightMode = appContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // ════════════════════════════════════════════════════════════
    // Setters
    // ════════════════════════════════════════════════════════════
    public static void setThemeMode(int mode) {
        checkInit();
        if (mode < MODE_LIGHT || mode > MODE_AUTO) {
            throw new IllegalArgumentException("Invalid theme mode: " + mode);
        }
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme();
    }

    public static void setColorScheme(int colorScheme) {
        checkInit();
        if (colorScheme < COLOR_BLUE || colorScheme > COLOR_DYNAMIC) {
            throw new IllegalArgumentException("Invalid color scheme: " + colorScheme);
        }
        prefs.edit().putInt(KEY_COLOR_SCHEME, colorScheme).apply();
    }

    public static void toggleDarkMode() {
        int currentMode = getThemeMode();
        if (currentMode == MODE_AUTO) {
            setThemeMode(isDarkModeActive() ? MODE_LIGHT : MODE_DARK);
        } else {
            setThemeMode(currentMode == MODE_LIGHT ? MODE_DARK : MODE_LIGHT);
        }
    }

    // ════════════════════════════════════════════════════════════
    // Helpers - أسماء وألوان
    // ════════════════════════════════════════════════════════════
    @NonNull
    public static String getThemeModeName() {
        switch (getThemeMode()) {
            case MODE_LIGHT: return "فاتح";
            case MODE_DARK:  return "داكن";
            case MODE_AUTO:  return "تلقائي";
            default:         return "غير معروف";
        }
    }

    @NonNull
    public static String getColorSchemeName() {
        switch (getColorScheme()) {
            case COLOR_BLUE:   return "أزرق كلاسيكي";
            case COLOR_PURPLE: return "بنفسجي أنيق";
            case COLOR_GREEN:  return "أخضر منعش";
            case COLOR_ORANGE: return "برتقالي دافئ";
            case COLOR_TEAL:    return "تركواز عصري";
            case COLOR_EGYPT:   return "ذهبي مصري";
            case COLOR_DYNAMIC: return "ألوان ديناميكية";
            default:            return "غير معروف";
        }
    }

    @NonNull
    public static String[] getAvailableThemeModes() {
        return new String[]{"فاتح", "داكن", "تلقائي (حسب النظام)"};
    }

    @NonNull
    public static String[] getAvailableColorSchemes() {
        return new String[]{
            "أزرق كلاسيكي",
            "بنفسجي أنيق",
            "أخضر منعش",
            "برتقالي دافئ",
            "تركواز عصري",
            "ذهبي مصري",
            "ألوان ديناميكية"
        };
    }

    /** اللون الأساسي كـ ARGB int */
    public static int getPrimaryColor() {
        checkInit();
        switch (getColorScheme()) {
            case COLOR_BLUE:   return 0xFF1976D2;
            case COLOR_PURPLE: return 0xFF6A1B9A;
            case COLOR_GREEN:  return 0xFF388E3C;
            case COLOR_ORANGE: return 0xFFE64A19;
            case COLOR_TEAL:   return 0xFF00796B;
            default:           return 0xFF1976D2;
        }
    }

    /** اللون الثانوي كـ ARGB int */
    public static int getSecondaryColor() {
        checkInit();
        switch (getColorScheme()) {
            case COLOR_BLUE:   return 0xFF42A5F5;
            case COLOR_PURPLE: return 0xFF9C27B0;
            case COLOR_GREEN:  return 0xFF66BB6A;
            case COLOR_ORANGE: return 0xFFFF7043;
            case COLOR_TEAL:   return 0xFF26A69A;
            default:           return 0xFF42A5F5;
        }
    }

    public static boolean isDynamicColorSupported() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S;
    }

    public static void applyDynamicColorIfEnabled(android.app.Activity activity) {
        if (getColorScheme() == COLOR_DYNAMIC && isDynamicColorSupported()) {
            DynamicColors.applyToActivityIfAvailable(activity);
        }
    }

    public static void resetToDefaults() {
        checkInit();
        prefs.edit()
            .putInt(KEY_THEME_MODE,   MODE_AUTO)
            .putInt(KEY_COLOR_SCHEME, COLOR_BLUE)
            .apply();
        applyTheme();
    }

    // ════════════════════════════════════════════════════════════
    // Internal
    // ════════════════════════════════════════════════════════════
    private static void checkInit() {
        if (appContext == null || prefs == null) {
            throw new IllegalStateException(
                "ThemeManager not initialized. Call ThemeManager.init(context) first."
            );
        }
    }
}
