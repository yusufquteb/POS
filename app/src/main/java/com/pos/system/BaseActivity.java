package com.pos.system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.snackbar.Snackbar;
import com.pos.system.managers.LanguageManager;
import com.pos.system.managers.ThemeManager;

public class BaseActivity extends AppCompatActivity {

    protected static final String TAG = "POS_System";

    private int    lastThemeResId;
    private String lastLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.updateResources(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyThemeToActivity(this);
        super.onCreate(savedInstanceState);

        // Enable true edge-to-edge so content draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        lastThemeResId = ThemeManager.getThemeResId();
        lastLanguage   = LanguageManager.getCurrentLanguage();
    }

    @Override
    protected void onResume() {
        super.onResume();

        int    currentTheme = ThemeManager.getThemeResId();
        String currentLang  = LanguageManager.getCurrentLanguage();

        if (currentTheme != lastThemeResId || !currentLang.equals(lastLanguage)) {
            lastThemeResId = currentTheme;
            lastLanguage   = currentLang;
            recreate();
        }
    }

    // ── Edge-to-Edge insets ──────────────────────────────────────────

    /**
     * Apply system bar insets to a root view so content is not hidden
     * behind the status bar or navigation bar.
     * Call after setContentView() when the root view needs padding.
     */
    protected void applyWindowInsets(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // CoordinatorLayout + AppBarLayout: AppBarLayout handles status-bar top via
            // android:fitsSystemWindows="true". Applying paddingTop here would push the
            // entire AppBarLayout down and leave a gap beneath the status bar.
            boolean isCoordinator = v instanceof androidx.coordinatorlayout.widget.CoordinatorLayout;
            v.setPadding(
                v.getPaddingLeft(),
                isCoordinator ? 0 : bars.top,
                v.getPaddingRight(),
                bars.bottom
            );
            return insets;
        });
    }

    /**
     * Apply only bottom insets (navigation bar) — use for FABs and
     * bottom-anchored content inside CoordinatorLayout.
     */
    protected void applyBottomInsets(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                v.getPaddingLeft(),
                v.getPaddingTop(),
                v.getPaddingRight(),
                bars.bottom
            );
            return insets;
        });
    }

    // ── Snackbar ─────────────────────────────────────────────────────

    /**
     * Theme-aware Snackbar using Material 3 color roles.
     * isError=true → uses colorError / onError pair.
     * isError=false → uses colorPrimary / onPrimary pair.
     */
    protected void showSnackbar(String message, boolean isError) {
        View root = findViewById(android.R.id.content);
        if (root == null) {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Snackbar sb = Snackbar.make(root, message, Snackbar.LENGTH_LONG);

        int bgAttr   = isError ? com.google.android.material.R.attr.colorError
                                : com.google.android.material.R.attr.colorPrimary;
        int textAttr = isError ? com.google.android.material.R.attr.colorOnError
                                : com.google.android.material.R.attr.colorOnPrimary;

        TypedValue bgVal   = new TypedValue();
        TypedValue textVal = new TypedValue();
        if (getTheme().resolveAttribute(bgAttr, bgVal, true)) {
            sb.setBackgroundTint(bgVal.data);
        }
        if (getTheme().resolveAttribute(textAttr, textVal, true)) {
            sb.setTextColor(textVal.data);
        }
        sb.show();
    }

    protected void showSnackbar(String message) {
        showSnackbar(message, false);
    }

    // ── Navigation ───────────────────────────────────────────────────

    protected void openActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    protected void openActivity(Class<?> cls, android.os.Bundle extras) {
        Intent i = new Intent(this, cls);
        if (extras != null) i.putExtras(extras);
        startActivity(i);
    }

    // ── UI Helpers ───────────────────────────────────────────────────

    protected void showToast(String message) {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    protected void showLongToast(String message) {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        } else {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
        }
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
