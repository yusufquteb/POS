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
        int mode = getThemeMode();
        switch (mode) {
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_AUTO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    public static void applyThemeToActivity(@NonNull Activity activity) {
        activity.setTheme(R.style.AppTheme);
    }

    public static int getThemeResId() {
        return R.style.AppTheme;
    }

    public static int getThemeMode() {
        if (prefs == null) return MODE_LIGHT;
        return prefs.getInt(KEY_THEME_MODE, MODE_LIGHT);
    }

    public static boolean isDarkModeActive() {
        if (appContext == null) return false;
        int mode = getThemeMode();
        if (mode == MODE_DARK) return true;
        if (mode == MODE_LIGHT) return false;
        int nightMode = appContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void setThemeMode(int mode) {
        if (prefs != null) {
            prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        }
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
}
