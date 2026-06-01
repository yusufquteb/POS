package com.pos.system;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.pos.system.managers.LanguageManager;
import com.pos.system.managers.ThemeManager;

/**
 * BaseActivity - الأساس لجميع الـ Activities
 *
 * يُطبِّق تلقائياً:
 * 1. الثيم (فاتح/داكن/ألوان) → setTheme قبل super.onCreate()
 * 2. اللغة (عربي/إنجليزي + RTL/LTR) → attachBaseContext
 * 3. تغيير الثيم/اللغة → recreate() يُطبِّق التغيير فوراً
 */
public class BaseActivity extends AppCompatActivity {

    protected static final String TAG = "POS_System";

    // آخر حالة ثيم ولغة - للكشف عن التغيير في onResume
    private int  lastThemeResId;
    private String lastLanguage;

    // ══════════════════════════════════════════════════════
    // 1. تطبيق اللغة قبل inflate أي layout
    // ══════════════════════════════════════════════════════
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.updateResources(newBase));
    }

    // ══════════════════════════════════════════════════════
    // 2. تطبيق الثيم قبل setContentView
    // ══════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ تطبيق الثيم قبل أي شيء آخر
        ThemeManager.applyThemeToActivity(this);

        super.onCreate(savedInstanceState);

        // حفظ الحالة الحالية للكشف عن التغيير
        lastThemeResId = ThemeManager.getThemeResId();
        lastLanguage   = LanguageManager.getCurrentLanguage();
    }

    // ══════════════════════════════════════════════════════
    // 3. إعادة إنشاء Activity إذا تغيّر الثيم أو اللغة
    //    (يُغطّي الصفحات التي تبقى في backstack)
    // ══════════════════════════════════════════════════════
    @Override
    protected void onResume() {
        super.onResume();

        int    currentTheme = ThemeManager.getThemeResId();
        String currentLang  = LanguageManager.getCurrentLanguage();

        boolean themeChanged = (currentTheme != lastThemeResId);
        boolean langChanged  = !currentLang.equals(lastLanguage);

        if (themeChanged || langChanged) {
            // تحديث الحالة المحفوظة ثم إعادة إنشاء
            lastThemeResId = currentTheme;
            lastLanguage   = currentLang;
            recreate();
        }
    }

    // ══════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════
    protected void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    protected void showLongToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    /**
     * تطبيق Window Insets للـ Edge-to-Edge display
     */
    protected void applyWindowInsets(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                v.getPaddingLeft(),
                bars.top,
                v.getPaddingRight(),
                bars.bottom
            );
            return insets;
        });
    }

    protected void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected void showKeyboard(View view) {
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    protected boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm =
            (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnectedOrConnecting();
    }
}
