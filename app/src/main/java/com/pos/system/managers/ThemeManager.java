package com.pos.system.managers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import com.pos.system.R;

public class ThemeManager {

    private static final String PREFS_NAME     = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK  = 1;
    public static final int MODE_AUTO  = 2;

    private static Context           appContext;
    private static SharedPreferences prefs;

    public static void init(@NonNull Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void applyTheme() {
        // Always force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void applyThemeToActivity(@NonNull Activity activity) {
        activity.setTheme(R.style.AppTheme);
    }

    public static int getThemeResId() {
        return R.style.AppTheme;
    }

    public static int getThemeMode() {
        return MODE_LIGHT;
    }

    public static boolean isDarkModeActive() {
        return false;
    }

    public static void setThemeMode(int mode) {
        // Ignore — light mode is fixed
        applyTheme();
    }

    public static void toggleDarkMode() {
        int current = getThemeMode();
        setThemeMode(current == MODE_LIGHT ? MODE_DARK :
                     current == MODE_DARK  ? MODE_LIGHT : MODE_AUTO);
    }

    @NonNull
    public static String getThemeModeName() {
        switch (getThemeMode()) {
            case MODE_LIGHT: return "فاتح";
            case MODE_DARK:  return "داكن";
            default:         return "تلقائي";
        }
    }

    @NonNull
    public static String[] getAvailableThemeModes() {
        return new String[]{"فاتح", "داكن", "تلقائي (حسب النظام)"};
    }

    private static void checkInit() {
        if (appContext == null || prefs == null) {
            throw new IllegalStateException(
                "ThemeManager not initialized. Call ThemeManager.init(context) first.");
        }
    }
}
